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

/** 강성찬 개인 확장(10-16): 탐색 [추천] 맨 위 "첫 댓글을 기다리는 글" — 최근 7일·댓글 0개·전체 공개, 내 글 제외, 최대 4개, 없으면 칸 없음. */
class WaitingCommentIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private String section(String html) {
        int at = html.indexOf("class=\"waiting-comments\"");
        return at < 0 ? "" : html.substring(at, html.indexOf("</section>", at));
    }

    @Test
    void showsRecentPublicPostsWithoutComments() throws Exception {
        clock.set(NOW);
        long me = members.localMember("wcme", "기다림나", "wcme@example.com", "Blog#2026ok", true);
        long other = members.localMember("wcother", "기다림남", "wcother@example.com", "Blog#2026ok", true);
        assertThat(mockMvc.perform(get("/explore")).andReturn().getResponse().getContentAsString()).doesNotContain("waiting-comments");

        for (int i = 0; i < 5; i++) {
            posts.published(other, "기다리는 글" + i, "본문", 1, NOW.minus(Duration.ofHours(i + 1)));
        }
        long commented = posts.published(other, "댓글 있는 글", "본문", 1, NOW.minus(Duration.ofMinutes(30)));
        jdbc.update("UPDATE post SET comment_count = 1 WHERE id = ?", commented);
        posts.published(other, "오래된 글", "본문", 1, NOW.minus(Duration.ofDays(8)));
        long friends = posts.published(other, "친구 공개 글", "본문", 1, NOW.minus(Duration.ofMinutes(10)));
        jdbc.update("UPDATE post SET visibility = 'FRIENDS' WHERE id = ?", friends);
        posts.published(me, "내 글", "본문", 1, NOW.minus(Duration.ofMinutes(5)));

        String html = mockMvc.perform(get("/explore").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        String box = section(html);
        assertThat(box).contains("첫 댓글을 기다리는 글").contains("기다리는 글0").contains("기다리는 글3").doesNotContain("기다리는 글4")
                .doesNotContain("댓글 있는 글").doesNotContain("오래된 글").doesNotContain("친구 공개 글").doesNotContain("내 글");
        assertThat(box.split("class=\"post-card\"", -1)).hasSize(5);
        // 아래 추천 글에 같은 글이 또 나오지 않는다
        assertThat(html.substring(html.indexOf("id=\"card-grid\""))).doesNotContain("기다리는 글0");
        // 트렌딩 탭에는 없다
        assertThat(mockMvc.perform(get("/explore?tab=trending")).andReturn().getResponse().getContentAsString()).doesNotContain("waiting-comments");
    }
}
