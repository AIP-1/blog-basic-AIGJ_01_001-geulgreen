package com.team.blog.contact.application;

import com.team.blog.account.application.AuthProperties;
import com.team.blog.account.application.EmailRules;
import com.team.blog.account.application.InvalidEmailException;
import com.team.blog.account.application.MailSender;
import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.moderation.application.AdminGuard;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.event.InquiryFollowedUp;
import com.team.blog.shared.event.InquiryReceived;
import com.team.blog.shared.event.InquiryReplied;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.web.AdminList;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 090 문의하기(강성찬 개인 확장): 문의 저장(IP마다 한 시간 횟수 제한), 관리자 문의함, 답장 메일.
 * 10-26: 문의를 대화로. 관리자는 여러 번 답장하고, 보낸 회원은 내 문의에서 다시 답한다(다시 답하면 새 문의로 돌아간다).
 */
@Service
public class InquiryService {

    /** 문의 종류(화면 순서대로). */
    public static final Map<String, String> KINDS = Map.of("ACCOUNT", "계정·로그인", "BUG", "버그 신고", "IDEA", "기능 제안",
            "OTHER", "기타");
    public static final List<String> KIND_ORDER = List.of("ACCOUNT", "BUG", "IDEA", "OTHER");
    public static final int TITLE_MAX = 100;
    public static final int CONTENT_MAX = 3000;
    private static final String LENGTH_HINT = "자로 적어 주세요";

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

    /** 10-26 문의 대화 한 줄: 관리자 답장이거나 보낸 회원의 다시 답하기. */
    public record Message(long id, boolean fromAdmin, String body, Instant createdAt) {
    }

    private static final RowMapper<Message> MESSAGE = (rs, n) -> new Message(rs.getLong("id"), rs.getBoolean("from_admin"),
            rs.getString("body"), rs.getTimestamp("created_at").toInstant());

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
    private final AccountGuard accountGuard;
    private final AuthProperties authProperties;
    private final TransactionTemplate tx;

    public InquiryService(JdbcTemplate jdbc, RedisRateLimiter rateLimiter, ContactProperties properties, AdminGuard adminGuard,
                          MailSender mailSender, ApplicationEventPublisher events, Clock clock,
                          AccountGuard accountGuard, AuthProperties authProperties, TransactionTemplate tx) {
        this.accountGuard = accountGuard;
        this.authProperties = authProperties;
        this.tx = tx;
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
        return current.flatMap(u -> jdbc.queryForList( // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
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
            throw new Rejected("제목은 1~" + TITLE_MAX + "" + LENGTH_HINT);
        }
        if (content.isEmpty() || content.codePointCount(0, content.length()) > CONTENT_MAX) {
            throw new Rejected("내용은 1~" + CONTENT_MAX + "" + LENGTH_HINT);
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

    /** 관리자 문의함: 새 문의 먼저, 그 안에서 최신순(098 후속: 상태 new·done·all, 검색, 쪽 넘김을 DB에서). */
    public AdminList<Inquiry> list(Optional<CurrentUser> current, String status, String q, int page) {
        adminGuard.requireAdmin(current);
        String p = AdminList.like(q);
        return AdminList.query(jdbc, SELECT + " WHERE (? <> 'new' OR i.status = 'NEW') AND (? <> 'done' OR i.status = 'DONE')"
                + " AND (i.title ILIKE ? ESCAPE '\\' OR i.content ILIKE ? ESCAPE '\\' OR i.email ILIKE ? ESCAPE '\\'"
                + " OR coalesce(m.handle, '') ILIKE ? ESCAPE '\\')", "i.status = 'DONE', i.id DESC", ROW, page,
                String.valueOf(status), String.valueOf(status), p, p, p, p);
    }

    public Inquiry get(Optional<CurrentUser> current, long id) {
        adminGuard.requireAdmin(current);
        return jdbc.query(SELECT + " WHERE i.id = ?", ROW, id).stream().findFirst().orElseThrow(NotFoundException::new);
    }

    /** 10-26 문의 대화(처음 문의 뒤에 오간 답장, 오래된 순). 권한은 부른 쪽(get·mine)이 이미 확인했다. */
    public List<Message> messages(long inquiryId) {
        return jdbc.query("SELECT id, from_admin, body, created_at FROM inquiry_message WHERE inquiry_id = ? ORDER BY id",
                MESSAGE, inquiryId);
    }

    /**
     * 관리자 답장: 대화에 더하고 처리 완료로 바꾼 뒤 메일도 보낸다(10-26: 처리 완료여도 또 답장할 수 있다).
     * 메일 실패는 발송기가 로그만 남긴다.
     */
    public void reply(Optional<CurrentUser> current, long id, String rawReply) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        Inquiry inquiry = get(current, id);
        String reply = body(rawReply, "답장");
        // 대화 줄과 상태는 문의 줄을 잠근 한 트랜잭션에서(동시에 온 다시 답하기가 상태를 덮어쓰지 않게). 메일은 커밋 뒤에
        List<Long> member = tx.execute(status -> {
            lock(id);
            Timestamp now = Timestamp.from(clock.instant());
            jdbc.update("INSERT INTO inquiry_message (inquiry_id, author_id, from_admin, body, created_at) VALUES (?, ?, true, ?, ?)",
                    id, admin.memberId(), reply, now);
            jdbc.update("UPDATE inquiry SET status = 'DONE', reply = ?, replied_by = ?, replied_at = ? WHERE id = ?", reply,
                    admin.memberId(), now, id);
            List<Long> owner = jdbc.queryForList("SELECT member_id FROM inquiry WHERE id = ? AND member_id IS NOT NULL",
                    Long.class, id);
            owner.forEach(memberId -> events.publishEvent(new InquiryReplied(id, memberId)));
            return owner;
        });
        mailSender.send(inquiry.email(), "[글그린] 문의에 답장드려요: " + inquiry.title(), "mail/inquiry-reply",
                Map.of("title", inquiry.title(), "content", inquiry.content(), "reply", reply,
                        "mineLink", member.isEmpty() ? "" : authProperties.mail().linkBaseUrl() + "/contact/mine/" + id));
    }

    /**
     * 10-26 답장 없이 처리 완료(고맙다는 다시 답하기처럼 더 답할 게 없을 때). seenLastMessageId는 관리자가 본 화면의 마지막 대화 줄
     * 번호(없으면 0): 그 뒤에 보낸 사람이 다시 답했다면 닫지 않는다. @return 닫았으면 true
     */
    public boolean close(Optional<CurrentUser> current, long id, long seenLastMessageId) {
        get(current, id);
        return jdbc.update("UPDATE inquiry SET status = 'DONE' WHERE id = ?"
                + " AND (SELECT coalesce(max(m.id), 0) FROM inquiry_message m WHERE m.inquiry_id = ?) = ?", id, id,
                seenLastMessageId) > 0;
    }

    /** 10-26 보낸 회원이 내 문의에서 다시 답한다: 대화에 더하고 새 문의로 돌려 관리자에게 알린다.
     * 횟수 제한은 새 문의(IP별)와 따로 회원별로 센다(다시 답하기가 새 문의 몫을 쓰지 않게). */
    public void followUp(Optional<CurrentUser> current, long id, String rawBody) {
        Inquiry inquiry = mine(current, id);
        String text = body(rawBody, "답장");
        long memberId = current.orElseThrow().memberId();
        if (!rateLimiter.tryAcquire(RedisRateLimiter.key("contact:send", "m" + memberId), properties.perHour(),
                Duration.ofHours(1)).allowed()) {
            throw new Rejected("답장을 너무 많이 보냈어요. 한 시간 뒤에 다시 보내 주세요");
        }
        tx.executeWithoutResult(status -> {
            lock(inquiry.id());
            jdbc.update("INSERT INTO inquiry_message (inquiry_id, author_id, from_admin, body, created_at) VALUES (?, ?, false, ?, ?)",
                    inquiry.id(), memberId, text, Timestamp.from(clock.instant()));
            jdbc.update("UPDATE inquiry SET status = 'NEW' WHERE id = ?", inquiry.id());
            events.publishEvent(new InquiryFollowedUp(inquiry.id()));
        });
    }

    private void lock(long id) {
        jdbc.queryForList("SELECT id FROM inquiry WHERE id = ? FOR UPDATE", Long.class, id);
    }

    private static String body(String raw, String what) {
        String text = raw == null ? "" : raw.strip().replace("\r\n", "\n");
        if (text.isEmpty() || text.codePointCount(0, text.length()) > CONTENT_MAX) {
            throw new Rejected(what + "은 1~" + CONTENT_MAX + "" + LENGTH_HINT);
        }
        return text;
    }

    /** 099 내 문의: 로그인한 회원이 보낸 문의(최신순). */
    public List<Inquiry> mine(Optional<CurrentUser> current) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        return jdbc.query(SELECT + " WHERE i.member_id = ? ORDER BY i.id DESC LIMIT 100", ROW, me.memberId());
    }

    /** 099 내 문의 하나. 남의 문의는 없는 주소와 같은 404. */
    public Inquiry mine(Optional<CurrentUser> current, long id) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        return jdbc.query(SELECT + " WHERE i.id = ? AND i.member_id = ?", ROW, id, me.memberId()).stream().findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM inquiry WHERE member_id = ?", memberId);
    }
}
