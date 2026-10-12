package com.team.blog.post.integration;

import static com.team.blog.post.integration.PostTestSupport.autosave;
import static com.team.blog.post.integration.PostTestSupport.manualSave;
import static com.team.blog.post.integration.PostTestSupport.writer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.sql.Timestamp;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 062 빈 임시글이 안 생기게(강성찬 개인 확장): [새 글]은 빈 임시글을 다시 열고, 비운 채 떠나면 지우고, 목록에서도 뺀다. */
class EmptyDraftLeaveIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private long newPost(long who) throws Exception {
        String location = mockMvc.perform(post("/write").with(csrf()).with(TestAuth.member(who)))
                .andExpect(status().isSeeOther()).andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private void leave(long who, long postId) throws Exception {
        mockMvc.perform(post("/api/posts/{id}/discard-empty", postId).with(csrf()).with(TestAuth.member(who)))
                .andExpect(status().isNoContent());
    }

    @Test
    void newPostReusesEmptyDraftAndLeavingEmptyDeletesIt() throws Exception {
        long me = writer(members, "emptyleave");
        long other = writer(members, "emptyother");
        long first = newPost(me);
        // 아무것도 안 쓴 임시글이 있으면 [새 글]은 그 글을 다시 연다
        assertThat(newPost(me)).isEqualTo(first);
        assertThat(posts.count()).isEqualTo(1);
        // 남의 글은 지울 수 없다
        mockMvc.perform(post("/api/posts/{id}/discard-empty", first).with(csrf()).with(TestAuth.member(other)))
                .andExpect(status().isNotFound());
        // 비운 채 떠나면 지운다
        leave(me, first);
        assertThat(posts.count()).isZero();
        mockMvc.perform(get("/write/{id}", first).with(TestAuth.member(me))).andExpect(status().isNotFound());
        // 쓰는 중(자동 저장 버퍼에만 내용)이면 지우지 않고, [새 글]은 새로 만든다
        long writing = newPost(me);
        mockMvc.perform(autosave(writing, "", "본문을 쓰는 중", 0).with(TestAuth.member(me))).andExpect(status().isOk());
        leave(me, writing);
        assertThat(posts.count()).isEqualTo(1);
        long next = newPost(me);
        assertThat(next).isNotEqualTo(writing);
        // 제목만 있어도 임시글로 남는다
        mockMvc.perform(manualSave(next, "제목만", "", 0).with(TestAuth.member(me))).andExpect(status().isOk());
        leave(me, next);
        assertThat(posts.count()).isEqualTo(2);
    }

    @Test
    void blankDraftsDisappearFromListAfterTwoMinutes() throws Exception {
        long me = writer(members, "emptylist");
        long blank = posts.draft(me, "", "", 0);
        long titled = posts.draft(me, "쓰다 만 글", "", 0);
        // 막 만든 빈 임시글은 보인다(자동 저장이 DB에 반영되기 전일 수 있음)
        String fresh = mockMvc.perform(get("/manage/posts").param("tab", "drafts").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        assertThat(fresh).contains("임시글 2").contains("(제목 없음)").contains("쓰다 만 글");
        // 2분이 지나면 빈 임시글은 목록·개수에서 빠진다
        jdbc.update("UPDATE post SET updated_at = ? WHERE id IN (?, ?)", Timestamp.from(clock.instant().minus(Duration.ofMinutes(3))),
                blank, titled);
        String later = mockMvc.perform(get("/manage/posts").param("tab", "drafts").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        assertThat(later).contains("임시글 1").doesNotContain("(제목 없음)").contains("쓰다 만 글");
    }
}
