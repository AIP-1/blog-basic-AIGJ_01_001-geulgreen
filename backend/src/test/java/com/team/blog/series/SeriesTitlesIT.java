package com.team.blog.series;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 강성찬 개인 확장(10-21): 탐색 카드의 시리즈 이름. 공개 글만 알려 주고, 탐색 화면은 이름을 붙이는 스크립트를 싣는다. */
class SeriesTitlesIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void titlesOnlyForPublicPostsInSeries() throws Exception {
        Instant t = Instant.parse("2026-10-21T03:00:00Z");
        clock.set(t);
        long me = members.localMember("stwriter", "시리즈작가", "stwriter@example.com", "Blog#2026ok", true);
        long inSeries = posts.published(me, "공부 1편", "본문", 1, t.minusSeconds(60));
        long hidden = posts.published(me, "공부 비공개", "본문", 1, t.minusSeconds(30));
        long alone = posts.published(me, "따로 쓴 글", "본문", 1, t.minusSeconds(10));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", hidden);
        long sid = jdbc.queryForObject("INSERT INTO series (member_id, title) VALUES (?, '웹 개발 기초 공부') RETURNING id", Long.class, me);
        jdbc.update("INSERT INTO series_post (post_id, series_id, position) VALUES (?, ?, 1), (?, ?, 2)", inSeries, sid, hidden, sid);

        String json = mockMvc.perform(get("/api/series/titles?ids=" + inSeries + "," + hidden + "," + alone))
                .andReturn().getResponse().getContentAsString();
        assertThat(json).isEqualTo("{\"" + inSeries + "\":\"웹 개발 기초 공부\"}");
        assertThat(mockMvc.perform(get("/api/series/titles")).andReturn().getResponse().getContentAsString()).isEqualTo("{}");
        assertThat(mockMvc.perform(get("/explore")).andReturn().getResponse().getContentAsString()).contains("/js/post/series-labels");
    }
}
