package com.team.blog.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.friend.application.FriendPurgeStep;
import com.team.blog.friend.application.FriendQuery;
import com.team.blog.friend.application.FriendQuery.Status;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 025 친구 공개 + 069 친한 친구(강성찬 개인 확장): 내가 팔로우하는 사람 중에서 골라 넣는 한쪽 관계. */
class FriendIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-09T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FriendQuery friendQuery;

    @Autowired
    FriendPurgeStep purgeStep;

    private long member(String handle) {
        return members.localMember(handle, handle.substring(0, Math.min(10, handle.length())), handle + "@example.com",
                "Blog#2026ok", true);
    }

    private MockHttpServletRequestBuilder friend(String handle, String action) {
        return post("/" + handle + "/friend").param("action", action).with(csrf());
    }

    private void follow(long follower, long followee) {
        jdbc.update("INSERT INTO follow (follower_id, followee_id, created_at) VALUES (?, ?, now())", follower, followee);
    }

    /** 친구 공개 글: 발행한 뒤 공개 범위를 FRIENDS로(친구 공개 글에는 최초 공개 시각이 없다). */
    private long friendsPost(long author, String title, Instant at) {
        long id = posts.published(author, title, "본문 " + title, 1, at);
        jdbc.update("UPDATE post SET visibility = 'FRIENDS', first_public_at = NULL WHERE id = ?", id);
        return id;
    }

    @Test
    void ownerPicksPeopleTheyFollowOneWayWithoutAskingOrNotifying() throws Exception {
        clock.set(T);
        long owner = member("frowner");
        long pick = member("frpick");
        long stranger = member("frnotfollowed");
        follow(owner, pick);
        assertThat(friendQuery.status(owner, pick)).isEqualTo(Status.FOLLOWING);
        assertThat(friendQuery.status(pick, owner)).isEqualTo(Status.NONE);
        // 내가 팔로우하는 사람: 바로 친구(수락 없음), 두 번 넣어도 한 줄
        mockMvc.perform(friend("frpick", "add").with(TestAuth.member(owner))).andExpect(status().is3xxRedirection());
        mockMvc.perform(friend("frpick", "add").with(TestAuth.member(owner))).andExpect(status().is3xxRedirection());
        assertThat(friendQuery.hasFriend(owner, pick)).isTrue();
        assertThat(friendQuery.hasFriend(pick, owner)).as("한쪽 관계").isFalse();
        assertThat(friendQuery.status(owner, pick)).isEqualTo(Status.FRIENDS);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM close_friend", Integer.class)).isEqualTo(1);
        // 내가 팔로우하지 않는 사람·나 자신은 넣을 수 없다(아무것도 바뀌지 않음)
        mockMvc.perform(friend("frnotfollowed", "add").with(TestAuth.member(owner))).andExpect(status().is3xxRedirection());
        mockMvc.perform(friend("frowner", "add").with(TestAuth.member(owner))).andExpect(status().is3xxRedirection());
        assertThat(friendQuery.hasFriend(owner, stranger)).isFalse();
        // 팔로우받는 쪽은 팔로우하지 않았으니 거꾸로 넣을 수 없다
        mockMvc.perform(friend("frowner", "add").with(TestAuth.member(pick)));
        assertThat(friendQuery.hasFriend(pick, owner)).isFalse();
        // 빼기, 누구에게도 알림 없음
        mockMvc.perform(friend("frpick", "remove").with(TestAuth.member(owner))).andExpect(status().is3xxRedirection());
        assertThat(friendQuery.status(owner, pick)).isEqualTo(Status.FOLLOWING);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE receiver_id IN (?, ?) AND type <> 'FOLLOW'",
                Integer.class, owner, pick)).isZero();
        // 없는 회원 404, 비회원은 로그인
        mockMvc.perform(friend("nobody-here", "add").with(TestAuth.member(owner))).andExpect(status().isNotFound());
        assertThat(mockMvc.perform(friend("frpick", "add")).andReturn().getResponse().getHeader("Location")).contains("/login");
    }

    @Test
    void friendsPostsAreReadableOnlyByPickedPeopleAndNeverListedPublicly() throws Exception {
        clock.set(T);
        long author = member("frauthor");
        long friend = member("frfriend");
        long stranger = member("frstranger");
        members.addFriend(author, friend);
        posts.published(author, "공개 글 하나", "본문", 1, T.minusSeconds(3600));
        long secret = friendsPost(author, "친구만 보는 비밀글", T.minusSeconds(60));
        String url = "/frauthor/posts/" + secret;
        mockMvc.perform(get(url).with(TestAuth.member(friend))).andExpect(status().isOk());
        mockMvc.perform(get(url).with(TestAuth.member(author))).andExpect(status().isOk());
        String strangerPage = mockMvc.perform(get(url).with(TestAuth.member(stranger))).andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(strangerPage).contains("볼 수 없는 글이에요").doesNotContain("친구만 보는 비밀글");
        mockMvc.perform(get(url)).andExpect(status().isNotFound());
        // 팔로우만 주고받는 사람·관리자도 404(관리자 예외 없음)
        long fan = member("frfanonly");
        follow(fan, author);
        follow(author, fan);
        mockMvc.perform(get(url).with(TestAuth.member(fan))).andExpect(status().isNotFound());
        long admin = member("fradmin");
        mockMvc.perform(get(url).with(TestAuth.admin(admin))).andExpect(status().isNotFound());
        // 한쪽 관계: 글쓴이는 친구가 넣지 않았으니 친구의 친구 공개 글을 못 본다
        long back = friendsPost(friend, "거꾸로는 안 보이는 글", T.minusSeconds(30));
        mockMvc.perform(get("/frfriend/posts/" + back).with(TestAuth.member(author))).andExpect(status().isNotFound());
        // 친구가 보는 블로그: 공개 + 친구 공개(발행 시각 최신순), 다른 사람에게는 공개만
        String friendBlog = mockMvc.perform(get("/frauthor").with(TestAuth.member(friend))).andReturn().getResponse().getContentAsString();
        assertThat(friendBlog).contains("친구만 보는 비밀글").contains("공개 글 하나");
        assertThat(friendBlog.indexOf("친구만 보는 비밀글")).isLessThan(friendBlog.indexOf("공개 글 하나"));
        assertThat(mockMvc.perform(get("/api/members/frauthor/posts").with(TestAuth.member(friend))).andReturn().getResponse()
                .getContentAsString()).contains("친구만 보는 비밀글");
        for (String path : new String[] {"/frauthor", "/api/members/frauthor/posts"}) {
            assertThat(mockMvc.perform(get(path).with(TestAuth.member(stranger))).andReturn().getResponse().getContentAsString())
                    .as(path).contains("공개 글 하나").doesNotContain("친구만 보는 비밀글");
        }
        // 공용 목록(홈·트렌딩·검색·피드)에는 친구에게도 나오지 않는다
        for (String path : new String[] {"/", "/explore", "/explore?tab=trending", "/api/posts", "/feed", "/search?q=" + "비밀글"}) {
            assertThat(mockMvc.perform(get(path).with(TestAuth.member(friend))).andReturn().getResponse().getContentAsString())
                    .as(path).doesNotContain("친구만 보는 비밀글");
        }
        // 친구 반응은 공개 글과 같은 규칙, 친구 아님은 404
        mockMvc.perform(put("/api/posts/{id}/like", secret).with(csrf()).with(TestAuth.member(friend)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/posts/{id}/like", secret).with(csrf()).with(TestAuth.member(stranger)))
                .andExpect(status().isNotFound());
        // 글쓴이가 빼면 즉시 404, 블로그에서도 빠진다
        mockMvc.perform(friend("frfriend", "remove").with(TestAuth.member(author)));
        mockMvc.perform(get(url).with(TestAuth.member(friend))).andExpect(status().isNotFound());
        assertThat(mockMvc.perform(get("/frauthor").with(TestAuth.member(friend))).andReturn().getResponse()
                .getContentAsString()).doesNotContain("친구만 보는 비밀글");
    }

    @Test
    void blogHeaderShowsAddButtonOnlyForPeopleIFollow() throws Exception {
        long me = member("frbtnme");
        long followed = member("frbtnfan");
        member("frbtnother");
        follow(me, followed);
        assertThat(mockMvc.perform(get("/frbtnfan").with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString()).contains("친구로 넣기");
        assertThat(mockMvc.perform(get("/frbtnother").with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString()).doesNotContain("친구로 넣기").doesNotContain("친구 요청");
        assertThat(mockMvc.perform(get("/frbtnfan")).andReturn().getResponse().getContentAsString()).doesNotContain("친구로 넣기");
        // 087 후속: 내 블로그 숫자 줄에만 "친구 N" → 친구 관리
        members.addFriend(me, followed);
        assertThat(mockMvc.perform(get("/frbtnme").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString())
                .contains("href=\"/settings/friends\"").contains("친구 1");
        assertThat(mockMvc.perform(get("/frbtnme").with(TestAuth.member(followed))).andReturn().getResponse().getContentAsString())
                .doesNotContain("href=\"/settings/friends\"");
    }

    @Test
    void unfollowingDropsTheFriendAndTheirPlaceInMyGroups() throws Exception {
        long me = member("frunme");
        long pal = member("frunpal");
        members.addFriend(me, pal);
        mockMvc.perform(post("/settings/groups").param("name", "가까운 사람").with(csrf()).with(TestAuth.member(me)));
        long group = jdbc.queryForObject("SELECT id FROM friend_group WHERE owner_id = ?", Long.class, me);
        mockMvc.perform(post("/settings/groups/{g}/members", group).param("memberId", String.valueOf(pal)).with(csrf())
                .with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        mockMvc.perform(delete("/api/members/frunpal/follow").with(csrf()).with(TestAuth.member(me))).andExpect(status().isOk());
        assertThat(friendQuery.hasFriend(me, pal)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM group_member WHERE group_id = ?", Integer.class, group)).isZero();
    }

    @Test
    void friendPageSearchesPeopleIFollowByHandleOrNickname() throws Exception {
        long me = member("frsrchme");
        follow(me, members.localMember("frsrch_kim", "김하늘", "kim@example.com", "Blog#2026ok", true));
        follow(me, members.localMember("frsrchlee", "이바다", "lee@example.com", "Blog#2026ok", true));
        member("frsrchnot");
        String page = mockMvc.perform(get("/settings/friends").param("q", "하늘").with(TestAuth.member(me)))
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains(">frsrch_kim<").doesNotContain(">frsrchlee<");
        assertThat(mockMvc.perform(get("/settings/friends").param("q", "frsrch").with(TestAuth.member(me))).andReturn()
                .getResponse().getContentAsString()).contains(">frsrch_kim<").contains(">frsrchlee<").doesNotContain(">frsrchnot<");
        assertThat(mockMvc.perform(get("/settings/friends").param("q", "없는사람").with(TestAuth.member(me))).andReturn()
                .getResponse().getContentAsString()).contains("찾는 사람이 없어요");
        // _는 글자 그대로
        assertThat(mockMvc.perform(get("/settings/friends").param("q", "h_k").with(TestAuth.member(me))).andReturn()
                .getResponse().getContentAsString()).contains(">frsrch_kim<").doesNotContain(">frsrchlee<");
    }

    @Test
    void authorCanChooseFriendsVisibilityAndDefault() throws Exception {
        long author = member("frchooser");
        long id = posts.published(author, "범위 바꿀 글", "본문", 1, T);
        mockMvc.perform(patch("/api/posts/{id}/visibility", id).with(csrf()).with(TestAuth.member(author))
                .contentType(MediaType.APPLICATION_JSON).content("{\"visibility\":\"FRIENDS\"}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT visibility FROM post WHERE id = ?", String.class, id)).isEqualTo("FRIENDS");
        mockMvc.perform(patch("/api/me/settings").with(csrf()).with(TestAuth.member(author))
                .contentType(MediaType.APPLICATION_JSON).content("{\"defaultVisibility\":\"FRIENDS\"}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT default_visibility FROM member WHERE id = ?", String.class, author)).isEqualTo("FRIENDS");
    }

    @Test
    void friendPageIsOwnerOnlyAndWithdrawalPurgesRelations() throws Exception {
        long a = member("frlista");
        long b = member("frlistb");
        long c = member("frlistc");
        members.addFriend(a, b);
        follow(a, c);
        members.addFriend(c, a);
        String page = mockMvc.perform(get("/settings/friends").with(TestAuth.member(a))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("내 친구 1").contains("내가 팔로우하는 사람 1").contains(">frlistb<").contains(">frlistc<")
                .doesNotContain("받은 친구 요청");
        assertThat(mockMvc.perform(get("/settings/friends")).andReturn().getResponse().getHeader("Location")).contains("/login");
        purgeStep.purge(a);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM close_friend WHERE ? IN (owner_id, friend_id)", Integer.class, a))
                .isZero();
    }
}
