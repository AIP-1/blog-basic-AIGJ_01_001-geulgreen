package com.team.blog.discovery.application;

import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.FeedCursor;
import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.application.PostCard;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.shared.error.PostContentException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 038 이전 글·다음 글 + 080 관심 있을 만한 글(강성찬 개인 확장). 모두 공용 목록 조건(012) 글만. */
@Component
public class PostNavigationQuery {

    public record Link(long id, String title, String handle) {

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + id;
        }
    }

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;
    private final PostListQuery listQuery;

    public PostNavigationQuery(JdbcTemplate jdbc, PostAccessPolicy accessPolicy, PostListQuery listQuery) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
        this.listQuery = listQuery;
    }

    /** 같은 글쓴이의 바로 앞(older=true) 또는 뒤 글. */
    public Optional<Link> neighbor(long authorId, long postId, Instant firstPublicAt, boolean older) {
        String cmp = older ? "<" : ">";
        String dir = older ? "DESC" : "ASC";
        // 별칭을 np·nm으로: 글 상세의 본 조회(010, 글 한 번만 읽기 검사)와 구분된다
        return jdbc.query("SELECT np.id, np.title, nm.handle FROM post np JOIN member nm ON nm.id = np.author_id WHERE "
                        + accessPolicy.blogListingCondition("np", "nm") + " AND np.author_id = ? AND (np.first_public_at, np.id) "
                        + cmp + " (?, ?) ORDER BY np.first_public_at " + dir + ", np.id " + dir + " LIMIT 1",
                (rs, i) -> new Link(rs.getLong("id"), rs.getString("title"), rs.getString("handle")),
                authorId, Timestamp.from(firstPublicAt), postId).stream().findFirst();
    }

    /**
     * 080 글 아래 "관심 있을 만한 글"(관련 글을 대신함): 공용 목록 조건(012) 글을 이 글과 태그가 겹치거나 같은 작가 → 내가 좋아요하거나 쓴 글의 태그를 단
     * 아직 팔로우하지 않은 사람 글(077 추천 규칙) → 최신 순으로. 이 글·내 글은 뺀다. 비회원은 {@code viewerId} -1.
     * 이 글이 공용 목록 글이 아니면 그 태그·작가는 쓰지 않는다(아무 글 번호로 불러도 새지 않게). 커서는 건너뛸 개수.
     * ponytail: 건너뛰기 커서라 보는 사이 새 글이 올라오면 한두 개가 겹칠 수 있다(화면에서 이미 있는 글 번호는 건너뛴다).
     */
    public CardPage more(long postId, long viewerId, String cursor) {
        int offset = offset(cursor);
        long sourceId = listQuery.cardsByIds(List.of(postId)).containsKey(postId) ? postId : -1;
        List<Long> ids = jdbc.queryForList("""
                SELECT p.id FROM post p JOIN member m ON m.id = p.author_id
                WHERE""" + " " + accessPolicy.publicListingCondition("p", "m") + """
                 AND p.hidden_at IS NULL AND p.id <> ? AND p.author_id <> ?
                ORDER BY (p.author_id = (SELECT s.author_id FROM post s WHERE s.id = ?)
                          OR EXISTS (SELECT 1 FROM post_tag pt WHERE pt.post_id = p.id
                                     AND pt.tag_id IN (SELECT st.tag_id FROM post_tag st WHERE st.post_id = ?))) DESC,
                         (p.author_id NOT IN (SELECT f.followee_id FROM follow f WHERE f.follower_id = ?)
                          AND EXISTS (SELECT 1 FROM post_tag pt WHERE pt.post_id = p.id AND pt.tag_id IN (
                                SELECT mine.tag_id FROM post_tag mine WHERE mine.post_id IN (
                                    SELECT pl.post_id FROM post_like pl WHERE pl.member_id = ?
                                    UNION SELECT own.id FROM post own WHERE own.author_id = ?)))) DESC,
                         p.first_public_at DESC, p.id DESC
                LIMIT ? OFFSET ?""", Long.class, postId, viewerId, sourceId, sourceId, viewerId, viewerId, viewerId,
                PostListQuery.PAGE_SIZE + 1, offset);
        boolean hasNext = ids.size() > PostListQuery.PAGE_SIZE;
        List<Long> page = hasNext ? ids.subList(0, PostListQuery.PAGE_SIZE) : ids;
        Map<Long, PostCard> cards = listQuery.cardsByIds(page);
        return new CardPage(page.stream().map(cards::get).filter(Objects::nonNull).toList(),
                hasNext ? String.valueOf(offset + PostListQuery.PAGE_SIZE) : null);
    }

    private static int offset(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            int n = Integer.parseInt(cursor);
            if (n >= 0 && n <= 10_000) {
                return n;
            }
        } catch (NumberFormatException ignored) {
            // 아래에서 400
        }
        throw new PostContentException(FeedCursor.INVALID);
    }
}
