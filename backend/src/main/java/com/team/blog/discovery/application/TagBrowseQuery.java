package com.team.blog.discovery.application;

import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.application.TagListingQuery.TagCount;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 056 태그 화면(강성찬 개인 확장): 많이 본 순 정렬과 전체 태그 수. 공용 목록 조건(012) 글만 센다. */
@Component
public class TagBrowseQuery {

    private final JdbcTemplate jdbc;
    private final PostAccessPolicy accessPolicy;

    public TagBrowseQuery(JdbcTemplate jdbc, PostAccessPolicy accessPolicy) {
        this.jdbc = jdbc;
        this.accessPolicy = accessPolicy;
    }

    /** 태그 글들의 조회수 합이 많은 순(같으면 글 수, 이름). 보여 주는 수는 글 수. */
    public List<TagCount> byViews(int limit) {
        return jdbc.query("SELECT t.name, count(*) AS n, sum(p.view_count) AS v FROM tag t JOIN post_tag pt ON pt.tag_id = t.id"
                        + " JOIN post p ON p.id = pt.post_id JOIN member m ON m.id = p.author_id WHERE "
                        + accessPolicy.publicListingCondition("p", "m") + " GROUP BY t.name ORDER BY v DESC, n DESC, t.name LIMIT ?",
                (rs, i) -> new TagCount(rs.getString("name"), rs.getLong("n")), limit);
    }

    /** 태그 화면 한 묶음(강성찬 개인 확장, devlog 태그 화면처럼): 태그 번호·이름·공개 글 수·가장 최근 글 시각. */
    public record Section(long id, String name, long postCount, java.time.Instant latest) {
    }

    /**
     * 정렬: popular = 글 수 → 최근 글, recent = 최근 글. {@code q}가 있으면 이름에 그 글자가 든 태그만.
     * 공용 목록 조건(공개 글·공개 계정) 글만 센다.
     */
    public List<Section> sections(String sort, String q, int offset, int limit) {
        String order = switch (sort) {
            case "recent" -> "latest DESC, t.name";
            default -> "n DESC, latest DESC, t.name";
        };
        boolean filter = q != null && !q.isBlank();
        String sql = "SELECT t.id, t.name, count(*) AS n, max(p.first_public_at) AS latest FROM tag t JOIN post_tag pt ON pt.tag_id = t.id"
                + " JOIN post p ON p.id = pt.post_id JOIN member m ON m.id = p.author_id WHERE "
                + accessPolicy.publicListingCondition("p", "m") + (filter ? " AND position(lower(?) in t.name) > 0" : "")
                + " GROUP BY t.id, t.name ORDER BY " + order + " OFFSET ? LIMIT ?";
        org.springframework.jdbc.core.RowMapper<Section> map = (rs, i) -> new Section(rs.getLong("id"), rs.getString("name"),
                rs.getLong("n"), rs.getTimestamp("latest").toInstant());
        return filter ? jdbc.query(sql, map, q.strip(), offset, limit) : jdbc.query(sql, map, offset, limit);
    }

    /** {@code q}가 든 이름의 태그 수(공개 글에 쓰인 것만). */
    public long count(String q) {
        if (q == null || q.isBlank()) {
            return total();
        }
        Long n = jdbc.queryForObject("SELECT count(DISTINCT pt.tag_id) FROM post_tag pt JOIN tag t ON t.id = pt.tag_id JOIN post p ON p.id = pt.post_id"
                + " JOIN member m ON m.id = p.author_id WHERE " + accessPolicy.publicListingCondition("p", "m")
                + " AND position(lower(?) in t.name) > 0", Long.class, q.strip());
        return n == null ? 0 : n;
    }

    /** 공개 글에 한 번이라도 쓰인 태그 수. */
    public long total() {
        Long n = jdbc.queryForObject("SELECT count(DISTINCT pt.tag_id) FROM post_tag pt JOIN post p ON p.id = pt.post_id"
                + " JOIN member m ON m.id = p.author_id WHERE " + accessPolicy.publicListingCondition("p", "m"), Long.class);
        return n == null ? 0 : n;
    }
}
