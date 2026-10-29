package com.team.blog.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 111 후속: 방 종류(링크로만·승인 후 참여·누구나 바로), 공개 목록, 참여 요청 승인·거절, 팔로워 초대, 정원, 하루 초대 한도. */
class ClubJoinIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private long create(long owner, String name, String mode) throws Exception {
        var req = post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(owner)).param("name", name);
        if (mode != null) {
            req = req.param("joinMode", mode);
        }
        String location = mockMvc.perform(req).andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private String page(String url, long who) throws Exception {
        return mockMvc.perform(get(url).with(TestAuth.member(who))).andReturn().getResponse().getContentAsString();
    }

    private boolean member(long club, long who) {
        return jdbc.queryForObject("SELECT count(*) FROM writing_club_member WHERE club_id = ? AND member_id = ?", Integer.class, club, who) == 1;
    }

    @Test
    void approvalRoomIsListedAndOwnerApprovesOrRejects() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("roomowner", "방장", "roomowner@example.com", "Blog#2026ok", true);
        long a = members.localMember("roomguesta", "손님가", "roomguesta@example.com", "Blog#2026ok", true);
        long b = members.localMember("roomguestb", "손님나", "roomguestb@example.com", "Blog#2026ok", true);
        long club = create(owner, "공개 독서방", null);
        assertThat(jdbc.queryForObject("SELECT join_mode FROM writing_club WHERE id = ?", String.class, club)).isEqualTo("APPROVAL");
        assertThat(page("/missions/clubs/find?q=독서", a)).contains("공개 독서방").contains("승인 후 참여");
        assertThat(page("/missions/clubs/" + club, a)).contains("참여 요청");
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(a)));
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(b)));
        assertThat(member(club, a)).isFalse();
        assertThat(page("/missions/clubs/" + club, owner)).contains("참여 요청 2명");
        mockMvc.perform(post("/missions/clubs/" + club + "/requests/" + a + "/approve").with(csrf()).with(TestAuth.member(owner)));
        mockMvc.perform(post("/missions/clubs/" + club + "/requests/" + b + "/reject").with(csrf()).with(TestAuth.member(owner)));
        assertThat(member(club, a)).isTrue();
        assertThat(member(club, b)).isFalse();
        // 거절된 사람은 7일 동안 다시 못 보낸다
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(b)));
        assertThat(page("/missions/clubs/" + club, b)).contains("거절됐어요");
        // 남은 요청을 승인하는 일은 만든 사람만
        mockMvc.perform(post("/missions/clubs/" + club + "/requests/" + b + "/approve").with(csrf()).with(TestAuth.member(a)));
        assertThat(member(club, b)).isFalse();
    }

    @Test
    void openRoomJoinsAtOnceUntilCapAndShowsCountOnly() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("openowner", "열린방장", "openowner@example.com", "Blog#2026ok", true);
        long a = members.localMember("openguesta", "열린가", "openguesta@example.com", "Blog#2026ok", true);
        long b = members.localMember("openguestb", "열린나", "openguestb@example.com", "Blog#2026ok", true);
        long club = create(owner, "누구나 일기방", "OPEN");
        mockMvc.perform(post("/missions/clubs/" + club + "/settings").with(csrf()).with(TestAuth.member(owner))
                .param("joinMode", "OPEN").param("memberLimit", "true").param("memberCap", "2"));
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(a)));
        assertThat(member(club, a)).isTrue();
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(b)));
        assertThat(member(club, b)).isFalse();
        long opened = posts.published(a, "열린 글", "본문", 1, NOW.plus(Duration.ofMinutes(1)));
        mockMvc.perform(post("/posts/" + opened + "/clubs/" + club).with(csrf()).with(TestAuth.member(a)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        assertThat(page("/missions/clubs/" + club, a)).contains("오늘 <strong>1명</strong>이 썼어요").contains("열린 글");
    }

    @Test
    void linkRoomStaysHiddenAndFollowersCanBeInvited() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("linkowner", "링크방장", "linkowner@example.com", "Blog#2026ok", true);
        long fan = members.localMember("linkfan", "팬이", "linkfan@example.com", "Blog#2026ok", true);
        long stranger = members.localMember("linkstranger", "남남", "linkstranger@example.com", "Blog#2026ok", true);
        long club = create(owner, "비밀 일기방", "LINK");
        assertThat(page("/missions/clubs", stranger)).doesNotContain("비밀 일기방");
        assertThat(mockMvc.perform(get("/missions/clubs/" + club).with(TestAuth.member(stranger))).andReturn().getResponse().getStatus())
                .isEqualTo(404);
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?)", fan, owner);
        assertThat(page("/missions/clubs/" + club, owner)).contains("linkfan").doesNotContain("linkstranger");
        // 팔로워가 아닌 사람은 골라도 초대되지 않는다
        mockMvc.perform(post("/missions/clubs/" + club + "/invite").with(csrf()).with(TestAuth.member(owner))
                .param("memberIds", String.valueOf(fan), String.valueOf(stranger)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_join WHERE club_id = ? AND kind = 'INVITE'", Integer.class, club))
                .isEqualTo(1);
        assertThat(page("/missions/clubs", fan)).contains("받은 초대 1개").contains("비밀 일기방");
        assertThat(page("/missions/clubs/" + club, fan)).contains("초대 수락");
        mockMvc.perform(post("/missions/clubs/" + club + "/invite/accept").with(csrf()).with(TestAuth.member(fan)));
        assertThat(member(club, fan)).isTrue();
        assertThat(page("/notifications", fan)).contains("함께 쓰기 「비밀 일기방」에 초대했어요");
    }

    @Test
    void dailyInviteLimit() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("limitowner", "한도방장", "limitowner@example.com", "Blog#2026ok", true);
        long fan = members.localMember("limitfan", "한도팬", "limitfan@example.com", "Blog#2026ok", true);
        long club = create(owner, "한도 방", "LINK");
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?)", fan, owner);
        for (int i = 0; i < 20; i++) {
            long m = members.localMember("limitx" + i, "한도" + (char) ('가' + i), "limitx" + i + "@example.com", "Blog#2026ok", true);
            jdbc.update("INSERT INTO writing_club_join (club_id, member_id, kind, invited_by, created_at) VALUES (?, ?, 'INVITE', ?, ?)",
                    club, m, owner, java.sql.Timestamp.from(NOW));
        }
        mockMvc.perform(post("/missions/clubs/" + club + "/invite").with(csrf()).with(TestAuth.member(owner)).param("memberIds", String.valueOf(fan)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_join WHERE member_id = ?", Integer.class, fan)).isZero();
    }

    @Test
    void approvalRoomNeedsAdminEvenByLinkOrMemberInvite() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("apowner", "승인방장", "apowner@example.com", "Blog#2026ok", true);
        long helper = members.localMember("aphelper", "부방장", "aphelper@example.com", "Blog#2026ok", true);
        long plain = members.localMember("applain", "그냥멤버", "applain@example.com", "Blog#2026ok", true);
        long byLink = members.localMember("aplink", "링크손님", "aplink@example.com", "Blog#2026ok", true);
        long friendOfPlain = members.localMember("apfriend", "멤버친구", "apfriend@example.com", "Blog#2026ok", true);
        long friendOfHelper = members.localMember("aphfriend", "부방친구", "aphfriend@example.com", "Blog#2026ok", true);
        long club = create(owner, "승인 일기방", "APPROVAL");
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?), (?, ?)", club, helper, club, plain);
        String code = jdbc.queryForObject("SELECT invite_code FROM writing_club WHERE id = ?", String.class, club);
        // 링크로 와도 요청
        mockMvc.perform(post("/missions/clubs/join/" + code).with(csrf()).with(TestAuth.member(byLink)));
        assertThat(member(club, byLink)).isFalse();
        // 관리자가 아닌 사람은 승인 못 함, 만든 사람이 관리자로 정하면 됨
        mockMvc.perform(post("/missions/clubs/" + club + "/requests/" + byLink + "/approve").with(csrf()).with(TestAuth.member(helper)));
        assertThat(member(club, byLink)).isFalse();
        mockMvc.perform(post("/missions/clubs/" + club + "/members/" + helper + "/admin").with(csrf()).with(TestAuth.member(plain)));
        assertThat(page("/missions/clubs/" + club, owner)).doesNotContain("관리자 해제");
        mockMvc.perform(post("/missions/clubs/" + club + "/members/" + helper + "/admin").with(csrf()).with(TestAuth.member(owner)));
        assertThat(page("/missions/clubs/" + club, helper)).contains("참여 요청 1명").doesNotContain("club-settings");
        mockMvc.perform(post("/missions/clubs/" + club + "/requests/" + byLink + "/approve").with(csrf()).with(TestAuth.member(helper)));
        assertThat(member(club, byLink)).isTrue();
        // 일반 멤버의 초대는 수락하면 요청, 관리자의 초대는 바로
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?), (?, ?)", friendOfPlain, plain, friendOfHelper, helper);
        mockMvc.perform(post("/missions/clubs/" + club + "/invite").with(csrf()).with(TestAuth.member(plain)).param("memberIds", String.valueOf(friendOfPlain)));
        mockMvc.perform(post("/missions/clubs/" + club + "/invite").with(csrf()).with(TestAuth.member(helper)).param("memberIds", String.valueOf(friendOfHelper)));
        mockMvc.perform(post("/missions/clubs/" + club + "/invite/accept").with(csrf()).with(TestAuth.member(friendOfPlain)));
        mockMvc.perform(post("/missions/clubs/" + club + "/invite/accept").with(csrf()).with(TestAuth.member(friendOfHelper)));
        assertThat(member(club, friendOfPlain)).isFalse();
        assertThat(member(club, friendOfHelper)).isTrue();
        assertThat(jdbc.queryForObject("SELECT kind FROM writing_club_join WHERE club_id = ? AND member_id = ?", String.class, club, friendOfPlain))
                .isEqualTo("REQUEST");
    }

    @Test
    void tabsOnboardingAndWriteButtons() throws Exception {
        clock.set(NOW);
        long me = members.localMember("nexter", "다음할일", "nexter@example.com", "Blog#2026ok", true);
        long mate = members.localMember("nextmate", "짝꿍이", "nextmate@example.com", "Blog#2026ok", true);
        // 처음 화면: 한 줄 설명과 [방 만들기]·[방 찾기]. 공개방·비공개방과 세 단계는 만들기 창(dialog) 안
        assertThat(page("/missions/clubs", me)).contains("친구들과 같이 쓰면 꾸준해져요").contains("class=\"link-btn\" href=\"/missions/clubs/find\"")
                .containsPattern("(?s)<dialog id=\"club-new\".*공개방.*비공개방.*class=\"club-steps\".*</dialog>")
                .doesNotContain("반 방 만들기").doesNotContain("방 이름·주제로 찾기").contains("name=\"subject\"");
        // 만들기 폼은 csrf 칸을 직접 넣어야 한다(th:action이 아니어서)
        assertThat(page("/missions/clubs", me)).containsPattern("action=\"/missions/clubs\" class=\"club-form\">\\s*<input type=\"hidden\" name=\"_csrf\"");
        assertThat(page("/missions/clubs/find", me)).contains("아직 공개된 방이 없어요").contains("방 만들기").contains("방 이름·주제로 찾기");
        long club = create(me, "다음 할 일 방", "LINK");
        // 빈 방: 아직 아무도 안 씀. 10-28 글 쓰기 단추(위·휴대폰 아래 고정)는 이 방이 골라진 새 글
        assertThat(page("/missions/clubs/" + club, me)).contains("오늘은 아직 아무도 안 썼어요").contains("아직 올라온 글이 없어요")
                .contains("action=\"/missions/clubs/" + club + "/write\"").containsPattern("action=\"/missions/clubs/" + club + "/write\" class=\"room-write-bar\"");
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?)", club, mate);
        String write = mockMvc.perform(post("/missions/clubs/" + club + "/write").with(csrf()).with(TestAuth.member(me)))
                .andReturn().getResponse().getRedirectedUrl();
        long draft = Long.parseLong(write.substring(write.lastIndexOf('/') + 1));
        // 발행 설정의 올릴 방: 이 방이 골라져 있다
        assertThat(page(write, me)).contains("올릴 방").containsPattern("(?s)<input type=\"checkbox\" value=\"" + club + "\" checked=\"checked\">\\s*<span class=\"room-pick-text\"><strong>다음 할 일 방</strong>");
        jdbc.update("UPDATE post SET title = '오늘 쓴 글' WHERE id = ?", draft);
        posts.publish(draft, NOW.plus(Duration.ofMinutes(1)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        assertThat(page("/missions/clubs/" + club, me)).contains("오늘 <strong>1명</strong>이 썼어요").contains("오늘 쓴 글");
        assertThat(page("/missions/clubs", me)).contains("참여 중인 방").contains("다음할일: 오늘 쓴 글");
    }

    @Test
    void roomHasNoLimitByDefaultAndOwnerLimitKeepsExistingMembers() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("limitowner", "제한방장", "limitowner@example.com", "Blog#2026ok", true);
        long a = members.localMember("limitguesta", "제한가", "limitguesta@example.com", "Blog#2026ok", true);
        long b = members.localMember("limitguestb", "제한나", "limitguestb@example.com", "Blog#2026ok", true);
        long c = members.localMember("limitguestc", "제한다", "limitguestc@example.com", "Blog#2026ok", true);
        long club = create(owner, "제한 없는 방", "OPEN");
        assertThat(jdbc.queryForObject("SELECT member_cap FROM writing_club WHERE id = ?", Integer.class, club)).isNull();
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(a)));
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(b)));
        assertThat(member(club, a)).isTrue();
        assertThat(member(club, b)).isTrue();
        assertThat(page("/missions/clubs/find", c)).contains("멤버 3명").doesNotContain("멤버 3/");
        // 지금(3명)보다 적게 정해도 있는 멤버는 그대로, 새로 들어오는 것만 막힌다
        mockMvc.perform(post("/missions/clubs/" + club + "/settings").with(csrf()).with(TestAuth.member(owner))
                .param("joinMode", "OPEN").param("memberLimit", "true").param("memberCap", "2"));
        assertThat(jdbc.queryForObject("SELECT member_cap FROM writing_club WHERE id = ?", Integer.class, club)).isEqualTo(2);
        assertThat(member(club, b)).isTrue();
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(c)));
        assertThat(member(club, c)).isFalse();
        assertThat(page("/missions/clubs/find", c)).contains("멤버 3/2명");
        // 제한을 끄면 다시 들어올 수 있다
        mockMvc.perform(post("/missions/clubs/" + club + "/settings").with(csrf()).with(TestAuth.member(owner))
                .param("joinMode", "OPEN").param("memberCap", "2"));
        assertThat(jdbc.queryForObject("SELECT member_cap FROM writing_club WHERE id = ?", Integer.class, club)).isNull();
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(c)));
        assertThat(member(club, c)).isTrue();
    }

    @Test
    void ownerCanSetLimitWhenCreating() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("capmaker", "정원방장", "capmaker@example.com", "Blog#2026ok", true);
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner))
                .param("name", "다섯 명 방").param("subject", "자유 주제").param("joinMode", "APPROVAL")
                .param("memberLimit", "true").param("memberCap", "5")).andReturn().getResponse().getRedirectedUrl();
        long club = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        assertThat(jdbc.queryForObject("SELECT member_cap FROM writing_club WHERE id = ?", Integer.class, club)).isEqualTo(5);
        // 켜고 범위 밖이면 만들지 않는다
        mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner))
                .param("name", "한 명 방").param("subject", "자유 주제").param("memberLimit", "true").param("memberCap", "1"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club WHERE name = '한 명 방'", Integer.class)).isZero();
    }
}
