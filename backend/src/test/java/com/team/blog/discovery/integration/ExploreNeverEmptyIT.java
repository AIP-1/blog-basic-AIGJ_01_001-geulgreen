package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 10-19(chan "추천이 아예 없는 경우는 없게"): 다른 글쓴이를 모두 팔로우해도 탐색 [추천] 목록이 비지 않는다. 내 글은 계속 빠진다. */
class ExploreNeverEmptyIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-19T03:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private String list(long viewer) throws Exception {
        String html = mockMvc.perform(get("/explore").with(TestAuth.member(viewer))).andReturn().getResponse().getContentAsString();
        return html.substring(html.indexOf("id=\"card-grid\""));
    }

    @Test
    void followingEveryoneStillShowsTheirPosts() throws Exception {
        clock.set(NOW);
        long me = members.localMember("nemine", "추천보는이", "nemine@example.com", "Blog#2026ok", true);
        long a = members.localMember("nefriend", "팔로우한이", "nefriend@example.com", "Blog#2026ok", true);
        long b = members.localMember("nefriend2", "팔로우한이둘", "nefriend2@example.com", "Blog#2026ok", true);
        posts.published(me, "내가 쓴 글", "본문", 1, NOW.minus(Duration.ofDays(1)));
        long old = posts.published(a, "팔로우한 사람 지난 글", "본문", 1, NOW.minus(Duration.ofDays(20)));
        jdbc.update("UPDATE post SET comment_count = 1 WHERE id = ?", old);
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?), (?, ?)", me, a, me, b);

        // 팔로우한 사람 글로 채운다, 내 글은 없다
        assertThat(list(me)).contains("팔로우한 사람 지난 글").doesNotContain("내가 쓴 글").doesNotContain("아직 둘러볼 글이 없어요");

        // 댓글 없는 새 글도 추천에 그대로 나온다(10-19 "첫 댓글을 기다리는 글" 칸을 없앴다)
        posts.published(b, "댓글 없는 새 글", "본문", 1, NOW.minus(Duration.ofHours(2)));
        String html = mockMvc.perform(get("/explore").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("waiting-comments").doesNotContain("첫 댓글을 기다리는 글");
        assertThat(html.substring(html.indexOf("id=\"card-grid\""))).contains("댓글 없는 새 글");

        // 내 글뿐이면 비어 있는 게 맞다
        jdbc.update("DELETE FROM post WHERE author_id <> ?", me);
        assertThat(list(me)).doesNotContain("내가 쓴 글");
    }
}
