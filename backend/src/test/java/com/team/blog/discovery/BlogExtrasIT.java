package com.team.blog.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 051~054 방문자 수·읽기 진행 막대·공유하기·글 고정(강성찬 개인 확장). */
class BlogExtrasIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-12T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    @Test
    void visitorsCountedOncePerDayAndNotOwner() throws Exception {
        long owner = member("visitowner");
        long other = member("visitother");
        long postId = posts.published(owner, "방문 글", "본문", 1, T);
        clock.set(T.plusSeconds(3600));
        mockMvc.perform(get("/visitowner").with(TestAuth.member(owner)));
        mockMvc.perform(get("/visitowner").with(TestAuth.member(other)));
        mockMvc.perform(get("/visitowner/posts/{id}", postId).with(TestAuth.member(other)));
        mockMvc.perform(get("/visitowner").cookie(new Cookie("vid", "guest-one")));
        String html = mockMvc.perform(get("/visitowner").with(TestAuth.member(owner))).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("오늘 2</span> · <span class=\"meta-part\">전체 2");
        clock.set(T.plusSeconds(3600 * 25));
        mockMvc.perform(get("/visitowner").with(TestAuth.member(other)));
        assertThat(mockMvc.perform(get("/visitowner").with(TestAuth.member(owner))).andReturn().getResponse().getContentAsString())
                .contains("오늘 1</span> · <span class=\"meta-part\">전체 3");
    }

    @Test
    void progressShareAndPin() throws Exception {
        long me = member("pinme");
        long other = member("pinother");
        long first = posts.published(me, "고정할 글", "본문", 1, T.minusSeconds(100));
        posts.published(me, "새 글", "본문", 1, T);
        String html = mockMvc.perform(get("/pinme/posts/{id}", first)).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("id=\"read-progress\"").contains("share-row").contains("twitter.com/intent/tweet").contains("share.naver.com/web/shareView")
                .contains("/js/post/reading-extras-").doesNotContain("블로그 맨 위에 고정");
        assertThat(mockMvc.perform(get("/pinme/posts/{id}", first).with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString()).contains("블로그 맨 위에 고정");
        mockMvc.perform(post("/posts/{id}/pin", first).with(csrf()).with(TestAuth.member(other))).andExpect(status().isNotFound());
        mockMvc.perform(post("/posts/{id}/pin", first).with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        String blog = mockMvc.perform(get("/pinme")).andReturn().getResponse().getContentAsString();
        blog = blog.substring(blog.indexOf("<body")); // 검색 문구(head)에도 글 제목이 들어가서 순서는 본문에서만
        assertThat(blog).contains("class=\"pinned-label\"");
        assertThat(blog.indexOf("고정할 글")).isLessThan(blog.indexOf("새 글"));
        // 비공개로 바꾸면 목록에서 고정 글도 안 보인다
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", first);
        assertThat(mockMvc.perform(get("/pinme")).andReturn().getResponse().getContentAsString()).doesNotContain("class=\"pinned-label\"");
        jdbc.update("UPDATE post SET visibility = 'PUBLIC' WHERE id = ?", first);
        mockMvc.perform(post("/posts/{id}/unpin", first).with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(mockMvc.perform(get("/pinme")).andReturn().getResponse().getContentAsString()).doesNotContain("class=\"pinned-label\"");
    }
}
