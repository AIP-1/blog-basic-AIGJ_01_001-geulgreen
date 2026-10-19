package com.team.blog.post.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** 113 후속 강성찬 개인 확장: 임시글 한 번에 발행(저장된 설정, 빠진 글은 건너뜀, 팔로워 알림 한 번). */
class DraftBulkPublishIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void publishesSelectedDraftsSkipsIncompleteAndNotifiesOnce() throws Exception {
        long me = members.localMember("bulkme", "bulkme", "bulkme@example.com", "Blog#2026ok", true);
        long fan = members.localMember("bulkfan", "bulkfan", "bulkfan@example.com", "Blog#2026ok", true);
        long other = members.localMember("bulkoth", "bulkoth", "bulkoth@example.com", "Blog#2026ok", true);
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?)", fan, me);
        long a = posts.draft(me, "스터디 1주차", "본문 하나", 0);
        long b = posts.draft(me, "스터디 2주차", "본문 둘", 0);
        long noTitle = posts.draft(me, "", "제목 없는 본문", 0);
        long friends = posts.draft(me, "친구에게만", "본문 셋", 0);
        jdbc.update("UPDATE post SET visibility = 'FRIENDS' WHERE id = ?", friends);
        long notMine = posts.draft(other, "남의 임시글", "본문", 0);

        String json = mockMvc.perform(post("/api/me/posts/publish-drafts").with(csrf()).with(TestAuth.member(me))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postIds\":[" + a + "," + b + "," + noTitle + "," + friends + "," + notMine + "]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(json).contains("\"published\":[" + a + "," + b + "," + friends + "]").contains("제목이 없어요").contains("임시글이 아니에요");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE id IN (?, ?, ?) AND status = 'PUBLISHED'", Integer.class, a, b, friends)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT visibility FROM post WHERE id = ?", String.class, friends)).isEqualTo("FRIENDS"); // 저장된 설정 그대로
        assertThat(jdbc.queryForObject("SELECT status FROM post WHERE id = ?", String.class, noTitle)).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT status FROM post WHERE id = ?", String.class, notMine)).isEqualTo("DRAFT");
        // 팔로워에게는 새 글 알림 한 번(전체 공개 2편)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE receiver_id = ? AND type = 'NEW_POST'", Integer.class, fan)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actor_count FROM notification WHERE receiver_id = ? AND type = 'NEW_POST'", Integer.class, fan)).isEqualTo(2);
        assertThat(mockMvc.perform(get("/api/notifications").with(TestAuth.member(fan))).andReturn().getResponse().getContentAsString())
                .contains("새 글 2편을 올렸어요").contains("외 1편");
    }

    @Test
    void rejectsEmptyOrTooMany() throws Exception {
        long me = members.localMember("bulkme2", "bulkme2", "bulkme2@example.com", "Blog#2026ok", true);
        mockMvc.perform(post("/api/me/posts/publish-drafts").with(csrf()).with(TestAuth.member(me))
                .contentType(MediaType.APPLICATION_JSON).content("{\"postIds\":[]}")).andExpect(status().isBadRequest());
    }
}
