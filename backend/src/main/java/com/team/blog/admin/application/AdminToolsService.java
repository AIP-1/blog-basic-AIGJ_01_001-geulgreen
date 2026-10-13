package com.team.blog.admin.application;

import com.team.blog.moderation.application.AdminGuard;
import com.team.blog.moderation.application.ReportReason;
import com.team.blog.post.application.PostCounters;
import com.team.blog.shared.error.FieldError;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.ProfileValidationException;
import com.team.blog.shared.event.ContentHidden;
import com.team.blog.shared.security.CurrentUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 화면 확장(045~050, 강성찬 개인 확장). 모든 동작은 관리자만(그 밖에는 없는 주소와 같은 404, 42 P-10),
 * 바꾸는 동작은 관리 기록(admin_log)에 남긴다.
 */
@Service
public class AdminToolsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public record Day(LocalDate date, long members, long posts, long comments) {
    }

    public record Dashboard(long totalMembers, long totalPosts, long totalComments, long pendingReports, long todayMembers,
                            long todayPosts, long todayComments, List<Day> week, long maxDaily) {
    }

    public record PostRow(long id, String title, String handle, String nickname, String visibility, Instant publishedAt,
                          boolean hidden, String hiddenReason, long views, int comments) {

        public String url() {
            return "/" + handle + "/posts/" + id;
        }
    }

    public record CommentRow(long id, long postId, String postTitle, String postHandle, String content, String handle,
                             String nickname, Instant createdAt, boolean hidden) {

        public String url() {
            return "/" + postHandle + "/posts/" + postId + "?comment=" + id + "#comment-" + id;
        }
    }

    public record GuestbookRow(long id, String ownerHandle, String content, boolean secret, String handle, String nickname,
                               Instant createdAt) {
    }

    public record Notice(long id, String message, String linkUrl, boolean active, Instant createdAt) {
    }

    public record LogRow(Instant at, String admin, String action, String target, String detail) {
    }

    private final JdbcTemplate jdbc;
    private final AdminGuard adminGuard;
    private final PostCounters postCounters;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AdminToolsService(JdbcTemplate jdbc, AdminGuard adminGuard, PostCounters postCounters,
                             ApplicationEventPublisher events, Clock clock) {
        this.jdbc = jdbc;
        this.adminGuard = adminGuard;
        this.postCounters = postCounters;
        this.events = events;
        this.clock = clock;
    }

    // ----- 045 대시보드 -----

    public Dashboard dashboard(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
        Instant from = today.minusDays(6).atStartOfDay(SEOUL).toInstant();
        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            days.put(today.minusDays(i), new long[3]);
        }
        count(days, "SELECT (created_at AT TIME ZONE 'Asia/Seoul')::date AS d, count(*) FROM member WHERE created_at >= ?"
                + " AND withdrawn_at IS NULL GROUP BY 1", from, 0);
        count(days, "SELECT (first_public_at AT TIME ZONE 'Asia/Seoul')::date AS d, count(*) FROM post WHERE first_public_at >= ?"
                + " AND deleted_at IS NULL GROUP BY 1", from, 1);
        count(days, "SELECT (created_at AT TIME ZONE 'Asia/Seoul')::date AS d, count(*) FROM comment WHERE created_at >= ?"
                + " AND deleted_at IS NULL GROUP BY 1", from, 2);
        List<Day> week = new ArrayList<>();
        long max = 1;
        for (Map.Entry<LocalDate, long[]> e : days.entrySet()) {
            long[] v = e.getValue();
            week.add(new Day(e.getKey(), v[0], v[1], v[2]));
            max = Math.max(max, Math.max(v[0], Math.max(v[1], v[2])));
        }
        long[] t = days.get(today);
        return new Dashboard(one("SELECT count(*) FROM member WHERE withdrawn_at IS NULL"),
                one("SELECT count(*) FROM post WHERE status = 'PUBLISHED' AND deleted_at IS NULL"),
                one("SELECT count(*) FROM comment WHERE deleted_at IS NULL"),
                one("SELECT count(*) FROM report_case WHERE status = 'PENDING'"), t[0], t[1], t[2], week, max);
    }

    private void count(Map<LocalDate, long[]> days, String sql, Instant from, int slot) {
        jdbc.query(sql, rs -> {
            long[] v = days.get(rs.getDate(1).toLocalDate());
            if (v != null) {
                v[slot] = rs.getLong(2);
            }
        }, Timestamp.from(from));
    }

    private long one(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    // ----- 047 글·댓글 관리 -----

    public List<PostRow> posts(Optional<CurrentUser> current, String q) {
        adminGuard.requireAdmin(current);
        return jdbc.query("""
                SELECT p.id, p.title, m.handle, m.nickname, p.visibility, p.published_at, p.hidden_at, p.hidden_reason,
                       p.view_count, p.comment_count
                FROM post p JOIN member m ON m.id = p.author_id
                WHERE p.status = 'PUBLISHED' AND p.deleted_at IS NULL AND (p.title ILIKE ? ESCAPE '\\' OR m.handle ILIKE ? ESCAPE '\\'
                      OR m.nickname ILIKE ? ESCAPE '\\')
                ORDER BY p.published_at DESC, p.id DESC LIMIT 50
                """, (rs, i) -> new PostRow(rs.getLong("id"), rs.getString("title"), rs.getString("handle"), rs.getString("nickname"),
                rs.getString("visibility"), rs.getTimestamp("published_at").toInstant(), rs.getTimestamp("hidden_at") != null,
                rs.getString("hidden_reason"), rs.getLong("view_count"), rs.getInt("comment_count")),
                like(q), like(q), like(q));
    }

    public List<CommentRow> comments(Optional<CurrentUser> current, String q) {
        adminGuard.requireAdmin(current);
        return jdbc.query("""
                SELECT c.id, c.post_id, p.title, pm.handle AS post_handle, c.content, m.handle, m.nickname, c.created_at, c.hidden_at
                FROM comment c JOIN post p ON p.id = c.post_id JOIN member pm ON pm.id = p.author_id JOIN member m ON m.id = c.author_id
                WHERE c.deleted_at IS NULL AND p.deleted_at IS NULL AND (c.content ILIKE ? ESCAPE '\\' OR m.handle ILIKE ? ESCAPE '\\'
                      OR m.nickname ILIKE ? ESCAPE '\\')
                ORDER BY c.created_at DESC, c.id DESC LIMIT 50
                """, (rs, i) -> new CommentRow(rs.getLong("id"), rs.getLong("post_id"), rs.getString("title"),
                rs.getString("post_handle"), rs.getString("content"), rs.getString("handle"), rs.getString("nickname"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("hidden_at") != null), like(q), like(q), like(q));
    }

    /** 신고 없이 바로 숨기기(022와 같은 숨김 칸·사유를 쓴다). */
    @Transactional
    public void hidePost(Optional<CurrentUser> current, long postId, String reasonRaw) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        ReportReason reason = reason(reasonRaw);
        Long author = jdbc.query("UPDATE post SET hidden_at = ?, hidden_by = ?, hidden_reason = ? WHERE id = ? AND hidden_at IS NULL"
                        + " AND deleted_at IS NULL RETURNING author_id", (rs, i) -> rs.getLong(1),
                Timestamp.from(clock.instant()), admin.memberId(), reason.name(), postId).stream().findFirst().orElse(null);
        if (author == null) {
            throw new NotFoundException();
        }
        events.publishEvent(new ContentHidden(postId, null, author));
        log(admin, "POST_HIDE", "글 #" + postId, reason.label());
    }

    @Transactional
    public void unhidePost(Optional<CurrentUser> current, long postId) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        if (jdbc.update("UPDATE post SET hidden_at = NULL, hidden_by = NULL, hidden_reason = NULL WHERE id = ? AND hidden_at IS NOT NULL",
                postId) == 0) {
            throw new NotFoundException();
        }
        log(admin, "POST_UNHIDE", "글 #" + postId, null);
    }

    @Transactional
    public void hideComment(Optional<CurrentUser> current, long commentId, String reasonRaw) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        ReportReason reason = reason(reasonRaw);
        List<long[]> rows = jdbc.query("UPDATE comment SET hidden_at = ?, hidden_by = ?, hidden_reason = ? WHERE id = ?"
                        + " AND hidden_at IS NULL AND deleted_at IS NULL RETURNING post_id, author_id",
                (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)}, Timestamp.from(clock.instant()), admin.memberId(),
                reason.name(), commentId);
        if (rows.isEmpty()) {
            throw new NotFoundException();
        }
        postCounters.adjustComments(rows.get(0)[0], -1);
        events.publishEvent(new ContentHidden(rows.get(0)[0], commentId, rows.get(0)[1]));
        log(admin, "COMMENT_HIDE", "댓글 #" + commentId, reason.label());
    }

    @Transactional
    public void unhideComment(Optional<CurrentUser> current, long commentId) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        List<Long> post = jdbc.query("UPDATE comment SET hidden_at = NULL, hidden_by = NULL, hidden_reason = NULL WHERE id = ?"
                + " AND hidden_at IS NOT NULL AND deleted_at IS NULL RETURNING post_id", (rs, i) -> rs.getLong(1), commentId);
        if (post.isEmpty()) {
            throw new NotFoundException();
        }
        postCounters.adjustComments(post.get(0), +1);
        log(admin, "COMMENT_UNHIDE", "댓글 #" + commentId, null);
    }

    // ----- 048 방명록 관리 -----

    public List<GuestbookRow> guestbook(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        return jdbc.query("""
                SELECT g.id, o.handle AS owner_handle, g.content, g.secret, m.handle, m.nickname, g.created_at
                FROM guestbook_entry g JOIN member o ON o.id = g.owner_id JOIN member m ON m.id = g.author_id
                ORDER BY g.id DESC LIMIT 100
                """, (rs, i) -> new GuestbookRow(rs.getLong("id"), rs.getString("owner_handle"), rs.getString("content"),
                rs.getBoolean("secret"), rs.getString("handle"), rs.getString("nickname"), rs.getTimestamp("created_at").toInstant()));
    }

    @Transactional
    public void deleteGuestbook(Optional<CurrentUser> current, long id) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        if (jdbc.update("DELETE FROM guestbook_entry WHERE id = ?", id) == 0) {
            throw new NotFoundException();
        }
        log(admin, "GUESTBOOK_DELETE", "방명록 #" + id, null);
    }

    // ----- 049 사이트 공지 -----

    public List<Notice> notices(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        return jdbc.query("SELECT id, message, link_url, active, created_at FROM site_notice ORDER BY id DESC LIMIT 50",
                (rs, i) -> new Notice(rs.getLong("id"), rs.getString("message"), rs.getString("link_url"), rs.getBoolean("active"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    /** 새 공지를 켠다(켜진 공지는 하나만: 예전 것은 끈다). 링크는 이 사이트 주소(/로 시작)나 https만. */
    @Transactional
    public void postNotice(Optional<CurrentUser> current, String rawMessage, String rawLink) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        String message = rawMessage == null ? "" : rawMessage.strip();
        if (message.isEmpty() || message.codePointCount(0, message.length()) > 200) {
            throw new ProfileValidationException(FieldError.of("message", "NOTICE_MESSAGE_INVALID"));
        }
        String link = rawLink == null || rawLink.isBlank() ? null : rawLink.strip();
        if (link != null && !((link.startsWith("/") && !link.startsWith("//")) || link.startsWith("https://")) || link != null
                && link.length() > 300) {
            throw new ProfileValidationException(FieldError.of("linkUrl", "NOTICE_LINK_INVALID"));
        }
        jdbc.update("UPDATE site_notice SET active = false WHERE active");
        jdbc.update("INSERT INTO site_notice (message, link_url, created_by) VALUES (?, ?, ?)", message, link, admin.memberId());
        log(admin, "NOTICE_POST", "공지", message);
    }

    @Transactional
    public void turnOffNotice(Optional<CurrentUser> current, long id) {
        CurrentUser admin = adminGuard.requireAdmin(current);
        if (jdbc.update("UPDATE site_notice SET active = false WHERE id = ? AND active", id) == 0) {
            throw new NotFoundException();
        }
        log(admin, "NOTICE_OFF", "공지 #" + id, null);
    }

    /** 지금 켜진 공지(모든 화면 위 띠). */
    public Optional<Notice> activeNotice() {
        return jdbc.query("SELECT id, message, link_url, active, created_at FROM site_notice WHERE active ORDER BY id DESC LIMIT 1",
                (rs, i) -> new Notice(rs.getLong("id"), rs.getString("message"), rs.getString("link_url"), true,
                        rs.getTimestamp("created_at").toInstant())).stream().findFirst();
    }

    // ----- 050 관리 기록 -----

    /** 이 화면에서 한 일 + 신고 처리(022) + 회원 정지(022)를 한 줄로, 최신순. */
    public List<LogRow> logs(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        return jdbc.query("""
                SELECT at, admin, action, target, detail FROM (
                  SELECT l.created_at AS at, a.nickname AS admin, l.action, l.target, l.detail
                  FROM admin_log l JOIN member a ON a.id = l.admin_id
                  UNION ALL
                  SELECT c.handled_at, a.nickname, 'REPORT_' || c.status, '신고 #' || c.id, NULL
                  FROM report_case c JOIN member a ON a.id = c.handled_by WHERE c.handled_at IS NOT NULL
                  UNION ALL
                  SELECT s.started_at, a.nickname, 'SUSPEND', '@' || m.handle, s.reason
                  FROM member_suspension s JOIN member m ON m.id = s.member_id JOIN member a ON a.id = s.suspended_by
                ) x ORDER BY at DESC LIMIT 100
                """, (rs, i) -> new LogRow(rs.getTimestamp("at").toInstant(), rs.getString("admin"), rs.getString("action"),
                rs.getString("target"), rs.getString("detail")));
    }

    private void log(CurrentUser admin, String action, String target, String detail) {
        jdbc.update("INSERT INTO admin_log (admin_id, action, target, detail) VALUES (?, ?, ?, ?)", admin.memberId(), action,
                target, detail == null ? null : detail.length() > 300 ? detail.substring(0, 300) : detail);
    }

    private static ReportReason reason(String raw) {
        return ReportReason.parse(raw).orElseThrow(() -> new ProfileValidationException(FieldError.of("reason", "INVALID_HIDE_REASON")));
    }

    private static String like(String q) {
        return "%" + (q == null ? "" : q.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")) + "%";
    }
}
