package com.team.blog.discovery.application;

import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.application.PostCard;
import com.team.blog.post.application.PostListQuery;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 홈 오른쪽 칸 "추천 글"(077, 강성찬 개인 확장). 5개, 내 글 제외, 공용 목록 조건(공개 글·공개 계정)만.
 * 로그인했고 기록이 있으면 내가 좋아요하거나 쓴 글의 태그를 단 최근 30일 글 중 아직 팔로우하지 않은 사람 글을 먼저(새 사람 발견),
 * 모자라면 최근 7일 글을 조회수 순으로 채운다(그래도 모자라면 7일 밖 글도 조회수 순). 비회원은 {@code memberId} -1.
 */
@Service
public class RecommendedPostQuery {

    static final int LIMIT = 5;
    static final Duration TAG_WINDOW = Duration.ofDays(30);
    static final Duration VIEW_WINDOW = Duration.ofDays(7);

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;
    private final PostListQuery listQuery;
    private final Clock clock;

    public RecommendedPostQuery(JdbcTemplate jdbc, PostAccessPolicy accessPolicy, PostListQuery listQuery, Clock clock) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
        this.listQuery = listQuery;
        this.clock = clock;
    }

    public List<PostCard> forMember(long memberId) {
        String visible = accessPolicy.publicListingCondition("p", "m");
        Set<Long> ids = new LinkedHashSet<>(jdbc.queryForList(tagSql(visible) + " " + LIMIT,
                Long.class, memberId, Timestamp.from(clock.instant().minus(TAG_WINDOW)), memberId, memberId, memberId));
        if (ids.size() < LIMIT) {
            ids.addAll(jdbc.queryForList("""
                    SELECT p.id FROM post p JOIN member m ON m.id = p.author_id
                    WHERE""" + " " + visible + """
                     AND p.author_id <> ?
                    ORDER BY p.first_public_at >= ? DESC, p.view_count DESC, p.first_public_at DESC, p.id DESC LIMIT""" + " " + 2 * LIMIT,
                    Long.class, memberId, Timestamp.from(clock.instant().minus(VIEW_WINDOW))));
        }
        return cards(ids, LIMIT);
    }

    // ponytail: 탐색 추천 글은 앞 100개까지만(그 뒤는 "모든 글을 다 봤어요"). 글이 아주 많아지면 커서 방식으로.
    static final int POOL = 100;

    /** 086 탐색 추천 글 순서: 같은 태그(새 사람) 먼저, 그다음 조회수 순. 내 글·팔로우한 사람 글(피드에 나옴)은 뺀다. */
    public List<Long> pool(long memberId) {
        String visible = accessPolicy.publicListingCondition("p", "m");
        Set<Long> ids = new LinkedHashSet<>(jdbc.queryForList(tagSql(visible) + " " + POOL,
                Long.class, memberId, Timestamp.from(clock.instant().minus(TAG_WINDOW)), memberId, memberId, memberId));
        ids.addAll(jdbc.queryForList("""
                SELECT p.id FROM post p JOIN member m ON m.id = p.author_id
                WHERE""" + " " + visible + """
                 AND p.author_id <> ? AND p.author_id NOT IN (SELECT f.followee_id FROM follow f WHERE f.follower_id = ?)
                ORDER BY p.first_public_at >= ? DESC, p.view_count DESC, p.first_public_at DESC, p.id DESC LIMIT""" + " " + POOL,
                Long.class, memberId, memberId, Timestamp.from(clock.instant().minus(VIEW_WINDOW))));
        return ids.stream().limit(POOL).toList();
    }

    /**
     * 강성찬 개인 확장(10-16): 첫 댓글을 기다리는 글. 최근 7일 안에 처음 공개됐고 댓글이 0개인 글(공용 목록 조건 = 전체 공개 글·공개 계정),
     * 보는 사람 자신의 글은 빼고 최신순으로 {@code limit}개.
     */
    public List<Long> waitingFirstComment(long memberId, int limit) {
        return jdbc.queryForList("""
                SELECT p.id FROM post p JOIN member m ON m.id = p.author_id
                WHERE""" + " " + accessPolicy.publicListingCondition("p", "m") + """
                 AND p.visibility = 'PUBLIC' AND p.comment_count = 0 AND p.author_id <> ? AND p.first_public_at >= ?
                ORDER BY p.first_public_at DESC, p.id DESC LIMIT ?""",
                Long.class, memberId, Timestamp.from(clock.instant().minus(VIEW_WINDOW)), limit);
    }

    private static String tagSql(String visible) {
        return """
                SELECT p.id FROM post p JOIN member m ON m.id = p.author_id
                WHERE""" + " " + visible + """
                 AND p.author_id <> ? AND p.first_public_at >= ?
                  AND p.author_id NOT IN (SELECT f.followee_id FROM follow f WHERE f.follower_id = ?)
                  AND EXISTS (SELECT 1 FROM post_tag pt WHERE pt.post_id = p.id AND pt.tag_id IN (
                        SELECT mine.tag_id FROM post_tag mine WHERE mine.post_id IN (
                            SELECT pl.post_id FROM post_like pl WHERE pl.member_id = ?
                            UNION SELECT own.id FROM post own WHERE own.author_id = ?)))
                ORDER BY p.first_public_at DESC, p.id DESC LIMIT""";
    }

    private List<PostCard> cards(java.util.Collection<Long> ids, int limit) {
        Map<Long, PostCard> cards = listQuery.cardsByIds(ids);
        List<PostCard> out = new ArrayList<>();
        ids.stream().map(cards::get).filter(Objects::nonNull).limit(limit).forEach(out::add);
        return out;
    }
}
