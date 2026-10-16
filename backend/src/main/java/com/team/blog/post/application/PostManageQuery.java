package com.team.blog.post.application;

import com.team.blog.post.domain.PostStatus;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.team.blog.post.markdown.ContentRenderer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 내 글 관리(011 R-1, 41 §5). 대상은 로그인 정보로만 정한다. 탭마다 20개(21개 조회로 다음 판단), 본문 칸 없이 SQL 1번
 * (작업본 유무는 LEFT JOIN). 정렬·커서: 임시·발행 {@code (updated_at, id)}, 휴지통 {@code (deleted_at, id)}.
 */
@Service
public class PostManageQuery {

    /** 062 강성찬 개인 확장: 아무것도 안 쓴 임시글은 만든 지(다시 연 지) 2분이 지나면 목록·개수에서 뺀다(자동 저장이 DB에 반영되기 전 글은 보이게). */
    static final java.time.Duration BLANK_DRAFT_GRACE = java.time.Duration.ofMinutes(2);
    private static final String BLANK_DRAFT = "p.status = 'DRAFT' AND p.deleted_at IS NULL AND btrim(p.title, E' \\t\\r\\n') = ''"
            + " AND btrim(p.content_md, E' \\t\\r\\n') = '' AND p.updated_at < ?";

    private final AccountGuard accountGuard;
    private final JdbcTemplate jdbc;
    private final PostProperties properties;
    private final java.time.Clock clock;
    private final ContentRenderer renderer;

    public PostManageQuery(AccountGuard accountGuard, JdbcTemplate jdbc, PostProperties properties, java.time.Clock clock,
                           ContentRenderer renderer) {
        this.clock = clock;
        this.renderer = renderer;
        this.accountGuard = accountGuard;
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public ManagePage page(Optional<CurrentUser> currentUser, ManageTab tab, String visibility, String cursor) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUser);
        Optional<FeedCursor> after = FeedCursor.decode(cursor);
        int size = properties.managePageSize();
        String sortColumn = tab == ManageTab.TRASH ? "p.deleted_at" : "p.updated_at";
        StringBuilder sql = new StringBuilder(select(tab));
        List<Object> args = new ArrayList<>();
        sql.append(where(tab, visibility, user.memberId(), args));
        if (after.isPresent()) {
            sql.append(" AND (").append(sortColumn).append(", p.id) < (?, ?)");
            args.add(Timestamp.from(after.get().firstPublicAt()));
            args.add(after.get().id());
        }
        sql.append(" ORDER BY ").append(sortColumn).append(" DESC, p.id DESC LIMIT ").append(size + 1);
        List<ManageRow> rows = jdbc.query(sql.toString(), (rs, n) -> row(rs, tab == ManageTab.TRASH), args.toArray());
        String next = null;
        if (rows.size() > size) {
            rows = rows.subList(0, size);
            ManageRow last = rows.get(size - 1);
            next = new FeedCursor(tab == ManageTab.TRASH ? last.deletedAt() : last.updatedAt(), last.id()).encode();
        }
        Map<String, Long> counts = after.isEmpty() ? counts(user.memberId()) : null;
        return new ManagePage(List.copyOf(rows), next, counts);
    }

    /** 목록 줄 SELECT … WHERE p.author_id = ?(조건은 {@link #where}). */
    private static String select(ManageTab tab) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.title, p.status, p.visibility, p.updated_at, p.published_at, p.edited_at, p.deleted_at,
                       p.view_count, p.like_count, p.comment_count, p.hidden_at, p.edit_version,
                       d.edit_version AS d_version, m.handle,
                       COALESCE(tz.teaser, p.excerpt) AS excerpt, COALESCE(pcv.thumb_url, p.thumbnail_url) AS thumbnail_url,
                       (SELECT t.name FROM post_tag pt JOIN tag t ON t.id = pt.tag_id
                        WHERE pt.post_id = p.id ORDER BY pt.position LIMIT 1) AS first_tag""");
        // 휴지통 탭만: 발행 전에 지운 글은 요약이 없어 본문 앞부분으로 미리보기를 만든다(제목 없는 글 구분)
        sql.append(tab == ManageTab.TRASH ? ", CASE WHEN p.excerpt IS NULL THEN left(p.content_md, 4000) END AS preview_md" : "");
        sql.append("""

                FROM post p JOIN member m ON m.id = p.author_id LEFT JOIN post_draft d ON d.post_id = p.id
                     LEFT JOIN post_teaser tz ON tz.post_id = p.id LEFT JOIN post_cover pcv ON pcv.post_id = p.id
                WHERE p.author_id = ?""");
        return sql.toString();
    }

    /** 탭·공개 범위 필터 조건(" AND ..."). {@code args}에 값을 더한다. */
    private String where(ManageTab tab, String visibility, long memberId, List<Object> args) {
        StringBuilder sql = new StringBuilder();
        args.add(memberId);
        switch (tab) {
            case DRAFTS -> {
                sql.append(" AND p.status = 'DRAFT' AND p.deleted_at IS NULL AND NOT (").append(BLANK_DRAFT).append(")");
                args.add(Timestamp.from(clock.instant().minus(BLANK_DRAFT_GRACE)));
            }
            case PUBLISHED -> {
                sql.append(" AND p.status = 'PUBLISHED' AND p.deleted_at IS NULL");
                String v = visibility == null ? null : visibility.toUpperCase(java.util.Locale.ROOT);
                if ("PUBLIC".equals(v) || "PRIVATE".equals(v) || "FRIENDS".equals(v) || "LINK".equals(v) || "GROUP".equals(v)) { // FRIENDS·LINK·GROUP: 025·029·030 강성찬 개인 확장
                    sql.append(" AND p.visibility = ?");
                    args.add(v);
                }
            }
            case TRASH -> sql.append(" AND p.deleted_at IS NOT NULL");
        }
        return sql.toString();
    }

    /**
     * 110 강성찬 개인 확장: 내 글 화면은 [더 보기] 대신 쪽 번호. 한 쪽은 {@code managePageSize}개, 정렬은 커서 조회와 같다.
     * 쪽이 끝 쪽보다 크면 끝 쪽을(휴지통으로 옮겨 쪽이 비었을 때 앞 쪽으로). 커서 조회(API·MCP)는 그대로 둔다.
     */
    public NumberedPage numbered(Optional<CurrentUser> currentUser, ManageTab tab, String visibility, int page) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUser);
        int size = properties.managePageSize();
        List<Object> countArgs = new ArrayList<>();
        Long total = jdbc.queryForObject("SELECT count(*) FROM post p WHERE p.author_id = ?" + where(tab, visibility, user.memberId(), countArgs),
                Long.class, countArgs.toArray());
        int totalPages = (int) Math.max(1, (total == null ? 0 : total + size - 1) / size);
        int shown = Math.min(Math.max(1, page), totalPages);
        String sortColumn = tab == ManageTab.TRASH ? "p.deleted_at" : "p.updated_at";
        List<Object> args = new ArrayList<>();
        String sql = select(tab) + where(tab, visibility, user.memberId(), args)
                + " ORDER BY " + sortColumn + " DESC, p.id DESC LIMIT " + size + " OFFSET " + ((long) (shown - 1) * size);
        List<ManageRow> rows = jdbc.query(sql, (rs, n) -> row(rs, tab == ManageTab.TRASH), args.toArray());
        return new NumberedPage(List.copyOf(rows), shown, totalPages, counts(user.memberId()));
    }

    /** 쪽 번호 조회 결과(110). */
    public record NumberedPage(List<ManageRow> items, int page, int totalPages, Map<String, Long> counts) {
    }

    /** 로그인한 회원의 탭별 글 수(화면에서 삭제·복구 뒤 탭 숫자 갱신용). */
    public Map<String, Long> counts(Optional<CurrentUser> currentUser) {
        return counts(accountGuard.requireLoggedIn(currentUser).memberId());
    }

    /** 탭별 글 수(GROUP BY 1번). */
    public Map<String, Long> counts(long memberId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("drafts", 0L);
        counts.put("published", 0L);
        counts.put("trash", 0L);
        jdbc.query("SELECT CASE WHEN p.deleted_at IS NOT NULL THEN 'trash' WHEN p.status = 'DRAFT' THEN 'drafts' ELSE 'published' END AS tab,"
                + " count(*) AS n FROM post p WHERE p.author_id = ? AND NOT (" + BLANK_DRAFT + ") GROUP BY 1", rs -> {
                    counts.put(rs.getString("tab"), rs.getLong("n"));
                }, memberId, Timestamp.from(clock.instant().minus(BLANK_DRAFT_GRACE)));
        return counts;
    }

    private ManageRow row(ResultSet rs, boolean trash) throws SQLException {
        String excerpt = rs.getString("excerpt");
        String previewMd = trash ? rs.getString("preview_md") : null;
        if (excerpt == null && previewMd != null && !previewMd.isBlank()) {
            try {
                excerpt = renderer.render(previewMd).excerpt(); // 화면에 보이는 글자만(정화된 요약과 같은 규칙)
            } catch (RuntimeException e) {
                excerpt = null; // 미리보기는 덤: 못 만들면 아래 안내
            }
            if (excerpt == null || excerpt.isBlank()) {
                excerpt = "글자 없이 사진·코드만 있어요"; // 본문은 있으니 "빈 임시글"이 아니다
            }
        }
        PostStatus status = PostStatus.valueOf(rs.getString("status"));
        long draftVersion = rs.getLong("d_version");
        boolean editing = status == PostStatus.PUBLISHED && !rs.wasNull() && draftVersion > rs.getLong("edit_version");
        Instant deletedAt = instant(rs.getTimestamp("deleted_at"));
        long id = rs.getLong("id");
        return new ManageRow(id, rs.getString("title"), status, rs.getString("visibility"), editing,
                rs.getTimestamp("hidden_at") != null, instant(rs.getTimestamp("updated_at")),
                instant(rs.getTimestamp("published_at")), instant(rs.getTimestamp("edited_at")), deletedAt,
                deletedAt == null ? null : deletedAt.plus(properties.trash().retention()),
                rs.getLong("view_count"), rs.getInt("like_count"), rs.getInt("comment_count"),
                com.team.blog.shared.web.BlogPaths.of(rs.getString("handle")) + "/posts/" + id,
                excerpt, rs.getString("thumbnail_url"), rs.getString("first_tag"));
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
