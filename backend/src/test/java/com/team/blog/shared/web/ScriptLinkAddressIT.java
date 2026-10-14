package com.team.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** 076 후속(강성찬 개인 확장): 스크립트가 그리는 카드·댓글의 글쓴이 링크도 새 주소(/아이디), 옛 /@아이디는 301로 넘어간다. */
class ScriptLinkAddressIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-14T03:00:00Z");

    @Test
    void cardAndCommentAuthorLinksUseNewAddress() throws Exception {
        clock.set(T);
        long writer = members.localMember("slwriter", "쓴이", "slwriter@example.com", "Blog#2026ok", true);
        long reader = members.localMember("slreader", "읽는이", "slreader@example.com", "Blog#2026ok", true);
        long postId = posts.published(writer, "주소 확인 글", "본문", 1, T.minusSeconds(10));
        mockMvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).with(TestAuth.member(reader))
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"좋은 글\"}")).andExpect(status().is2xxSuccessful());

        assertThat(mockMvc.perform(get("/api/posts")).andReturn().getResponse().getContentAsString())
                .contains("\"blogUrl\":\"/slwriter\"").doesNotContain("/@slwriter");
        assertThat(mockMvc.perform(get("/api/posts/{id}/comments", postId)).andReturn().getResponse().getContentAsString())
                .contains("\"blogUrl\":\"/slreader\"").doesNotContain("/@slreader");
        mockMvc.perform(get("/@slwriter")).andExpect(status().isMovedPermanently()).andExpect(header().string("Location", "/slwriter"));
    }
}
