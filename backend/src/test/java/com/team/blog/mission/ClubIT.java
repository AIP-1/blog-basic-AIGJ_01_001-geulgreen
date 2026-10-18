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

/** 111 후보 모임: 만들기, 초대 링크로만 참여, 멤버만 봄, 기간 안 공개 글이 "썼어요", 나가기·내보내기·삭제. */
class ClubIT extends IntegrationTestBase {

    // 한국 시간 2026-10-16 12:00
    private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void inviteOnlyClubCountsPublicPostsInPeriod() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("clubowner", "모임장", "clubowner@example.com", "Blog#2026ok", true);
        long friend = members.localMember("clubfriend", "반친구", "clubfriend@example.com", "Blog#2026ok", true);
        long stranger = members.localMember("clubstranger", "남남", "clubstranger@example.com", "Blog#2026ok", true);
        // 이름이 비면 만들지 않고 만들기 화면으로
        assertThat(mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner))
                .param("name", " ").param("everyDays", "7").param("startDate", "2026-10-12").param("endDate", "2026-11-08"))
                .andReturn().getResponse().getRedirectedUrl()).isEqualTo("/missions/clubs");
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner))
                .param("name", "3반 모임").param("intro", "같이 써요").param("everyDays", "7")
                .param("startDate", "2026-10-12").param("endDate", "2026-11-08")).andReturn().getResponse().getRedirectedUrl();
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        String code = jdbc.queryForObject("SELECT invite_code FROM writing_club WHERE id = ?", String.class, id);
        // 멤버가 아니면 없는 모임
        assertThat(mockMvc.perform(get(location).with(TestAuth.member(stranger))).andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mockMvc.perform(get("/missions/clubs/join/" + code).with(TestAuth.member(friend))).andReturn().getResponse()
                .getContentAsString()).contains("3반 모임").contains("참여하기");
        mockMvc.perform(post("/missions/clubs/join/" + code).with(csrf()).with(TestAuth.member(friend)));
        // 이번 기간(10-12~10-18)에 친구만 공개 글, 모임장은 기간 전 글만
        posts.published(friend, "이번 주 글", "본문", 1, NOW.minus(Duration.ofHours(2)));
        posts.published(owner, "지난주 글", "본문", 1, NOW.minus(Duration.ofDays(6)));
        String board = mockMvc.perform(get(location).with(TestAuth.member(friend))).andReturn().getResponse().getContentAsString();
        assertThat(board).contains("<details class=\"club-members\" open").contains("1/2명 썼어요").contains("1/4번째</span> · <span class=\"meta-part\">10월 12일 ~ 18일").contains("이번 주 글")
                .doesNotContain("지난주 글").doesNotContain("club-count is-done").contains("/missions/clubs/join/" + code).contains("함께 쓰기에서 나가기").doesNotContain("함께 쓰기 삭제");
        assertThat(mockMvc.perform(get("/missions/clubs").with(TestAuth.member(friend))).andReturn().getResponse().getContentAsString())
                .contains("3반 모임").contains("aria-current=\"page\">함께 쓰기</a>").contains("club-state is-done").contains("멤버 2명");
        posts.published(owner, "모임장 이번 주 글", "본문", 1, NOW.minus(Duration.ofHours(1)));
        assertThat(mockMvc.perform(get(location).with(TestAuth.member(owner))).andReturn().getResponse().getContentAsString())
                .contains("club-count is-done").contains("함께 쓰기 삭제").contains("내보내기");
        // 만든 사람: 내보내기, 삭제
        mockMvc.perform(post(location + "/members/" + friend + "/remove").with(csrf()).with(TestAuth.member(owner)));
        assertThat(mockMvc.perform(get(location).with(TestAuth.member(friend))).andReturn().getResponse().getStatus()).isEqualTo(404);
        // 남이 지우려 하면 404, 모임은 남는다
        mockMvc.perform(post(location + "/delete").with(csrf()).with(TestAuth.member(stranger)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club", Integer.class)).isEqualTo(1);
        mockMvc.perform(post(location + "/delete").with(csrf()).with(TestAuth.member(owner)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_member", Integer.class)).isZero();
    }
}
