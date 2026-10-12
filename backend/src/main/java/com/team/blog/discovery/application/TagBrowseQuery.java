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

    /** 공개 글에 한 번이라도 쓰인 태그 수. */
    public long total() {
        Long n = jdbc.queryForObject("SELECT count(DISTINCT pt.tag_id) FROM post_tag pt JOIN post p ON p.id = pt.post_id"
                + " JOIN member m ON m.id = p.author_id WHERE " + accessPolicy.publicListingCondition("p", "m"), Long.class);
        return n == null ? 0 : n;
    }
}
