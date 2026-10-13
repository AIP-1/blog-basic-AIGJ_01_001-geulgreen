package com.team.blog.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.privacy.application.PrivacyPurgeStep;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 073 비공개 계정(강성찬 개인 확장, 인스타처럼): 팔로우는 요청, 수락한 사람만 글을 본다. */
class PrivateAccountIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-13T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PrivacyPurgeStep purgeStep;

    private long member(String handle) {
        return members.localMember(handle, handle.substring(0, Math.min(10, handle.length())), handle + "@example.com",
                "Blog#2026ok", true);
    }

    private void makePrivate(long owner) throws Exception {
        mockMvc.perform(post("/settings/privacy").param("private", "true").with(csrf()).with(TestAuth.member(owner)))
                .andExpect(status().is3xxRedirection());
    }

    private String page(String path, long viewer) throws Exception {
        return mockMvc.perform(get(path).with(TestAuth.member(viewer))).andReturn().getResponse().getContentAsString();
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void strangersSeeOnlyTheProfileTopUntilTheOwnerAccepts() throws Exception {
        clock.set(T);
        long owner = member("pvowner");
        long old = member("pvoldfan");
        long fan = member("pvfan");
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?)", old, owner);
        long postId = posts.published(owner, "비공개 계정의 글", "본문", 1, T.minusSeconds(60));
        makePrivate(owner);

        // 수락받지 않은 사람: 프로필 윗부분만, 글·다른 주소·API는 막힘
        assertThat(page("/pvowner", fan)).contains("비공개 계정이에요").doesNotContain("비공개 계정의 글");
        assertThat(mockMvc.perform(get("/pvowner")).andReturn().getResponse().getContentAsString())
                .contains("비공개 계정이에요").doesNotContain("비공개 계정의 글");
        String url = "/pvowner/posts/" + postId;
        assertThat(mockMvc.perform(get(url).with(TestAuth.member(fan))).andReturn().getResponse().getRedirectedUrl())
                .isEqualTo("/pvowner");
        assertThat(mockMvc.perform(get("/pvowner/followers").with(TestAuth.member(fan))).andReturn().getResponse()
                .getRedirectedUrl()).isEqualTo("/pvowner");
        mockMvc.perform(get("/api/members/pvowner/posts").with(TestAuth.member(fan))).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/posts/{id}/like", postId).with(csrf()).with(TestAuth.member(fan))).andExpect(status().isNotFound());
        // 원래 팔로워·본인은 그대로 본다
        assertThat(page("/pvowner", old)).contains("비공개 계정의 글");
        assertThat(page(url, old)).contains("비공개 계정의 글");
        assertThat(page("/pvowner", owner)).contains("비공개 계정의 글");

        // 팔로우를 누르면 요청: 알림 1건, 다시 눌러도 한 줄, 화면은 "요청됨"
        String res = mockMvc.perform(put("/api/members/pvowner/follow").with(csrf()).with(TestAuth.member(fan)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(res).contains("\"requested\":true").contains("\"following\":false");
        mockMvc.perform(put("/api/members/pvowner/follow").with(csrf()).with(TestAuth.member(fan)));
        assertThat(count("SELECT count(*) FROM follow_request WHERE target_id = ?", owner)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM follow WHERE follower_id = ?", fan)).isZero();
        assertThat(count("SELECT count(*) FROM notification WHERE receiver_id = ? AND type = 'FOLLOW_REQUEST'", owner)).isEqualTo(1);
        assertThat(page("/pvowner", fan)).contains("요청됨").contains("팔로우 요청을 보냈어요");
        assertThat(page("/settings/privacy", owner)).contains("받은 팔로우 요청 1").contains(">pvfan<");

        // 수락: 팔로우가 되고 글이 열린다, 요청 알림은 빠진다
        mockMvc.perform(post("/settings/privacy/requests/pvfan").param("action", "accept").with(csrf()).with(TestAuth.member(owner)))
                .andExpect(status().is3xxRedirection());
        assertThat(count("SELECT count(*) FROM follow WHERE follower_id = ? AND followee_id = ?", fan, owner)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM follow_request")).isZero();
        assertThat(count("SELECT count(*) FROM notification WHERE receiver_id = ? AND type = 'FOLLOW_REQUEST'", owner)).isZero();
        assertThat(page(url, fan)).contains("비공개 계정의 글");
        assertThat(page("/api/members/pvowner/posts", fan)).contains("비공개 계정의 글");
        // 팔로잉 피드에는 나오고, 여러 사람이 섞이는 목록에는 팔로워에게도 나오지 않는다
        assertThat(page("/feed", fan)).contains("비공개 계정의 글");
        for (String path : new String[] {"/", "/api/posts", "/search?q=비공개", "/sitemap.xml", "/pvowner/rss"}) {
            assertThat(page(path, fan)).as(path).doesNotContain("비공개 계정의 글");
        }
        // 언팔로우하면 다시 잠긴다
        mockMvc.perform(delete("/api/members/pvowner/follow").with(csrf()).with(TestAuth.member(fan))).andExpect(status().isOk());
        assertThat(page("/pvowner", fan)).contains("비공개 계정이에요");
    }

    @Test
    void declineCancelAndGoingPublicAcceptsEveryPendingRequest() throws Exception {
        long owner = member("pvowner2");
        long a = member("pvreqa");
        long b = member("pvreqb");
        long c = member("pvreqc");
        makePrivate(owner);
        for (long m : new long[] {a, b, c}) {
            mockMvc.perform(put("/api/members/pvowner2/follow").with(csrf()).with(TestAuth.member(m)));
        }
        // 거절, 요청한 사람이 취소: 상대에게 알림 없음
        mockMvc.perform(post("/settings/privacy/requests/pvreqa").param("action", "decline").with(csrf()).with(TestAuth.member(owner)));
        String res = mockMvc.perform(delete("/api/members/pvowner2/follow").with(csrf()).with(TestAuth.member(b)))
                .andReturn().getResponse().getContentAsString();
        assertThat(res).contains("\"requested\":false");
        assertThat(count("SELECT count(*) FROM follow_request")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM notification WHERE receiver_id IN (?, ?)", a, b)).isZero();
        // 공개로 바꾸면 남은 요청은 모두 수락
        mockMvc.perform(post("/settings/privacy").param("private", "false").with(csrf()).with(TestAuth.member(owner)));
        assertThat(count("SELECT count(*) FROM follow WHERE followee_id = ?", owner)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM follow WHERE follower_id = ? AND followee_id = ?", c, owner)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM private_account")).isZero();
        // 공개 계정은 바로 팔로우
        String direct = mockMvc.perform(put("/api/members/pvowner2/follow").with(csrf()).with(TestAuth.member(a)))
                .andReturn().getResponse().getContentAsString();
        assertThat(direct).contains("\"following\":true").contains("\"requested\":false");
    }

    @Test
    void privateAccountCanBeAFriendOnlyAfterTheRequestIsAccepted() throws Exception {
        long owner = member("pvfrowner");
        long me = member("pvfrme");
        makePrivate(owner);
        mockMvc.perform(put("/api/members/pvfrowner/follow").with(csrf()).with(TestAuth.member(me)));
        mockMvc.perform(post("/pvfrowner/friend").param("action", "add").with(csrf()).with(TestAuth.member(me)));
        assertThat(count("SELECT count(*) FROM close_friend")).isZero();
        mockMvc.perform(post("/settings/privacy/requests/pvfrme").param("action", "accept").with(csrf()).with(TestAuth.member(owner)));
        mockMvc.perform(post("/pvfrowner/friend").param("action", "add").with(csrf()).with(TestAuth.member(me)));
        assertThat(count("SELECT count(*) FROM close_friend WHERE owner_id = ? AND friend_id = ?", me, owner)).isEqualTo(1);
    }

    @Test
    void settingsPageIsOwnerOnlyAndWithdrawalPurges() throws Exception {
        long owner = member("pvpurge");
        long fan = member("pvpurgefan");
        assertThat(mockMvc.perform(get("/settings/privacy")).andReturn().getResponse().getHeader("Location")).contains("/login");
        makePrivate(owner);
        mockMvc.perform(put("/api/members/pvpurge/follow").with(csrf()).with(TestAuth.member(fan)));
        purgeStep.purge(owner);
        assertThat(count("SELECT count(*) FROM private_account")).isZero();
        assertThat(count("SELECT count(*) FROM follow_request")).isZero();
    }
}
