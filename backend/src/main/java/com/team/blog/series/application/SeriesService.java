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
 * 041 시리즈(강성찬 개인 확장): 내 글을 이름 붙인 묶음에 넣은 순서대로. 글은 시리즈 0~1개.
 * 107: 글이 다 빠져도 시리즈(이름·설명·사진)는 남고, 지우기는 관리 탭 [시리즈 삭제]로만.
 * 목록·편 번호는 공용 목록 조건(012) 글만 센다.
 */
@Service
public class SeriesService implements com.team.blog.media.application.ImageReferenceLookup {

    /** 107: {@code coverUrl}은 화면에 쓸 사진(고른 것, 없으면 1편부터 첫 사진), {@code pickedCover}는 주인이 고른 것(관리 탭). */
    public record Series(long id, String title, long postCount, String description, String coverUrl, String pickedCover,
                         String latestLabel) {

        /** 108: 카드 아래 줄 "글 3편 · 2026.10.14 업데이트". */
        public String meta() {
            return "글 " + postCount + "편" + (latestLabel == null || latestLabel.isEmpty() ? "" : " · " + latestLabel + " 업데이트");
        }
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
    private final java.time.Clock clock;
    private final com.team.blog.media.application.PostImageService postImages;

    public SeriesService(JdbcTemplate jdbc, AccountGuard accountGuard, PostAccessPolicy accessPolicy,
                         SeriesProperties properties, java.time.Clock clock,
                         com.team.blog.media.application.PostImageService postImages) {
        this.clock = clock;
        this.postImages = postImages;
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.accessPolicy = accessPolicy;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    /** 107 강성찬 개인 확장: 대표 사진(고른 것, 없으면 보이는 글 중 넣은 순서로 첫 사진). {@code s}는 series 별칭. */
    private String coverSql(String condition) {
        return "COALESCE(s.cover_url, (SELECT COALESCE(cp.thumb_url, cpost.thumbnail_url) FROM series_post csp"
                + " JOIN post cpost ON cpost.id = csp.post_id JOIN member cm ON cm.id = cpost.author_id"
                + " LEFT JOIN post_cover cp ON cp.post_id = cpost.id WHERE csp.series_id = s.id AND " + condition
                + " AND COALESCE(cp.thumb_url, cpost.thumbnail_url) IS NOT NULL ORDER BY csp.position, cpost.id LIMIT 1)) AS cover";
    }

    /** 블로그의 시리즈(공개 글 수 포함, 최근에 만든 순). {@code includeEmpty}: 주인에게는 공개 글이 없는 것도. */
    public List<Series> list(long memberId, boolean includeEmpty) {
        List<Series> all = jdbc.query("SELECT s.id, s.title, s.description, s.cover_url, " + coverSql(accessPolicy.blogListingCondition("cpost", "cm"))
                        + ", (SELECT count(*) FROM series_post sp JOIN post p ON p.id = sp.post_id"
                        + " JOIN member m ON m.id = p.author_id WHERE sp.series_id = s.id AND "
                        + accessPolicy.blogListingCondition("p", "m") + ") AS cnt, (SELECT max(COALESCE(p.first_public_at, p.published_at))"
                        + " FROM series_post sp JOIN post p ON p.id = sp.post_id JOIN member m ON m.id = p.author_id WHERE sp.series_id = s.id AND "
                        + accessPolicy.blogListingCondition("p", "m") + ") AS latest FROM series s WHERE s.member_id = ?"
                        + " ORDER BY latest DESC NULLS LAST, s.id DESC", // 108: 최근에 글이 올라온 시리즈부터
                (rs, i) -> new Series(rs.getLong("id"), rs.getString("title"), rs.getLong("cnt"), rs.getString("description"),
                        rs.getString("cover"), rs.getString("cover_url"), label(rs.getTimestamp("latest"))), memberId);
        return includeEmpty ? all : all.stream().filter(s -> s.postCount() > 0).toList();
    }

    /** 탐색 시리즈 탭에 보일 한 줄: 누구의 시리즈인지와 공개 글 수. */
    public record Browse(long id, String title, String handle, long postCount, String description, String coverUrl,
                         String latestLabel) {

        public String meta() {
            return handle + " · 글 " + postCount + "편" + (latestLabel == null || latestLabel.isEmpty() ? "" : " · " + latestLabel + " 업데이트");
        }

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/series/" + id;
        }
    }

    public static final int BROWSE_PAGE = 60;

    /**
     * 104 강성찬 개인 확장: 모든 블로그의 시리즈(공용 목록 조건 글이 1편 이상). popular = 좋아요 합 → 글 수 → 최근 글,
     * recent = 최근에 글이 공개된 순. 108: {@code offset}부터 {@code limit}개(더 보기).
     */
    public List<Browse> browse(String sort, int offset, int limit) {
        String order = "recent".equals(sort) ? "latest DESC, s.id DESC" : "likes DESC, cnt DESC, latest DESC, s.id DESC";
        return jdbc.query("SELECT s.id, s.title, s.description, " + coverSql(accessPolicy.publicListingCondition("cpost", "cm"))
                        + ", owner.handle, count(*) AS cnt, sum(p.like_count) AS likes,"
                        + " max(COALESCE(p.first_public_at, p.published_at)) AS latest FROM series s"
                        + " JOIN member owner ON owner.id = s.member_id JOIN series_post sp ON sp.series_id = s.id"
                        + " JOIN post p ON p.id = sp.post_id JOIN member m ON m.id = p.author_id WHERE "
                        + accessPolicy.publicListingCondition("p", "m")
                        + " GROUP BY s.id, s.title, s.description, s.cover_url, owner.handle ORDER BY " + order + " OFFSET ? LIMIT ?",
                (rs, i) -> new Browse(rs.getLong("id"), rs.getString("title"), rs.getString("handle"), rs.getLong("cnt"),
                        rs.getString("description"), rs.getString("cover"), label(rs.getTimestamp("latest"))), offset, limit);
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
                (rs, i) -> new Series(rs.getLong("id"), rs.getString("title"), 0, null, null, null, null), postId).stream().findFirst();
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
            target = createOrFind(me, newTitle);
        }
        Optional<Series> before = ofPost(postId);
        if (before.isPresent() && before.get().id() == target) {
            return target;
        }
        jdbc.update("DELETE FROM series_post WHERE post_id = ?", postId);
        jdbc.update("INSERT INTO series_post (post_id, series_id, position) VALUES (?, ?, "
                + "(SELECT COALESCE(max(position), 0) + 1 FROM series_post WHERE series_id = ?))", postId, target, target);
        return target;
    }

    @Transactional
    public void remove(Optional<CurrentUser> current, long postId) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        requireMyPost(me, postId);
        jdbc.update("DELETE FROM series_post WHERE post_id = ?", postId);
    }

    /** 이름으로 내 시리즈를 만든다(같은 이름이 있으면 그것). */
    private long createOrFind(CurrentUser me, String newTitle) {
        String title = newTitle == null ? "" : newTitle.strip();
        if (title.isEmpty() || title.codePointCount(0, title.length()) > 60) {
            throw new PostContentException("SERIES_TITLE_INVALID");
        }
        jdbc.update("INSERT INTO series (member_id, title) VALUES (?, ?) ON CONFLICT (member_id, title) DO NOTHING",
                me.memberId(), title);
        return jdbc.queryForObject("SELECT id FROM series WHERE member_id = ? AND title = ?", Long.class, // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
                me.memberId(), title);
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

    private static final String THUMB = "COALESCE(pcv.thumb_url, p.thumbnail_url) AS thumb";

    /** 106 관리 탭 한 줄: 내 글(임시글 빼고, 지운 글 빼고)과 지금 든 시리즈. */
    public record MyPost(long id, String title, String visibility, Long seriesId, String thumb) {
    }

    /** 106 강성찬 개인 확장: 시리즈 관리 탭용 내 발행 글(최근 순). */
    public List<MyPost> myPosts(Optional<CurrentUser> current) {
        CurrentUser me = accountGuard.requireLoggedIn(current);
        return jdbc.query("SELECT p.id, p.title, p.visibility, sp.series_id, " + THUMB + " FROM post p LEFT JOIN series_post sp ON sp.post_id = p.id"
                        + " LEFT JOIN post_cover pcv ON pcv.post_id = p.id"
                        + " WHERE p.author_id = ? AND p.deleted_at IS NULL AND p.status = 'PUBLISHED'"
                        + " ORDER BY COALESCE(p.first_public_at, p.published_at) DESC NULLS LAST, p.id DESC",
                (rs, i) -> new MyPost(rs.getLong("id"), rs.getString("title"), rs.getString("visibility"),
                        rs.getObject("series_id") == null ? null : rs.getLong("series_id"), rs.getString("thumb")), me.memberId());
    }

    /** 106: 주인이 보는 시리즈 글 전체(비공개 포함, 넣은 순서). */
    public List<MyPost> ownerItems(long seriesId) {
        return jdbc.query("SELECT p.id, p.title, p.visibility, sp.series_id, " + THUMB + " FROM series_post sp JOIN post p ON p.id = sp.post_id"
                        + " LEFT JOIN post_cover pcv ON pcv.post_id = p.id WHERE sp.series_id = ? AND p.deleted_at IS NULL ORDER BY sp.position, p.id",
                (rs, i) -> new MyPost(rs.getLong("id"), rs.getString("title"), rs.getString("visibility"), rs.getLong("series_id"),
                        rs.getString("thumb")),
                seriesId);
    }

    /**
     * 106: 여러 글을 한 번에 넣는다. {@code seriesId}가 없으면 {@code newTitle}로 새 시리즈.
     * 107: 새 시리즈는 글 없이도 만든다. 있는 시리즈에 넣을 때만 글이 하나 이상 필요하다.
     */
    @Transactional
    public long addAll(Optional<CurrentUser> current, List<Long> postIds, Long seriesId, String newTitle) {
        if (postIds == null || postIds.isEmpty()) {
            if (seriesId != null) {
                throw new PostContentException("SERIES_POSTS_REQUIRED");
            }
            return createOrFind(accountGuard.requireWritable(current), newTitle);
        }
        // 화면 목록은 최신 글이 위라 고른 순서 그대로 넣으면 거꾸로 들어간다: 공개(발행)한 순서대로 뒤에 붙인다
        List<Long> ordered = new java.util.ArrayList<>(jdbc.queryForList("SELECT id FROM post WHERE id = ANY (?)"
                + " ORDER BY COALESCE(first_public_at, published_at) NULLS LAST, id", Long.class, (Object) postIds.toArray(new Long[0])));
        postIds.stream().filter(id -> !ordered.contains(id)).forEach(ordered::add); // 없는 글은 add가 404로
        long target = add(current, ordered.get(0), seriesId, newTitle);
        ordered.stream().skip(1).forEach(id -> add(current, id, target, null));
        return target;
    }

    /**
     * 106 이름 바꾸기, 107 설명 한 줄(100자까지, 비우면 없음)·대표 사진. 사진은 이 시리즈에 든 글의 사진 중에서, 또는 111: 내가 올린 사진(썸네일 만들기 포함)
     * 고를 수 있다(아무 주소나 넣지 못하게). 비우면 1편부터 첫 사진을 쓴다. 이름은 1~60자, 내 다른 시리즈와 겹치면 안 된다.
     */
    @Transactional
    public void update(Optional<CurrentUser> current, long seriesId, String newTitle, String description, String coverUrl) {
        CurrentUser me = accountGuard.requireWritable(current);
        requireMySeries(me, seriesId);
        String title = newTitle == null ? "" : newTitle.strip();
        if (title.isEmpty() || title.codePointCount(0, title.length()) > 60) {
            throw new PostContentException("SERIES_TITLE_INVALID");
        }
        Integer taken = jdbc.queryForObject("SELECT count(*) FROM series WHERE member_id = ? AND title = ? AND id <> ?", Integer.class,
                me.memberId(), title, seriesId);
        if (taken != null && taken > 0) {
            throw new PostContentException("SERIES_TITLE_TAKEN");
        }
        String desc = description == null ? "" : description.strip();
        if (desc.codePointCount(0, desc.length()) > 100) {
            throw new PostContentException("SERIES_DESCRIPTION_TOO_LONG");
        }
        // 111: 이 시리즈 글의 사진이거나, 내가 올린(올리기를 마친) 사진만. 그 밖의 주소는 자동(1편부터 첫 사진)
        String cover = null;
        Long imageId = null;
        var saved = jdbc.queryForMap("SELECT cover_url, cover_image_id FROM series WHERE id = ?", seriesId);
        if (coverUrl != null && ownerItems(seriesId).stream().anyMatch(p -> coverUrl.equals(p.thumb()))) {
            cover = coverUrl;
        } else if (coverUrl != null && saved.get("cover_image_id") != null && coverUrl.equals(saved.get("cover_url"))) {
            cover = coverUrl; // 이미 올려 둔 대표 사진을 그대로 두고 이름·설명만 고칠 때
            imageId = ((Number) saved.get("cover_image_id")).longValue();
        } else if (coverUrl != null && !coverUrl.isBlank()) {
            imageId = postImages.ownedCompletedImageId(me.memberId(), coverUrl.strip()).orElse(null);
            cover = imageId == null ? null : postImages.cardThumbnailOf(coverUrl.strip());
        }
        jdbc.update("UPDATE series SET title = ?, description = ?, cover_url = ?, cover_image_id = ? WHERE id = ?", title,
                desc.isEmpty() ? null : desc, cover, imageId, seriesId);
    }

    /** 106: 시리즈 묶음만 지운다(글은 그대로, series_post는 CASCADE). */
    @Transactional
    public void delete(Optional<CurrentUser> current, long seriesId) {
        CurrentUser me = accountGuard.requireWritable(current);
        requireMySeries(me, seriesId);
        jdbc.update("DELETE FROM series WHERE id = ?", seriesId);
    }

    private String label(java.sql.Timestamp at) {
        return at == null ? null : com.team.blog.post.application.CardDates.label(at.toInstant(), clock.instant());
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

    /** 111: 시리즈 대표 사진으로 올린 사진은 정리 작업이 지우지 않는다. */
    @Override
    public java.util.Set<Long> referencedIds(java.util.Collection<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return java.util.Set.of();
        }
        return new java.util.HashSet<>(jdbc.queryForList("SELECT cover_image_id FROM series WHERE cover_image_id = ANY (?)", Long.class,
                (Object) imageIds.toArray(new Long[0])));
    }

    static final String MISSION_TITLE = "오늘의 미션";

    /**
     * 113 강성찬 개인 확장: 오늘의 미션 글이 처음 발행되면 글쓴이의 미션 시리즈 맨 끝에 넣는다(없으면 "오늘의 미션"으로 만든다).
     * 미션 글이 아니거나 이미 다른 시리즈에 든 글은 그대로. 시리즈는 이름이 아니라 표시(mission)로 찾아서 이름을 바꿔도 이어진다.
     * 같은 이름의 내 시리즈가 이미 있으면 그 시리즈를 미션 시리즈로 쓴다.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void addMissionPost(long memberId, long postId) {
        Integer mission = jdbc.queryForObject("SELECT count(*) FROM mission_post WHERE post_id = ? AND member_id = ?", Integer.class,
                postId, memberId);
        if (mission == null || mission == 0 || ofPost(postId).isPresent()) {
            return;
        }
        Long target = jdbc.queryForList("SELECT id FROM series WHERE member_id = ? AND mission", Long.class, memberId)
                .stream().findFirst().orElseGet(() -> jdbc.queryForObject("""
                        INSERT INTO series (member_id, title, mission) VALUES (?, ?, TRUE)
                        ON CONFLICT (member_id, title) DO UPDATE SET mission = TRUE RETURNING id""", Long.class, memberId, MISSION_TITLE));
        jdbc.update("INSERT INTO series_post (post_id, series_id, position) VALUES (?, ?, "
                + "(SELECT COALESCE(max(position), 0) + 1 FROM series_post WHERE series_id = ?)) ON CONFLICT DO NOTHING", postId, target, target);
    }

    /** 113: 미션 시리즈면 글 화면에 시리즈 상자를 보이지 않는다(글 화면에는 미션 티를 내지 않는다). */
    public boolean isMission(long seriesId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT mission FROM series WHERE id = ?", Boolean.class, seriesId));
    }

    /** 탈퇴 정리. */
    public void purgeWithdrawn(long memberId) {
        jdbc.update("DELETE FROM series WHERE member_id = ?", memberId);
    }

    private void requireMyPost(CurrentUser me, long postId) {
        Integer mine = jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                Integer.class, postId, me.memberId());
        if (mine == null || mine == 0) {
            throw new NotFoundException();
        }
    }
}
