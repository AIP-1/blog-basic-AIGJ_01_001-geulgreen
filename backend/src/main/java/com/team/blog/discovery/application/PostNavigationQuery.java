package com.team.blog.discovery.application;

import com.team.blog.post.application.PostAccessPolicy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 038 이전 글·다음 글 + 관련 글(강성찬 개인 확장). 모두 공용 목록 조건(012) 글만. */
@Component
public class PostNavigationQuery {

    public record Link(long id, String title, String handle) {

        public String url() {
            return "/@" + handle + "/posts/" + id;
        }
    }

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;

    public PostNavigationQuery(JdbcTemplate jdbc, PostAccessPolicy accessPolicy) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
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

    /** 태그가 많이 겹치는 순, 같으면 최신순. */
    public List<Link> related(long postId, int limit) {
        return jdbc.query("""
                SELECT p.id, p.title, m.handle, count(*) AS shared, max(p.first_public_at) AS at
                FROM post_tag pt JOIN post p ON p.id = pt.post_id JOIN member m ON m.id = p.author_id
                WHERE pt.tag_id IN (SELECT tag_id FROM post_tag WHERE post_id = ?) AND p.id <> ? AND """ + " "
                        + accessPolicy.publicListingCondition("p", "m") + """
                 GROUP BY p.id, p.title, m.handle ORDER BY shared DESC, at DESC, p.id DESC LIMIT ?""",
                (rs, i) -> new Link(rs.getLong("id"), rs.getString("title"), rs.getString("handle")), postId, postId, limit);
    }
}
