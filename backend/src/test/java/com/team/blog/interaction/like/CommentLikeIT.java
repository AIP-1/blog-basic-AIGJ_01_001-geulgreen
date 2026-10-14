package com.team.blog.interaction.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** 089 댓글·답글 좋아요(강성찬 개인 확장). */
class CommentLikeIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-14T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private long comment(long postId, long who, String body) throws Exception {
        mockMvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).with(TestAuth.member(who))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT max(id) FROM comment", Long.class);
    }

    private int likeNotifications(long receiver) {
        return jdbc.queryForObject("SELECT count(*) FROM notification WHERE receiver_id = ? AND type = 'LIKE' AND comment_id IS NOT NULL",
                Integer.class, receiver);
    }

    @Test
    void likeCommentsAndRepliesLikePosts() throws Exception {
        long author = member("clauthor");
        long writer = member("clwriter");
        long fan = member("clfan");
        long outsider = member("clout");
        long postId = posts.published(author, "댓글 좋아요 시험", "본문", 1, T.minusSeconds(60));
        long root = comment(postId, writer, "{\"content\":\"좋은 글이에요\"}");
        long reply = comment(postId, author, "{\"content\":\"고마워요\",\"replyToCommentId\":" + root + "}");

        mockMvc.perform(put("/api/comments/{id}/like", root).with(csrf()).with(TestAuth.member(fan)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.liked").value(true)).andExpect(jsonPath("$.likeCount").value(1));
        // 같은 상태면 수는 그대로
        mockMvc.perform(put("/api/comments/{id}/like", root).with(csrf()).with(TestAuth.member(fan)))
                .andExpect(jsonPath("$.likeCount").value(1));
        mockMvc.perform(put("/api/comments/{id}/like", reply).with(csrf()).with(TestAuth.member(writer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likeCount").value(1));
        assertThat(likeNotifications(writer)).isEqualTo(1);
        assertThat(likeNotifications(author)).isEqualTo(1);
        assertThat(mockMvc.perform(get("/api/notifications").with(TestAuth.member(writer))).andReturn().getResponse()
                .getContentAsString()).contains("회원님의 댓글을 좋아해요");

        // 자기 댓글은 400, 비회원은 401
        mockMvc.perform(put("/api/comments/{id}/like", root).with(csrf()).with(TestAuth.member(writer)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/comments/{id}/like", root).with(csrf())).andExpect(status().isUnauthorized());

        String api = mockMvc.perform(get("/api/posts/{id}/comments", postId).with(TestAuth.member(fan))).andReturn()
                .getResponse().getContentAsString();
        assertThat(api).contains("\"likeCount\":1").contains("\"liked\":true");
        assertThat(mockMvc.perform(get("/clauthor/posts/" + postId).with(TestAuth.member(fan))).andReturn().getResponse()
                .getContentAsString()).contains("comment-like").contains("data-like-comment-id=\"" + root + "\"");

        // 취소하면 수와 알림이 빠진다
        mockMvc.perform(delete("/api/comments/{id}/like", root).with(csrf()).with(TestAuth.member(fan)))
                .andExpect(jsonPath("$.liked").value(false)).andExpect(jsonPath("$.likeCount").value(0));
        assertThat(likeNotifications(writer)).isZero();

        // 볼 수 없는 비밀 댓글은 없는 것처럼(404)
        long secret = comment(postId, writer, "{\"content\":\"몰래\",\"secret\":true}");
        mockMvc.perform(put("/api/comments/{id}/like", secret).with(csrf()).with(TestAuth.member(outsider)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/comments/{id}/like", secret).with(csrf()).with(TestAuth.member(author)))
                .andExpect(status().isOk());

        // 지우면 좋아요도 지운다(답글이 있어 자리만 남는 댓글도)
        mockMvc.perform(put("/api/comments/{id}/like", root).with(csrf()).with(TestAuth.member(fan)));
        mockMvc.perform(delete("/api/comments/{id}", root).with(csrf()).with(TestAuth.member(writer)))
                .andExpect(status().is2xxSuccessful());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM comment_like WHERE comment_id = ?", Integer.class, root)).isZero();
        mockMvc.perform(delete("/api/comments/{id}", secret).with(csrf()).with(TestAuth.member(writer)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM comment_like WHERE comment_id = ?", Integer.class, secret)).isZero();
    }
}
