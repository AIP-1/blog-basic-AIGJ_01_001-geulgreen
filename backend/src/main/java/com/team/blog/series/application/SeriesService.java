package com.team.blog.series.application;

import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.application.PostCard;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 041 시리즈(강성찬 개인 확장): 내 글을 이름 붙인 묶음에 넣은 순서대로. 글은 시리즈 0~1개, 비면 시리즈도 지운다.
 * 목록·편 번호는 공용 목록 조건(012) 글만 센다.
 */
@Service
public class SeriesService {

    public record Series(long id, String title, long postCount) {
    }

    public record Item(long id, String title, String handle) {

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + id;
        }
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final PostAccessPolicy accessPolicy;
    private final SeriesProperties properties;

    public SeriesService(JdbcTemplate jdbc, AccountGuard accountGuard, PostAccessPolicy accessPolicy,
                         SeriesProperties properties) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.accessPolicy = accessPolicy;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 블로그의 시리즈(공개 글 수 포함, 최근에 만든 순). {@code includeEmpty}: 주인에게는 공개 글이 없는 것도. */
    public List<Series> list(long memberId, boolean includeEmpty) {
        List<Series> all = jdbc.query("SELECT s.id, s.title, (SELECT count(*) FROM series_post sp JOIN post p ON p.id = sp.post_id"
                        + " JOIN member m ON m.id = p.author_id WHERE sp.series_id = s.id AND "
                        + accessPolicy.blogListingCondition("p", "m") + ") AS cnt FROM series s WHERE s.member_id = ?"
                        + " ORDER BY s.id DESC",
                (rs, i) -> new Series(rs.getLong("id"), rs.getString("title"), rs.getLong("cnt")), memberId);
        return includeEmpty ? all : all.stream().filter(s -> s.postCount() > 0).toList();
    }

    /** 탐색 시리즈 탭에 보일 한 줄: 누구의 시리즈인지와 공개 글 수. */
    public record Browse(long id, String title, String handle, long postCount) {

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/series/" + id;
        }
    }

    static final int BROWSE_LIMIT = 60;

    /**
     * 104 강성찬 개인 확장: 모든 블로그의 시리즈(공용 목록 조건 글이 1편 이상). popular = 좋아요 합 → 글 수 → 최근 글,
     * recent = 최근에 글이 공개된 순. ponytail: 앞 {@value #BROWSE_LIMIT}개만, 시리즈가 많아지면 더 보기를 붙인다.
     */
    public List<Browse> browse(String sort) {
        String order = "recent".equals(sort) ? "latest DESC, s.id DESC" : "likes DESC, cnt DESC, latest DESC, s.id DESC";
        return jdbc.query("SELECT s.id, s.title, owner.handle, count(*) AS cnt, sum(p.like_count) AS likes,"
                        + " max(COALESCE(p.first_public_at, p.published_at)) AS latest FROM series s"
                        + " JOIN member owner ON owner.id = s.member_id JOIN series_post sp ON sp.series_id = s.id"
                        + " JOIN post p ON p.id = sp.post_id JOIN member m ON m.id = p.author_id WHERE "
                        + accessPolicy.publicListingCondition("p", "m")
                        + " GROUP BY s.id, s.title, owner.handle ORDER BY " + order + " LIMIT " + BROWSE_LIMIT,
                (rs, i) -> new Browse(rs.getLong("id"), rs.getString("title"), rs.getString("handle"), rs.getLong("cnt")));
    }

    public Optional<Series> find(long memberId, long seriesId) {
        return list(memberId, true).stream().filter(s -> s.id() == seriesId).findFirst();
    }

    /** 시리즈의 공개 글(넣은 순서). */
    public List<Item> items(long seriesId) {
        return jdbc.query("SELECT p.id, p.title, m.handle FROM series_post sp JOIN post p ON p.id = sp.post_id"
                        + " JOIN member m ON m.id = p.author_id WHERE sp.series_id = ? AND "
                        + accessPolicy.blogListingCondition("p", "m") + " ORDER BY sp.position, p.id",
                (rs, i) -> new Item(rs.getLong("id"), rs.getString("title"), rs.getString("handle")), seriesId);
    }

    /** 067 후속(강성찬 개인 확장): 시리즈 화면용 — items와 같은 글·순서를 홈과 같은 카드 모양으로. */
    public List<PostCard> cards(long seriesId) {
        return jdbc.query("""
                SELECT p.id, p.title, COALESCE(tz.teaser, p.excerpt) AS excerpt, COALESCE(pcv.thumb_url, p.thumbnail_url) AS thumbnail_url,
                       COALESCE(p.first_public_at, p.published_at) AS first_public_at, p.comment_count, p.like_count,
                       m.handle, m.nickname, m.profile_image_url,
                       (SELECT t.name FROM post_tag pt JOIN tag t ON t.id = pt.tag_id
                        WHERE pt.post_id = p.id ORDER BY pt.position LIMIT 1) AS first_tag
                FROM series_post sp JOIN post p ON p.id = sp.post_id JOIN member m ON m.id = p.author_id
                     LEFT JOIN post_teaser tz ON tz.post_id = p.id LEFT JOIN post_cover pcv ON pcv.post_id = p.id
                WHERE sp.series_id = ?""" + " AND " + accessPolicy.blogListingCondition("p", "m") + " ORDER BY sp.position, p.id",
                (rs, i) -> new PostCard(rs.getLong("id"), com.team.blog.shared.web.BlogPaths.of(rs.getString("handle")) + "/posts/" + rs.getLong("id"),
                        rs.getString("title"), rs.getString("excerpt") == null ? "" : rs.getString("excerpt"),
                        rs.getString("thumbnail_url"), rs.getTimestamp("first_public_at") == null ? null
                        : rs.getTimestamp("first_public_at").toInstant(), rs.getInt("comment_count"), rs.getInt("like_count"),
                        new PostCard.Author(rs.getString("handle"), rs.getString("nickname"), rs.getString("profile_image_url")),
                        rs.getString("first_tag")), seriesId);
    }

    /** 글이 든 시리즈(없으면 빈 값). */
    public Optional<Series> ofPost(long postId) {
        return jdbc.query("SELECT s.id, s.title FROM series_post sp JOIN series s ON s.id = sp.series_id WHERE sp.post_id = ?",
                (rs, i) -> new Series(rs.getLong("id"), rs.getString("title"), 0), postId).stream().findFirst();
    }

    /**
     * 내 글을 시리즈에 넣는다. {@code seriesId}가 있으면 그 시리즈, 없으면 {@code newTitle}로 만들거나 같은 이름 시리즈를 쓴다.
     * 다른 시리즈에 있던 글이면 옮긴다(맨 끝).
     */
    @Transactional
    public long add(Optional<CurrentUser> current, long postId, Long seriesId, String newTitle) {
        CurrentUser me = accountGuard.requireWritable(current);
        requireMyPost(me, postId);
        long target;
        if (seriesId != null) {
            Integer mine = jdbc.queryForObject("SELECT count(*) FROM series WHERE id = ? AND member_id = ?", Integer.class,
                    seriesId, me.memberId());
            if (mine == null || mine == 0) {
                throw new NotFoundException();
            }
            target = seriesId;
        } else {
            String title = newTitle == null ? "" : newTitle.strip();
            if (title.isEmpty() || title.codePointCount(0, title.length()) > 60) {
                throw new PostContentException("SERIES_TITLE_INVALID");
            }
            jdbc.update("INSERT INTO series (member_id, title) VALUES (?, ?) ON CONFLICT (member_id, title) DO NOTHING",
                    me.memberId(), title);
            target = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ? AND title = ?", Long.class, // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
                    me.memberId(), title);
        }
        Optional<Series> before = ofPost(postId);
        if (before.isPresent() && before.get().id() == target) {
            return target;
        }
        jdbc.update("DELETE FROM series_post WHERE post_id = ?", postId);
        jdbc.update("INSERT INTO series_post (post_id, series_id, position) VALUES (?, ?, "
                + "(SELECT COALESCE(max(position), 0) + 1 FROM series_post WHERE series_id = ?))", postId, target, target);
        before.ifPresent(s -> dropIfEmpty(s.id()));
        return target;
    }

    @Transactional
    public void remove(Optional<CurrentUser> current, long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        requireMyPost(me, postId);
        Optional<Series> before = ofPost(postId);
        jdbc.update("DELETE FROM series_post WHERE post_id = ?", postId);
        before.ifPresent(s -> dropIfEmpty(s.id()));
    }

    /**
     * 100 강성찬 개인 확장: 내 시리즈에서 글을 바로 앞·뒤 글과 자리를 바꾼다. 106: 관리 탭은 비공개 글까지 보여 주므로
     * 넣은 글 전체 순서({@link #ownerItems})에서 바꾼다. 끝에서 더 못 가면 그대로. 자리 번호는 1부터 다시 매긴다.
     */
    @Transactional
    public void move(Optional<CurrentUser> current, long seriesId, long postId, boolean up) {
        CurrentUser me = accountGuard.requireWritable(current);
        requireMySeries(me, seriesId);
        // 잠그고 읽는다(사이에 글이 빠지거나 옮겨지지 않게)
        List<Long> all = new java.util.ArrayList<>(jdbc.queryForList(
                "SELECT post_id FROM series_post WHERE series_id = ? ORDER BY position, post_id FOR UPDATE", Long.class, seriesId));
        int at = all.indexOf(postId);
        int to = up ? at - 1 : at + 1;
        if (at < 0) {
            throw new NotFoundException();
        }
        if (to < 0 || to >= all.size()) {
            return;
        }
        java.util.Collections.swap(all, at, to);
        for (int i = 0; i < all.size(); i++) {
            jdbc.update("UPDATE series_post SET position = ? WHERE post_id = ?", i + 1, all.get(i));
        }
    }

    /** 106 관리 탭 한 줄: 내 글(임시글 빼고, 지운 글 빼고)과 지금 든 시리즈. */
    public record MyPost(long id, String title, String visibility, Long seriesId) {
    }

    /** 106 강성찬 개인 확장: 시리즈 관리 탭용 내 발행 글(최근 순). */
    public List<MyPost> myPosts(Optional<CurrentUser> current) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        return jdbc.query("SELECT p.id, p.title, p.visibility, sp.series_id FROM post p LEFT JOIN series_post sp ON sp.post_id = p.id"
                        + " WHERE p.author_id = ? AND p.deleted_at IS NULL AND p.status = 'PUBLISHED'"
                        + " ORDER BY COALESCE(p.first_public_at, p.published_at) DESC NULLS LAST, p.id DESC",
                (rs, i) -> new MyPost(rs.getLong("id"), rs.getString("title"), rs.getString("visibility"),
                        rs.getObject("series_id") == null ? null : rs.getLong("series_id")), me.memberId());
    }

    /** 106: 주인이 보는 시리즈 글 전체(비공개 포함, 넣은 순서). */
    public List<MyPost> ownerItems(long seriesId) {
        return jdbc.query("SELECT p.id, p.title, p.visibility, sp.series_id FROM series_post sp JOIN post p ON p.id = sp.post_id"
                        + " WHERE sp.series_id = ? AND p.deleted_at IS NULL ORDER BY sp.position, p.id",
                (rs, i) -> new MyPost(rs.getLong("id"), rs.getString("title"), rs.getString("visibility"), rs.getLong("series_id")),
                seriesId);
    }

    /**
     * 106: 여러 글을 한 번에 넣는다. {@code seriesId}가 없으면 {@code newTitle}로 새 시리즈(첫 글과 함께 만든다).
     * 글이 하나도 없으면 시리즈를 만들 수 없다(빈 시리즈는 지우는 규칙).
     */
    @Transactional
    public long addAll(Optional<CurrentUser> current, List<Long> postIds, Long seriesId, String newTitle) {
        if (postIds == null || postIds.isEmpty()) {
            throw new PostContentException("SERIES_POSTS_REQUIRED");
        }
        long target = add(current, postIds.get(0), seriesId, newTitle);
        postIds.stream().skip(1).forEach(id -> add(current, id, target, null));
        return target;
    }

    /** 106: 관리 탭에서 내 시리즈 화면 주소를 만들 때. */
    public String handle(long memberId) {
        return jdbc.queryForObject("SELECT handle FROM member WHERE id = ?", String.class, memberId);
    }

    private void requireMySeries(CurrentUser me, long seriesId) {
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM series WHERE id = ? AND member_id = ?", Integer.class,
                seriesId, me.memberId());
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
    }

    /** 탈퇴 정리. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM series WHERE member_id = ?", memberId);
    }

    private void dropIfEmpty(long seriesId) {
        jdbc.update("DELETE FROM series s WHERE s.id = ? AND NOT EXISTS (SELECT 1 FROM series_post sp WHERE sp.series_id = s.id)",
                seriesId);
    }

    private void requireMyPost(CurrentUser me, long postId) {
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                Integer.class, postId, me.memberId());
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
    }
}
