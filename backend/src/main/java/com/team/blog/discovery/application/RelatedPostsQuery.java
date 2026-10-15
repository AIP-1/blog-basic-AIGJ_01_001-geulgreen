package com.team.blog.discovery.application;

import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.application.PostCard;
import com.team.blog.post.application.PostListQuery;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 095 글 아래 "관련 글"(강성찬 개인 확장, 080 무한 스크롤을 대신함). 고르는 규칙은 여기 한 곳에만 둔다(094 비슷한 글 점수로 바꿀 자리).
 * 공용 목록 조건(012) 글 가운데 이 글을 빼고: 이 글과 겹치는 태그가 많은 글 → 같은 글쓴이의 최근 글 → 최근 30일 좋아요가 많은 글 → 최신 순.
 * 공용 목록 조건이라 비공개·친구 공개·숨김 글은 보는 사람과 상관없이 나오지 않는다. 외부 AI는 쓰지 않는다.
 * 이 글이 공용 목록 글이 아니면 그 태그·글쓴이는 쓰지 않는다(아무 글 번호로 불러도 새지 않게, 080과 같은 규칙).
 */
@Component
public class RelatedPostsQuery {

    public static final int SIZE = 4;
    static final Duration POPULAR_WINDOW = Duration.ofDays(30);

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;
    private final PostListQuery listQuery;
    private final Clock clock;

    public RelatedPostsQuery(JdbcTemplate jdbc, PostAccessPolicy accessPolicy, PostListQuery listQuery, Clock clock) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
        this.listQuery = listQuery;
        this.clock = clock;
    }

    public List<PostCard> related(long postId) {
        long sourceId = listQuery.cardsByIds(List.of(postId)).containsKey(postId) ? postId : -1;
        List<Long> ids = jdbc.queryForList("""
                SELECT p.id FROM post p JOIN member m ON m.id = p.author_id
                WHERE""" + " " + accessPolicy.publicListingCondition("p", "m") + """
                 AND p.hidden_at IS NULL AND p.id <> ?
                ORDER BY (SELECT count(*) FROM post_tag pt WHERE pt.post_id = p.id
                          AND pt.tag_id IN (SELECT st.tag_id FROM post_tag st WHERE st.post_id = ?)) DESC,
                         (p.author_id = (SELECT s.author_id FROM post s WHERE s.id = ?)) IS TRUE DESC,
                         CASE WHEN (p.author_id = (SELECT s.author_id FROM post s WHERE s.id = ?)) IS NOT TRUE AND p.first_public_at >= ? THEN p.like_count ELSE 0 END DESC,
                         p.first_public_at DESC, p.id DESC
                LIMIT ?""", Long.class, postId, sourceId, sourceId, sourceId,
                Timestamp.from(clock.instant().minus(POPULAR_WINDOW)), SIZE);
        Map<Long, PostCard> cards = listQuery.cardsByIds(ids);
        return ids.stream().map(cards::get).filter(Objects::nonNull).toList(); // NOSONAR 오탐: Spring 조회 결과는 null이 아니다(빈 목록이나 예외)
    }
}
