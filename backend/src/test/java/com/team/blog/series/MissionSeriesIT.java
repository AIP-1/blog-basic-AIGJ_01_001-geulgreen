package com.team.blog.series;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.mission.application.MissionService;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** 113 강성찬 개인 확장: 오늘의 미션 글을 발행하면 글쓴이의 '오늘의 미션' 시리즈 끝에 들어간다. 글 화면에는 상자가 없다. */
class MissionSeriesIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-12T03:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MissionService missions;

    private long startWriting(long who, long topicId) throws Exception {
        String location = mockMvc.perform(post("/missions/topics/{id}/write", topicId).with(csrf()).with(TestAuth.member(who)))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private void publish(long who, long postId) throws Exception {
        mockMvc.perform(post("/api/posts/{id}/publish", postId).with(csrf()).with(TestAuth.member(who))
                .header("Idempotency-Key", "ms-" + postId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"미션 글 " + postId + "\",\"contentMd\":\"미션 글 본문이에요\",\"tags\":[],\"visibility\":\"PUBLIC\",\"baseVersion\":0}"))
                .andExpect(status().isOk());
    }

    private String page(String url) throws Exception {
        return mockMvc.perform(get(url)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void missionPostsGoIntoMissionSeries() throws Exception {
        clock.set(T);
        long me = members.localMember("msseries", "미션러", "msseries@example.com", "Blog#2026ok", true);
        missions.topics(LocalDate.parse("2026-10-12"));
        var topics = missions.topics(LocalDate.parse("2026-10-12"));
        long first = startWriting(me, topics.get(0).id());
        publish(me, first);
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ? AND mission", Long.class, me);
        assertThat(jdbc.queryForObject("SELECT title FROM series WHERE id = ?", String.class, sid)).isEqualTo("오늘의 미션");
        // 이름을 바꿔도 다음 미션 글은 같은 시리즈 끝에
        jdbc.update("UPDATE series SET title = '매일 쓰기' WHERE id = ?", sid);
        long second = startWriting(me, topics.get(1).id());
        publish(me, second);
        assertThat(jdbc.queryForList("SELECT post_id FROM series_post WHERE series_id = ? ORDER BY position", Long.class, sid))
                .containsExactly(first, second);
        // 이미 다른 시리즈에 든 미션 글은 그대로
        long third = startWriting(me, topics.get(2).id());
        jdbc.update("INSERT INTO series (member_id, title) VALUES (?, '내 연재')", me);
        long mine = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ? AND title = '내 연재'", Long.class, me);
        jdbc.update("INSERT INTO series_post (post_id, series_id, position) VALUES (?, ?, 1)", third, mine);
        publish(me, third);
        assertThat(jdbc.queryForObject("SELECT series_id FROM series_post WHERE post_id = ?", Long.class, third)).isEqualTo(mine);
        // 미션이 아닌 글은 넣지 않는다
        long plain = posts.draft(me, "그냥 글", "본문", 0);
        publish(me, plain);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_post WHERE post_id = ?", Integer.class, plain)).isZero();
        // 글 화면에는 미션 시리즈 상자가 없고, 시리즈 화면·시리즈 탭에는 보인다
        assertThat(page("/msseries/posts/" + second)).doesNotContain("series-box");
        assertThat(page("/msseries/posts/" + third)).contains("series-box");
        assertThat(page("/msseries/series")).contains("매일 쓰기");
        assertThat(page("/msseries/series/" + sid)).contains("미션 글 " + first).contains("미션 글 " + second);
    }
}
