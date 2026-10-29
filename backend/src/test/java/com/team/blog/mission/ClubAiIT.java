package com.team.blog.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.mission.application.ClubAiClient;
import com.team.blog.mission.application.ClubAiKeys;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 10-29 방 글감 AI 추천: 방장만 [AI 연동하기], 키는 확인한 뒤 암호화해서 저장(화면에는 끝 4자리), 후보는 지난 1년 글감과 겹치지 않게,
 * 방장이 [올리기]를 눌러야 방 글감이 된다. 방마다 하루 10번, 14세 미만 방장은 연결 못 함, 1년 안 같은 글감은 서버도 막는다.
 */
class ClubAiIT extends IntegrationTestBase {

    // 한국 시간 2026-10-16 12:00
    private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");
    private static final String KEY = "AIzaTestKey0123456789abcdWXYZ";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ClubAiKeys keys;

    @MockitoBean
    ClubAiClient client;

    private String page(String url, long who) throws Exception {
        return mockMvc.perform(get(url).with(TestAuth.member(who))).andReturn().getResponse().getContentAsString();
    }

    private MockHttpServletResponse suggest(String location, long who) throws Exception {
        return mockMvc.perform(post(location + "/ai/suggest").with(csrf()).with(TestAuth.member(who))).andReturn().getResponse();
    }

    @Test
    void ownerConnectsAiAndPicksTopicsThatDoNotRepeatWithinAYear() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("aiowner", "에이아이방장", "aiowner@example.com", "Blog#2026ok", true);
        long mate = members.localMember("aimate", "에이아이친구", "aimate@example.com", "Blog#2026ok", true);
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "마인크래프트 이야기").with(TestAuth.member(owner))
                .param("name", "마크 방").param("joinMode", "LINK")).andReturn().getResponse().getRedirectedUrl();
        long club = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id, role) VALUES (?, ?, 'ADMIN')", club, mate);
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(owner)).param("title", "내가 처음 지은 집"));

        // 방장에게만 [AI 연동하기](관리자도 안 보임: 방장 키로 돈이 나간다)
        assertThat(page(location, owner)).contains("AI 연동하기").contains("Google AI Studio 열기").contains("data-ai-guide=\"CLAUDE\"");
        assertThat(page(location, mate)).doesNotContain("room-ai-btn").doesNotContain("data-club-ai");
        mockMvc.perform(post(location + "/ai").with(csrf()).with(TestAuth.member(mate)).param("provider", "GEMINI").param("apiKey", KEY));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_ai", Integer.class)).isZero();

        // 틀린 키: 확인에서 막히고 저장하지 않는다
        doThrow(new ClubAiClient.AiException("키가 맞지 않거나 지워졌어요. 키를 다시 확인해 주세요")).when(client).check("GEMINI", "AIzaWrongKey0123456789abcd");
        mockMvc.perform(post(location + "/ai").with(csrf()).with(TestAuth.member(owner)).param("provider", "GEMINI").param("apiKey", "AIzaWrongKey0123456789abcd"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_ai", Integer.class)).isZero();

        // 연결: 암호문만 저장, 화면에는 끝 4자리만
        mockMvc.perform(post(location + "/ai").with(csrf()).with(TestAuth.member(owner)).param("provider", "GEMINI").param("apiKey", " " + KEY + " "));
        verify(client).check("GEMINI", KEY);
        String cipher = jdbc.queryForObject("SELECT key_cipher FROM writing_club_ai WHERE club_id = ?", String.class, club);
        assertThat(cipher).doesNotContain(KEY);
        assertThat(keys.decrypt(cipher)).contains(KEY);
        assertThat(page(location, owner)).contains("AI 추천받기").contains("Gemini 연결됨").contains("키 끝 WXYZ · 오늘 10번 남음").doesNotContain(KEY);

        // 추천: 질문에 방 주제와 지난 1년 글감, 같은 글감은 빠지고 비슷한 글감은 표시
        when(client.ask(eq("GEMINI"), eq(KEY), anyString())).thenReturn("""
                1. 내가 처음 지은 집|첫 집 이야기
                처음 지은 집 다시 짓기|그때와 지금을 비교해 보세요
                - 동굴에서 찾은 보물|가장 기억에 남는 발견
                이건 형식이 틀린 줄
                """);
        MockHttpServletResponse res = suggest(location, owner);
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getContentAsString()).doesNotContain("첫 집 이야기").contains("처음 지은 집 다시 짓기").contains("\"similar\":\"내가 처음 지은 집\"")
                .contains("\"similarDay\":\"10월 16일\"").contains("동굴에서 찾은 보물").contains("\"left\":9");
        verify(client).ask(eq("GEMINI"), eq(KEY), argThat(p -> p.contains("방 주제: 마인크래프트 이야기") && p.contains("- 내가 처음 지은 집")
                && p.contains("글감|한 줄 설명")));
        // 후보는 저절로 올라가지 않는다
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_topic WHERE club_id = ?", Integer.class, club)).isEqualTo(1);
        assertThat(suggest(location, mate).getStatus()).isEqualTo(404);

        // 1년 안에 같은 글감(띄어쓰기·문장부호만 다름)은 서버도 막는다. 1년이 지나면 다시 쓸 수 있다
        clock.set(NOW.plus(Duration.ofDays(30)));
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(owner)).param("title", "내가 처음 지은 집!"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_topic WHERE club_id = ?", Integer.class, club)).isEqualTo(1);
        clock.set(NOW.plus(Duration.ofDays(366)));
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(owner)).param("title", "내가 처음 지은 집"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_topic WHERE club_id = ?", Integer.class, club)).isEqualTo(2);

        // 하루 10번: 다 쓰면 막히고, 날이 바뀌면 다시
        jdbc.update("UPDATE writing_club_ai SET used_on = ?, used_count = 10 WHERE club_id = ?",
                java.sql.Date.valueOf(java.time.LocalDate.of(2027, 10, 17)), club);
        assertThat(suggest(location, owner).getContentAsString()).contains("오늘은 10번 다 받았어요");
        clock.set(NOW.plus(Duration.ofDays(367)));
        assertThat(suggest(location, owner).getStatus()).isEqualTo(200);

        // 끊기: 키도 지운다
        mockMvc.perform(post(location + "/ai/delete").with(csrf()).with(TestAuth.member(owner)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_ai", Integer.class)).isZero();
        assertThat(page(location, owner)).contains("AI 연동하기");
    }

    @Test
    void under14OwnerCannotConnect() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("aikid", "어린방장", "aikid@example.com", "Blog#2026ok", true);
        jdbc.update("INSERT INTO member_agreement (member_id, type) VALUES (?, 'GUARDIAN')", owner);
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "책").with(TestAuth.member(owner))
                .param("name", "책 방").param("joinMode", "LINK")).andReturn().getResponse().getRedirectedUrl();
        String flash = mockMvc.perform(post(location + "/ai").with(csrf()).with(TestAuth.member(owner)).param("provider", "CLAUDE").param("apiKey", KEY))
                .andReturn().getFlashMap().get("clubError").toString();
        assertThat(flash).contains("14세 미만");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_ai", Integer.class)).isZero();
    }
}
