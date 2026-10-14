package com.team.blog.interaction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 088 후속(강성찬 개인 확장): 카드 하트를 칠할 "내가 좋아요 한 글" 번호를 한 번에. 비회원은 빈 목록, 화면에는 회원만 스크립트. */
class LikedMineIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-14T03:00:00Z");

    @Test
    void returnsOnlyMyLikedPostsAmongAsked() throws Exception {
        clock.set(T);
        long me = members.localMember("lmreader", "읽는이", "lmreader@example.com", "Blog#2026ok", true);
        long other = members.localMember("lmother", "다른이", "lmother@example.com", "Blog#2026ok", true);
        long writer = members.localMember("lmwriter", "쓴이", "lmwriter@example.com", "Blog#2026ok", true);
        long a = posts.published(writer, "좋아요 한 글", "본문", 1, T.minusSeconds(30));
        long b = posts.published(writer, "안 한 글", "본문", 1, T.minusSeconds(20));
        long c = posts.published(writer, "남만 한 글", "본문", 1, T.minusSeconds(10));
        mockMvc.perform(put("/api/posts/{id}/like", a).with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        mockMvc.perform(put("/api/posts/{id}/like", c).with(csrf()).with(TestAuth.member(other))).andExpect(status().isOk());

        String ids = a + "," + b + "," + c;
        assertThat(mockMvc.perform(get("/api/likes/mine?ids=" + ids).with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString()).isEqualTo("[" + a + "]");
        assertThat(mockMvc.perform(get("/api/likes/mine?ids=" + ids)).andReturn().getResponse().getContentAsString()).isEqualTo("[]");
        assertThat(mockMvc.perform(get("/api/likes/mine").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString())
                .isEqualTo("[]");
        // 화면: 회원만 하트 칠하는 스크립트
        assertThat(mockMvc.perform(get("/").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString())
                .contains("/js/post/liked-hearts");
        assertThat(mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString()).doesNotContain("liked-hearts.js");
    }
}
