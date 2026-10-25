package com.team.blog.invite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.support.Browser;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** 10-25 chan: 친구 초대 링크 /i/아이디[/코드] — 비회원 안내 화면, 로그인·가입 뒤 자동 수락(팔로우 + 방 참여), 내 초대 링크 화면. */
class InviteIT extends IntegrationTestBase {

    private static final String PASSWORD = "Blog#2026ok";

    @Autowired
    JdbcTemplate jdbc;

    private long createLinkClub(long owner, String name) throws Exception {
        clock.set(Instant.parse("2026-10-16T03:00:00Z"));
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner)).param("name", name)
                .param("everyDays", "7").param("startDate", "2026-10-12").param("endDate", "2026-11-08").param("joinMode", "LINK"))
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private String code(long club) {
        return jdbc.queryForObject("SELECT invite_code FROM writing_club WHERE id = ?", String.class, club);
    }

    private boolean follows(long follower, long followee) {
        return jdbc.queryForObject("SELECT count(*) FROM follow WHERE follower_id = ? AND followee_id = ?", Integer.class, follower, followee) == 1;
    }

    private boolean member(long club, long who) {
        return jdbc.queryForObject("SELECT count(*) FROM writing_club_member WHERE club_id = ? AND member_id = ?", Integer.class, club, who) == 1;
    }

    @Test
    void anonymousSeesInvitationAndIsTakenBackAfterLoginToJoinAndFollow() throws Exception {
        long inviter = members.localMember("inviter1", "초대한이", "inviter1@example.com", PASSWORD, true);
        long guest = members.localMember("guest1", "손님", "guest1@example.com", PASSWORD, true);
        long club = createLinkClub(inviter, "우리 반 글방");
        String path = "/i/inviter1/" + code(club);

        Browser browser = new Browser(mockMvc);
        MvcResult landing = browser.perform(get(path));
        assertThat(landing.getResponse().getStatus()).isEqualTo(200);
        String html = landing.getResponse().getContentAsString();
        assertThat(html).contains("초대한이님이</span> <span class=\"invite-fixed\">함께 글 쓰자고 초대했어요").contains("우리 반 글방")
                .contains("Google로 시작하기").contains("이메일로 시작하기").contains("/login?redirect=%2Fi%2Finviter1%2F");

        // 로그인하면(redirect 없이도) 초대 경로로 돌아가고, 그 화면이 바로 받아들인다
        MvcResult login = browser.perform(post("/login").with(csrf()).param("email", "guest1@example.com").param("password", PASSWORD));
        assertThat(login.getResponse().getStatus()).isEqualTo(303);
        assertThat(login.getResponse().getHeader("Location")).isEqualTo(path);
        MvcResult accepted = browser.perform(get(path));
        assertThat(accepted.getResponse().getStatus()).isEqualTo(303);
        assertThat(accepted.getResponse().getHeader("Location")).isEqualTo("/missions/clubs/" + club);
        assertThat(follows(guest, inviter)).isTrue();
        assertThat(member(club, guest)).isTrue();

        // 다시 열면(이미 멤버) 버튼 화면이 아니라 안내만, 두 번 들어가지 않는다
        MvcResult again = browser.perform(get(path));
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(again.getResponse().getContentAsString()).contains("함께 시작하기");
    }

    @Test
    void loggedInMemberSeesOneButtonAndPostFollowsInviter() throws Exception {
        long inviter = members.localMember("inviter2", "글친구", "inviter2@example.com", PASSWORD, true);
        long guest = members.localMember("guest2", "손님둘", "guest2@example.com", PASSWORD, true);

        String html = mockMvc.perform(get("/i/inviter2").with(TestAuth.member(guest))).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("함께 시작하기").contains("글친구님을 팔로우해요").doesNotContain("Google로 시작하기");

        MvcResult result = mockMvc.perform(post("/i/inviter2").with(csrf()).with(TestAuth.member(guest))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(303);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/inviter2");
        assertThat(follows(guest, inviter)).isTrue();

        // 내 링크를 내가 열면 팔로우 버튼 대신 안내
        String mine = mockMvc.perform(get("/i/inviter2").with(TestAuth.member(inviter))).andReturn().getResponse().getContentAsString();
        assertThat(mine).contains("내 초대 링크예요").doesNotContain("함께 시작하기");
    }

    @Test
    void unknownHandleOrCodeIs404() throws Exception {
        long inviter = members.localMember("inviter3", "세번째", "inviter3@example.com", PASSWORD, true);
        assertThat(mockMvc.perform(get("/i/nobody_here")).andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mockMvc.perform(get("/i/inviter3/wrongcode")).andReturn().getResponse().getStatus()).isEqualTo(404);
        // 남의 방 코드에 내 아이디를 붙인 링크는 없는 링크(초대한 사람이 그 방 멤버일 때만)
        long other = members.localMember("otherowner", "남의방장", "otherowner@example.com", PASSWORD, true);
        long club = createLinkClub(other, "남의 방");
        assertThat(mockMvc.perform(get("/i/inviter3/" + code(club))).andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mockMvc.perform(get("/i/otherowner/" + code(club))).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(inviter).isPositive();
    }

    @Test
    void myInviteLinksListBlogAndOpenClubs() throws Exception {
        long me = members.localMember("linkowner", "주인", "linkowner@example.com", PASSWORD, true);
        long club = createLinkClub(me, "친구 글방");
        String html = mockMvc.perform(get("/me/invite").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("내 블로그로 초대").contains("/i/linkowner\"").contains("함께 쓰기 &#39;친구 글방&#39;로 초대")
                .contains("/i/linkowner/" + code(club));
        assertThat(mockMvc.perform(get("/me/invite")).andReturn().getResponse().getStatus()).isEqualTo(303);
    }

    @Test
    void signupWithoutHandleAndPasswordConfirmDerivesHandleFromEmail() throws Exception {
        MvcResult first = mockMvc.perform(post("/signup").with(csrf()).param("email", "friendly@example.com").param("password", PASSWORD)
                .param("nickname", "친구하나").param("ageGroup", "OVER14").param("agreeTerms", "true").param("agreePrivacy", "true")).andReturn();
        assertThat(first.getResponse().getStatus()).isEqualTo(303);
        assertThat(jdbc.queryForObject("SELECT handle FROM member WHERE nickname = '친구하나'", String.class)).isEqualTo("friendly");

        // 같은 앞부분의 다른 이메일은 겹치지 않는 주소를 받는다
        MvcResult second = mockMvc.perform(post("/signup").with(csrf()).param("email", "friendly@other.com").param("password", PASSWORD)
                .param("nickname", "친구둘").param("ageGroup", "OVER14").param("agreeTerms", "true").param("agreePrivacy", "true")).andReturn();
        assertThat(second.getResponse().getStatus()).isEqualTo(303);
        String handle = jdbc.queryForObject("SELECT handle FROM member WHERE nickname = '친구둘'", String.class);
        assertThat(handle).startsWith("friendly").isNotEqualTo("friendly");

        // 가입 화면에 주소·비밀번호 확인 칸이 없다
        String page = mockMvc.perform(get("/signup")).andReturn().getResponse().getContentAsString();
        assertThat(page).doesNotContain("name=\"handle\"").doesNotContain("name=\"passwordConfirm\"").contains("블로그 주소는 이메일 앞부분으로 만들어요");
    }
}
