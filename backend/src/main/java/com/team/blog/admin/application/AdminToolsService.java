package com.team.blog.admin.application;

import com.team.blog.moderation.application.AdminGuard;
import com.team.blog.moderation.application.ReportReason;
import com.team.blog.post.application.PostCounters;
import com.team.blog.shared.error.FieldError;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.ProfileValidationException;
import com.team.blog.shared.event.ContentHidden;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.web.AdminList;
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


    public record Day(LocalDate date, long members, long posts, long comments, long views) {
    }

    /** 오늘 숫자와 비교 값(before). basis는 무엇과 비교했는지(화면 작은 글씨). */
    public record Delta(long now, long before, String basis) {

        public long diff() {
            return Math.abs(now - before);
        }

        public String dir() {
            return now > before ? "up" : now < before ? "down" : "same";
        }
    }

    /** 098 후속: 오늘 숫자 카드는 어제 같은 시각까지와 비교. 조회는 날짜별 합계뿐이라 어제 하루를 지난 시간 비율로 나눈 추정과 비교. */
    public record Dashboard(long totalMembers, long totalPosts, long totalComments, long pendingReports, long newInquiries,
                            Delta members, Delta posts, Delta comments, Delta views, Delta incoming) {
    }

    /** 그래프 한 점: 날짜 이름, 값, 위에서부터의 위치(0~100, viewBox와 툴팁 점이 같이 쓴다). */
    public record Point(String date, long value, double y) {
    }

    /**
     * 한 줄 추이. 그래프는 viewBox 0 0 300 100 안의 꺾은선(line)과 그 아래 면(area), 값이 0뿐이면 그리지 않는다.
     * 점은 날짜 칸의 가운데(x = (i + 0.5) / n)에 둬서 마우스·탭으로 여는 칸(trend-hit)과 맞춘다.
     */
    public record Trend(String label, String key, String unit, long total, long today, long max, String line, String area,
                        List<Point> points) {

        static Trend of(String label, String key, String unit, List<Day> days, java.util.function.ToLongFunction<Day> f) {
            long max = 0;
            long total = 0;
            for (Day d : days) {
                max = Math.max(max, f.applyAsLong(d));
                total += f.applyAsLong(d);
            }
            StringBuilder pts = new StringBuilder();
            List<Point> points = new ArrayList<>();
            int n = days.size();
            for (int i = 0; i < n; i++) {
                long v = f.applyAsLong(days.get(i));
                double y = round1(100 - (max == 0 ? 0 : v * 96.0 / max));
                pts.append(i == 0 ? "" : " ").append(x(i, n)).append(',').append(y);
                LocalDate d = days.get(i).date();
                points.add(new Point(d.getMonthValue() + "/" + d.getDayOfMonth(), v, y));
            }
            String line = pts.toString();
            return new Trend(label, key, unit, total, f.applyAsLong(days.get(n - 1)), max, line,
                    x(0, n) + ",100 " + line + " " + x(n - 1, n) + ",100", points);
        }

        private static double x(int i, int n) {
            return round1((i + 0.5) * 300.0 / n);
        }

        private static double round1(double v) {
            return Math.round(v * 10) / 10.0;
        }

        public boolean empty() {
            return total == 0;
        }
    }

    record Trends(Trend views, Trend members, Trend posts, Trend comments, List<Day> days) {
    }

    public record PopularPost(long id, String title, String handle, long views) {

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + id;
        }
    }

    public record Activity(Instant at, String ago, String kind, String handle, String what, String postHandle, Long postId,
                           Long commentId) {

        public String url() {
            if (postId == null) {
                return com.team.blog.shared.web.BlogPaths.of(handle);
            }
            return com.team.blog.shared.web.BlogPaths.of(postHandle) + "/posts/" + postId + (commentId == null ? "" : "?comment=" + commentId + "#comment-" + commentId);
        }
    }

    public record Insights(List<Trend> trends, List<Day> days, List<PopularPost> popular, List<Activity> activity) {
    }

    public record PostRow(long id, String title, String handle, String nickname, String visibility, Instant publishedAt,
                          boolean hidden, String hiddenReason, long views, int comments) {

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + id;
        }
    }

    public record CommentRow(long id, long postId, String postTitle, String postHandle, String content, String handle,
                             String nickname, Instant createdAt, boolean hidden) {

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(postHandle) + "/posts/" + postId + "?comment=" + id + "#comment-" + id;
        }
    }

    public record GuestbookRow(long id, String ownerHandle, String content, boolean secret, String handle, String nickname,
                               Instant createdAt) {
    }

    public record Notice(long id, String message, String linkUrl, boolean active, Instant createdAt) {
    }

    public record LogRow(Instant at, String admin, String action, String target, String detail) {

        private static final Map<String, String[]> LABELS = Map.ofEntries(
                Map.entry("POST_HIDE", new String[] {"글 숨김", "content"}), Map.entry("POST_UNHIDE", new String[] {"글 숨김 해제", "content"}),
                Map.entry("COMMENT_HIDE", new String[] {"댓글 숨김", "content"}),
                Map.entry("COMMENT_UNHIDE", new String[] {"댓글 숨김 해제", "content"}),
                Map.entry("GUESTBOOK_DELETE", new String[] {"방명록 삭제", "content"}),
                Map.entry("NOTICE_POST", new String[] {"공지 올림", "notice"}), Map.entry("NOTICE_OFF", new String[] {"공지 끔", "notice"}),
                Map.entry("REPORT_HIDDEN", new String[] {"신고 숨김 처리", "report"}),
                Map.entry("REPORT_REJECTED", new String[] {"신고 반려", "report"}),
                Map.entry("REPORT_CLOSED_NO_TARGET", new String[] {"신고 닫음(대상 없음)", "report"}),
                Map.entry("SUSPEND", new String[] {"회원 정지", "member"}),
                Map.entry("SAMPLE_POSTS_SEED", new String[] {"테스트 글 넣기", "etc"}),
                Map.entry("SAMPLE_POSTS_REMOVE", new String[] {"테스트 글 지우기", "etc"}));

        /** 화면에 보일 한국어 이름(모르는 코드는 그대로). */
        public String actionLabel() {
            String[] v = LABELS.get(action);
            return v == null ? action : v[0];
        }

        /** 한국어 이름에 q가 들어 있는 코드들(q가 비면 빈 배열: 다른 칸 ILIKE '%%'가 모두 고른다). */
        static String[] actionsMatching(String q) {
            if (q == null || q.isBlank()) {
                return new String[0];
            }
            return LABELS.entrySet().stream().filter(e -> e.getValue()[0].contains(q.strip())).map(Map.Entry::getKey)
                    .toArray(String[]::new);
        }

        /** 묶음에 든 코드들, 모르는 묶음(전체)이면 null. */
        static String[] actionsIn(String cat) {
            String[] hits = LABELS.entrySet().stream().filter(e -> e.getValue()[1].equals(cat) && !"etc".equals(cat))
                    .map(Map.Entry::getKey).toArray(String[]::new);
            return hits.length == 0 ? null : hits;
        }

        /** 필터 묶음: content·notice·report·member·etc. */
        public String category() {
            String[] v = LABELS.get(action);
            return v == null ? "etc" : v[1];
        }
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

    // ----- 045 대시보드, 098 30일 추이 -----

    /** 오늘 숫자 카드(한국 시간 자정부터 지금까지)와 어제 같은 시각까지의 비교. 조회는 016의 날짜별 조회수(post_view_daily)에서 온다. */
    public Dashboard dashboard(Optional<CurrentUser> current) {
        adminGuard.requireAdmin(current);
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, SEOUL);
        Instant[] range = {today.atStartOfDay(SEOUL).toInstant(), now, today.minusDays(1).atStartOfDay(SEOUL).toInstant(), null};
        java.time.Duration elapsed = java.time.Duration.between(range[0], now);
        range[3] = range[2].plus(elapsed);
        Delta members = same("SELECT count(*) FROM member WHERE withdrawn_at IS NULL AND created_at >= ? AND created_at < ?", range);
        Delta posts = same("SELECT count(*) FROM post WHERE deleted_at IS NULL AND first_public_at >= ? AND first_public_at < ?", range);
        Delta comments = same("SELECT count(*) FROM comment WHERE deleted_at IS NULL AND created_at >= ? AND created_at < ?", range);
        Delta reports = same("SELECT count(*) FROM report_case WHERE created_at >= ? AND created_at < ?", range);
        Delta inquiries = same("SELECT count(*) FROM inquiry WHERE created_at >= ? AND created_at < ?", range);
        long incomingBefore = reports.before() + inquiries.before();
        long viewsToday = views(today);
        long viewsYesterday = views(today.minusDays(1));
        long estimate = Math.round(viewsYesterday * (elapsed.toSeconds() / 86400.0));
        return new Dashboard(one("SELECT count(*) FROM member WHERE withdrawn_at IS NULL"),
                one("SELECT count(*) FROM post WHERE status = 'PUBLISHED' AND deleted_at IS NULL"),
                one("SELECT count(*) FROM comment WHERE deleted_at IS NULL"),
                one("SELECT count(*) FROM report_case WHERE status = 'PENDING'"),
                one("SELECT count(*) FROM inquiry WHERE status = 'NEW'"),
                members, posts, comments,
                new Delta(viewsToday, estimate, "어제 이 시각 추정 " + estimate + " · 어제 하루 " + viewsYesterday),
                new Delta(reports.now() + inquiries.now(), incomingBefore, SAME_TIME + incomingBefore));
    }

    private static final String SAME_TIME = "어제 이 시각까지 ";

    /** range = {오늘 0시, 지금, 어제 0시, 어제 이 시각}. */
    private Delta same(String sql, Instant[] range) {
        long now = count(sql, range[0], range[1]);
        long before = count(sql, range[2], range[3]);
        return new Delta(now, before, SAME_TIME + before);
    }

    private long count(String sql, Instant from, Instant to) {
        Long n = jdbc.queryForObject(sql, Long.class, Timestamp.from(from), Timestamp.from(to));
        return n == null ? 0 : n;
    }

    private long views(LocalDate day) {
        Long n = jdbc.queryForObject("SELECT coalesce(sum(views), 0) FROM post_view_daily WHERE view_date = ?", Long.class,
                java.sql.Date.valueOf(day));
        return n == null ? 0 : n;
    }

    /** 098: 최근 30일 추이와 인기 글(7일)·최근 활동(지금 공개된 글만). {@code full}이 아니면(스위치 끔) 예전처럼 7일 추이만 센다. */
    public Insights insights(Optional<CurrentUser> current, boolean full) {
        adminGuard.requireAdmin(current);
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);
        Trends t = trends(today, full ? 30 : 7);
        if (!full) {
            return new Insights(List.of(t.views(), t.members(), t.posts(), t.comments()), t.days(), List.of(), List.of());
        }
        List<PopularPost> popular = jdbc.query("""
                SELECT p.id, p.title, m.handle, sum(d.views) AS views FROM post_view_daily d
                JOIN post p ON p.id = d.post_id JOIN member m ON m.id = p.author_id
                WHERE d.view_date > ? AND p.status = 'PUBLISHED' AND p.visibility = 'PUBLIC' AND p.deleted_at IS NULL AND p.hidden_at IS NULL
                GROUP BY p.id, p.title, m.handle ORDER BY views DESC, p.id DESC LIMIT 5
                """, (rs, i) -> new PopularPost(rs.getLong("id"), rs.getString("title"), rs.getString("handle"), rs.getLong("views")),
                java.sql.Date.valueOf(today.minusDays(7)));
        List<Activity> activity = jdbc.query("""
                SELECT at, kind, who, what, ph, pid, cid FROM (
                  (SELECT created_at AS at, '가입' AS kind, handle AS who, nickname AS what, NULL AS ph, NULL::bigint AS pid,
                          NULL::bigint AS cid FROM member WHERE withdrawn_at IS NULL ORDER BY created_at DESC LIMIT 8)
                  UNION ALL
                  (SELECT p.first_public_at, '글', m.handle, p.title, m.handle, p.id, NULL
                   FROM post p JOIN member m ON m.id = p.author_id
                   WHERE p.status = 'PUBLISHED' AND p.visibility = 'PUBLIC' AND p.deleted_at IS NULL AND p.hidden_at IS NULL ORDER BY p.first_public_at DESC LIMIT 8)
                  UNION ALL
                  (SELECT c.created_at, '댓글', m.handle, p.title, pm.handle, p.id, c.id
                   FROM comment c JOIN member m ON m.id = c.author_id JOIN post p ON p.id = c.post_id JOIN member pm ON pm.id = p.author_id
                   WHERE c.deleted_at IS NULL AND c.hidden_at IS NULL AND p.status = 'PUBLISHED' AND p.visibility = 'PUBLIC' AND p.deleted_at IS NULL AND p.hidden_at IS NULL ORDER BY c.created_at DESC LIMIT 8)
                ) x ORDER BY at DESC LIMIT 8
                """, (rs, i) -> new Activity(rs.getTimestamp("at").toInstant(),
                com.team.blog.post.application.CardDates.label(rs.getTimestamp("at").toInstant(), clock.instant()),
                rs.getString("kind"), rs.getString("who"),
                rs.getString("what"), rs.getString("ph"), (Long) rs.getObject("pid"), (Long) rs.getObject("cid")));
        return new Insights(List.of(t.views(), t.members(), t.posts(), t.comments()), t.days(), popular, activity);
    }

    private Trends trends(LocalDate today, int n) {
        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        for (int i = n - 1; i >= 0; i--) {
            days.put(today.minusDays(i), new long[4]);
        }
        Instant from = today.minusDays(n - 1L).atStartOfDay(SEOUL).toInstant();
        count(days, "SELECT (created_at AT TIME ZONE 'Asia/Seoul')::date AS d, count(*) FROM member WHERE created_at >= ?"
                + " AND withdrawn_at IS NULL GROUP BY 1", from, 0);
        count(days, "SELECT (first_public_at AT TIME ZONE 'Asia/Seoul')::date AS d, count(*) FROM post WHERE first_public_at >= ?"
                + " AND deleted_at IS NULL GROUP BY 1", from, 1);
        count(days, "SELECT (created_at AT TIME ZONE 'Asia/Seoul')::date AS d, count(*) FROM comment WHERE created_at >= ?"
                + " AND deleted_at IS NULL GROUP BY 1", from, 2);
        jdbc.query("SELECT view_date, sum(views) FROM post_view_daily WHERE view_date >= ? GROUP BY 1", rs -> {
            long[] v = days.get(rs.getDate(1).toLocalDate());
            if (v != null) {
                v[3] = rs.getLong(2);
            }
        }, java.sql.Date.valueOf(today.minusDays(n - 1L)));
        List<Day> list = new ArrayList<>();
        days.forEach((d, v) -> list.add(new Day(d, v[0], v[1], v[2], v[3])));
        return new Trends(Trend.of("조회", "views", "회", list, Day::views), Trend.of("가입", "members", "명", list, Day::members),
                Trend.of("새 글", "posts", "편", list, Day::posts), Trend.of("댓글", "comments", "개", list, Day::comments), list);
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

    /** 047 글 목록(098 후속: 검색·숨긴 것만·쪽 넘김을 DB에서). */
    public AdminList<PostRow> posts(Optional<CurrentUser> current, String q, boolean hiddenOnly, int page) {
        adminGuard.requireAdmin(current);
        return AdminList.query(jdbc, """
                SELECT p.id, p.title, m.handle, m.nickname, p.visibility, p.published_at, p.hidden_at, p.hidden_reason,
                       p.view_count, p.comment_count
                FROM post p JOIN member m ON m.id = p.author_id
                WHERE p.status = 'PUBLISHED' AND p.deleted_at IS NULL AND (p.title ILIKE ? ESCAPE '\\' OR m.handle ILIKE ? ESCAPE '\\'
                      OR m.nickname ILIKE ? ESCAPE '\\') AND (NOT ? OR p.hidden_at IS NOT NULL)
                """, "p.published_at DESC, p.id DESC", (rs, n) -> new PostRow(rs.getLong("id"), rs.getString("title"),
                rs.getString("handle"), rs.getString("nickname"), rs.getString("visibility"), rs.getTimestamp("published_at").toInstant(),
                rs.getTimestamp("hidden_at") != null, rs.getString("hidden_reason"), rs.getLong("view_count"), rs.getInt("comment_count")),
                page, like(q), like(q), like(q), hiddenOnly);
    }

    public AdminList<CommentRow> comments(Optional<CurrentUser> current, String q, boolean hiddenOnly, int page) {
        adminGuard.requireAdmin(current);
        return AdminList.query(jdbc, """
                SELECT c.id, c.post_id, p.title, pm.handle AS post_handle, c.content, m.handle, m.nickname, c.created_at, c.hidden_at
                FROM comment c JOIN post p ON p.id = c.post_id JOIN member pm ON pm.id = p.author_id JOIN member m ON m.id = c.author_id
                WHERE c.deleted_at IS NULL AND p.deleted_at IS NULL AND (c.content ILIKE ? ESCAPE '\\' OR m.handle ILIKE ? ESCAPE '\\'
                      OR m.nickname ILIKE ? ESCAPE '\\') AND (NOT ? OR c.hidden_at IS NOT NULL)
                """, "c.created_at DESC, c.id DESC", (rs, n) -> new CommentRow(rs.getLong("id"), rs.getLong("post_id"),
                rs.getString("title"), rs.getString("post_handle"), rs.getString("content"), rs.getString("handle"), rs.getString("nickname"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("hidden_at") != null),
                page, like(q), like(q), like(q), hiddenOnly);
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

    public AdminList<GuestbookRow> guestbook(Optional<CurrentUser> current, String q, boolean secretOnly, int page) {
        adminGuard.requireAdmin(current);
        return AdminList.query(jdbc, """
                SELECT g.id, o.handle AS owner_handle, g.content, g.secret, m.handle, m.nickname, g.created_at
                FROM guestbook_entry g JOIN member o ON o.id = g.owner_id JOIN member m ON m.id = g.author_id
                WHERE (g.content ILIKE ? ESCAPE '\\' OR m.handle ILIKE ? ESCAPE '\\' OR m.nickname ILIKE ? ESCAPE '\\'
                       OR o.handle ILIKE ? ESCAPE '\\') AND (NOT ? OR g.secret)
                """, "g.id DESC", (rs, n) -> new GuestbookRow(rs.getLong("id"), rs.getString("owner_handle"), rs.getString("content"),
                rs.getBoolean("secret"), rs.getString("handle"), rs.getString("nickname"), rs.getTimestamp("created_at").toInstant()),
                page, like(q), like(q), like(q), like(q), secretOnly);
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

    public AdminList<Notice> notices(Optional<CurrentUser> current, String q, int page) {
        adminGuard.requireAdmin(current);
        return AdminList.query(jdbc, "SELECT id, message, link_url, active, created_at FROM site_notice"
                        + " WHERE message ILIKE ? ESCAPE '\\' OR coalesce(link_url, '') ILIKE ? ESCAPE '\\'", "id DESC",
                (rs, n) -> new Notice(rs.getLong("id"), rs.getString("message"), rs.getString("link_url"), rs.getBoolean("active"),
                        rs.getTimestamp("created_at").toInstant()), page, like(q), like(q));
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

    /**
     * 이 화면에서 한 일 + 신고 처리(022) + 회원 정지(022)를 한 줄로, 최신순. 찾을 말은 관리자·대상·내용·코드와 한국어 이름(글 숨김 등)에서,
     * cat은 한 일 묶음(report·member·content·notice, 그 밖은 전체).
     */
    public AdminList<LogRow> logs(Optional<CurrentUser> current, String q, String cat, int page) {
        adminGuard.requireAdmin(current);
        String[] labelHits = LogRow.actionsMatching(q);
        String[] inCat = LogRow.actionsIn(cat);
        return AdminList.query(jdbc, """
                SELECT at, admin, action, target, detail FROM (
                  SELECT l.created_at AS at, a.nickname AS admin, l.action, l.target, l.detail
                  FROM admin_log l JOIN member a ON a.id = l.admin_id
                  UNION ALL
                  SELECT c.handled_at, a.nickname, 'REPORT_' || c.status, '신고 #' || c.id, NULL
                  FROM report_case c JOIN member a ON a.id = c.handled_by WHERE c.handled_at IS NOT NULL
                  UNION ALL
                  SELECT s.started_at, a.nickname, 'SUSPEND', '@' || m.handle, s.reason
                  FROM member_suspension s JOIN member m ON m.id = s.member_id JOIN member a ON a.id = s.suspended_by
                ) x
                WHERE (admin ILIKE ? ESCAPE '\\' OR target ILIKE ? ESCAPE '\\' OR coalesce(detail, '') ILIKE ? ESCAPE '\\'
                       OR action ILIKE ? ESCAPE '\\' OR action = ANY (?)) AND (? OR action = ANY (?))
                """, "at DESC", (rs, n) -> new LogRow(rs.getTimestamp("at").toInstant(), rs.getString("admin"), rs.getString("action"),
                rs.getString("target"), rs.getString("detail")), page, like(q), like(q), like(q), like(q), labelHits,
                inCat == null, inCat == null ? new String[0] : inCat);
    }

    private void log(CurrentUser admin, String action, String target, String detail) {
        jdbc.update("INSERT INTO admin_log (admin_id, action, target, detail) VALUES (?, ?, ?, ?)", admin.memberId(), action,
                target, detail == null ? null : detail.length() > 300 ? detail.substring(0, 300) : detail);
    }

    private static ReportReason reason(String raw) {
        return ReportReason.parse(raw).orElseThrow(() -> new ProfileValidationException(FieldError.of("reason", "INVALID_HIDE_REASON")));
    }

    private static String like(String q) {
        return AdminList.like(q);
    }
}
