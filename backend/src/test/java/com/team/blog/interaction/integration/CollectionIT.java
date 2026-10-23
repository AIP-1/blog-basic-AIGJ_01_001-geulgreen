package com.team.blog.interaction.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.interaction.application.InteractionPurgeSteps;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 10-23 chan 모아보기: 좋아요한 글·저장한 글 목록과 글 저장. */
class CollectionIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-23T03:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InteractionPurgeSteps.Likes likesPurge;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private String html(String url, long me) throws Exception {
        return mockMvc.perform(get(url).with(TestAuth.member(me))).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    @Test
    void likedTabListsMyLikesNewestFirstAndDropsTrashed() throws Exception {
        clock.set(T);
        long me = member("colreader");
        long writer = member("colwriter");
        long older = posts.published(writer, "먼저 좋아요한 글", "본문", 1, T.minusSeconds(300));
        long newer = posts.published(writer, "나중에 좋아요한 글", "본문", 1, T.minusSeconds(600));
        long gone = posts.published(writer, "지워질 글", "본문", 1, T.minusSeconds(900));
        long notMine = posts.published(writer, "안 누른 글", "본문", 1, T.minusSeconds(100));
        mockMvc.perform(put("/api/posts/{id}/like", older).with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        clock.set(T.plusSeconds(10));
        mockMvc.perform(put("/api/posts/{id}/like", gone).with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        clock.set(T.plusSeconds(20));
        mockMvc.perform(put("/api/posts/{id}/like", newer).with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        jdbc.update("UPDATE post SET deleted_at = ? WHERE id = ?", java.sql.Timestamp.from(T), gone);

        String page = html("/me/liked", me);
        assertThat(page).contains("모아보기").contains("나중에 좋아요한 글").contains("먼저 좋아요한 글")
                .doesNotContain("지워질 글").doesNotContain("안 누른 글");
        // 누른 순서(최근 먼저): 글 공개 시각과 반대
        assertThat(page.indexOf("나중에 좋아요한 글")).isLessThan(page.indexOf("먼저 좋아요한 글"));
        assertThat(html("/me/saved", me)).contains("아직 저장한 글이 없어요").doesNotContain("먼저 좋아요한 글");
        mockMvc.perform(get("/api/me/liked").with(TestAuth.member(me)))
                .andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.items[0].id").value(newer));
        assertThat(notMine).isPositive();
    }

    @Test
    void likedTabPagesWithCursor() throws Exception {
        long me = member("colpager");
        long writer = member("colpagew");
        for (int i = 0; i < 13; i++) {
            clock.set(T.plusSeconds(i));
            long p = posts.published(writer, "글 " + i, "본문", 1, T.minusSeconds(1000 - i));
            mockMvc.perform(put("/api/posts/{id}/like", p).with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        }
        String first = mockMvc.perform(get("/api/me/liked").with(TestAuth.member(me)))
                .andExpect(jsonPath("$.items.length()").value(12)).andExpect(jsonPath("$.items[0].title").value("글 12"))
                .andReturn().getResponse().getContentAsString();
        String cursor = com.jayway.jsonpath.JsonPath.read(first, "$.nextCursor");
        mockMvc.perform(get("/api/me/liked").param("cursor", cursor).with(TestAuth.member(me)))
                .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].title").value("글 0"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void saveIsPrivateIdempotentAndShownOnDetail() throws Exception {
        clock.set(T);
        long me = member("colsaver");
        long other = member("colother");
        long writer = member("colsavew");
        long post = posts.published(writer, "저장할 글", "본문", 1, T.minusSeconds(60));
        mockMvc.perform(put("/api/posts/{id}/save", post).with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        mockMvc.perform(put("/api/posts/{id}/save", post).with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post_bookmark WHERE post_id = ?", Integer.class, post)).isEqualTo(1);
        // 글쓴이에게 알림·좋아요 수 변화 없음
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE receiver_id = ?", Integer.class, writer)).isZero();
        assertThat((Integer) posts.post(post).get("like_count")).isZero();

        assertThat(html("/me/saved", me)).contains("저장할 글");
        assertThat(html("/me/saved", other)).doesNotContain("저장할 글");
        assertThat(html("/me/liked", me)).doesNotContain("저장할 글");

        mockMvc.perform(delete("/api/posts/{id}/save", post).with(csrf()).with(TestAuth.member(me)))
                .andExpect(jsonPath("$.saved").value(false));
        assertThat(html("/me/saved", me)).doesNotContain("저장할 글");
    }

    @Test
    void detailShowsSaveButtonStateAndFormWorksWithoutScript() throws Exception {
        clock.set(T);
        long me = member("colform");
        long writer = member("colformw");
        long post = posts.published(writer, "폼으로 저장", "본문", 1, T.minusSeconds(60));
        String detail = "/colformw/posts/" + post;
        assertThat(html(detail, me)).contains("class=\"save-form\"").contains("aria-pressed=\"false\"");
        mockMvc.perform(post("/@colformw/posts/" + post + "/save").param("saved", "true").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().isSeeOther());
        assertThat(html(detail, me)).containsPattern("class=\"save-button\"[^>]*aria-pressed=\"true\"");
        // 비회원에게는 저장 버튼이 없다
        assertThat(mockMvc.perform(get(detail)).andReturn().getResponse().getContentAsString()).doesNotContain("save-form");
    }

    @Test
    void guestsAreSentToLoginAndCannotSave() throws Exception {
        long writer = member("colguestw");
        long post = posts.published(writer, "글", "본문", 1, T);
        mockMvc.perform(get("/me/liked")).andExpect(status().is3xxRedirection());
        mockMvc.perform(put("/api/posts/{id}/save", post).with(csrf())).andExpect(status().is4xxClientError());
    }

    @Test
    void withdrawalPurgeRemovesBookmarks() throws Exception {
        clock.set(T);
        long me = member("colpurge");
        long writer = member("colpurgew");
        long post = posts.published(writer, "글", "본문", 1, T);
        mockMvc.perform(put("/api/posts/{id}/save", post).with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        likesPurge.purge(me);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post_bookmark WHERE member_id = ?", Integer.class, me)).isZero();
    }
}
