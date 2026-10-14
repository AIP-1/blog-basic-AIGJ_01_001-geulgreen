package com.team.blog.discovery.application;

import com.team.blog.post.application.PostAccessPolicy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 038 이전 글·다음 글(강성찬 개인 확장). 공용 목록 조건(012) 글만. 글 아래 관련 글은 {@link RelatedPostsQuery}. */
@Component
public class PostNavigationQuery {

    public record Link(long id, String title, String handle) {

        public String url() {
            return com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + id;
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
}
