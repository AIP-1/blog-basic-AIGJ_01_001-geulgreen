package com.team.blog.contact.application;

import com.team.blog.account.application.EmailRules;
import com.team.blog.account.application.InvalidEmailException;
import com.team.blog.account.application.MailSender;
import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.moderation.application.AdminGuard;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.event.InquiryReceived;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/** 090 문의하기(강성찬 개인 확장): 문의 저장(IP마다 한 시간 횟수 제한), 관리자 문의함, 답장 메일. */
@Service
public class InquiryService {

    /** 문의 종류(화면 순서대로). */
    public static final Map<String, String> KINDS = Map.of("ACCOUNT", "계정·로그인", "BUG", "버그 신고", "IDEA", "기능 제안",
            "OTHER", "기타");
    public static final List<String> KIND_ORDER = List.of("ACCOUNT", "BUG", "IDEA", "OTHER");
    public static final int TITLE_MAX = 100;
    public static final int CONTENT_MAX = 3000;

    /** 양식으로 다시 보여 줄 거절 사유. */
    public static class Rejected extends RuntimeException {
        public Rejected(String message) {
            super(message);
        }
    }

    public record Inquiry(long id, String kind, String kindLabel, String title, String content, String email, String status,
                          String reply, Instant repliedAt, Instant createdAt, String memberHandle) {
        public boolean done() {
            return "DONE".equals(status);
        }
    }

    private static final RowMapper<Inquiry> ROW = (rs, n) -> new Inquiry(rs.getLong("id"), rs.getString("kind"),
            KINDS.get(rs.getString("kind")), rs.getString("title"), rs.getString("content"), rs.getString("email"),
            rs.getString("status"), rs.getString("reply"),
            rs.getTimestamp("replied_at") == null ? null : rs.getTimestamp("replied_at").toInstant(),
            rs.getTimestamp("created_at").toInstant(), rs.getString("handle"));

    private static final String SELECT = """
            SELECT i.id, i.kind, i.title, i.content, i.email, i.status, i.reply, i.replied_at, i.created_at, m.handle
            FROM inquiry i LEFT JOIN member m ON m.id = i.member_id
            """;

    private final JdbcTemplate jdbc;
    private final RedisRateLimiter rateLimiter;
    private final ContactProperties properties;
    private final AdminGuard adminGuard;
    private final MailSender mailSender;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public InquiryService(JdbcTemplate jdbc, RedisRateLimiter rateLimiter, ContactProperties properties, AdminGuard adminGuard,
                          MailSender mailSender, ApplicationEventPublisher events, Clock clock) {
        this.jdbc = jdbc;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.adminGuard = adminGuard;
        this.mailSender = mailSender;
        this.events = events;
        this.clock = clock;
    }

    /** 로그인한 회원의 답장 주소(처음 연결한 계정의 이메일). 없으면 null. */
    public String memberEmail(Optional<CurrentUser> current) {
        return current.flatMap(u -> jdbc.queryForList(
                "SELECT email FROM auth_identity WHERE member_id = ? AND email IS NOT NULL ORDER BY id LIMIT 1", String.class,
                u.memberId()).stream().findFirst()).orElse(null);
    }

    /** 회원은 저장된 이메일로만 답장받는다(양식 값은 무시). @return 문의 번호 */
    public long submit(Optional<CurrentUser> current, String ip, String kind, String rawTitle, String rawContent,
                       String rawEmail) {
        String title = rawTitle == null ? "" : rawTitle.strip();
        String content = rawContent == null ? "" : rawContent.strip().replace("\r\n", "\n");
        if (kind == null || !KINDS.containsKey(kind)) {
            throw new Rejected("문의 종류를 골라 주세요");
        }
        if (title.isEmpty() || title.codePointCount(0, title.length()) > TITLE_MAX) {
            throw new Rejected("제목은 1~" + TITLE_MAX + "자로 적어 주세요");
        }
        if (content.isEmpty() || content.codePointCount(0, content.length()) > CONTENT_MAX) {
            throw new Rejected("내용은 1~" + CONTENT_MAX + "자로 적어 주세요");
        }
        String email = memberEmail(current);
        if (email == null) {
            try {
                email = EmailRules.normalizeAndValidate(rawEmail);
            } catch (InvalidEmailException e) {
                throw new Rejected("답장 받을 이메일을 바르게 적어 주세요");
            }
        }
        if (!rateLimiter.tryAcquire(RedisRateLimiter.key("contact:send", ip), properties.perHour(), Duration.ofHours(1))
                .allowed()) {
            throw new Rejected("문의를 너무 많이 보냈어요. 한 시간 뒤에 다시 보내 주세요");
        }
        long id = jdbc.queryForObject("INSERT INTO inquiry (member_id, kind, title, content, email) VALUES (?, ?, ?, ?, ?) RETURNING id", // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
                Long.class, current.map(CurrentUser::memberId).orElse(null), kind, title, content, email);
        events.publishEvent(new InquiryReceived(id));
        return id;
    }

    /** 관리자 문의함: 새 문의 먼저, 그 안에서 최신순. */
    public List<Inquiry> list(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        return jdbc.query(SELECT + " ORDER BY i.status = 'DONE', i.id DESC LIMIT 500", ROW);
    }

    public Inquiry get(Optional<CurrentUser> current, long id) {
        adminGuard.requireAdmin(current);
        return jdbc.query(SELECT + " WHERE i.id = ?", ROW, id).stream().findFirst().orElseThrow(NotFoundException::new);
    }

    /** 답장 메일을 보내고 처리 완료로 바꾼다. 메일 실패는 발송기가 로그만 남긴다. */
    public void reply(Optional<CurrentUser> current, long id, String rawReply) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        Inquiry inquiry = get(current, id);
        String reply = rawReply == null ? "" : rawReply.strip().replace("\r\n", "\n");
        if (reply.isEmpty() || reply.codePointCount(0, reply.length()) > CONTENT_MAX) {
            throw new Rejected("답장은 1~" + CONTENT_MAX + "자로 적어 주세요");
        }
        jdbc.update("UPDATE inquiry SET status = 'DONE', reply = ?, replied_by = ?, replied_at = ? WHERE id = ?", reply,
                admin.memberId(), Timestamp.from(clock.instant()), id);
        mailSender.send(inquiry.email(), "[글그린] 문의에 답장드려요: " + inquiry.title(), "mail/inquiry-reply",
                Map.of("title", inquiry.title(), "content", inquiry.content(), "reply", reply));
    }

    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM inquiry WHERE member_id = ?", memberId);
    }
}
