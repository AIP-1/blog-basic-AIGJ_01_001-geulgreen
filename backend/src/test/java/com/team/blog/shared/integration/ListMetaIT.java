package com.team.blog.shared.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 태그·시리즈·블로그 홈의 검색·미리보기 문구: 화면마다 따로, 공개 글만, 소개글·시리즈 설명·대표 사진. */
class ListMetaIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void dump() throws Exception {
        Instant now = Instant.parse("2026-10-18T03:00:00Z");
        clock.set(now);
        long me = members.localMember("hikerkim", "등산하는김씨", "hiker@example.com", "Blog#2026ok", true);
        jdbc.update("UPDATE member SET bio = '주말마다 산에 오르고 기록해요' WHERE id = ?", me);
        long a = posts.published(me, "북한산 백운대 코스", "본문", 1, now.minus(Duration.ofDays(1)));
        long b = posts.published(me, "가을 설악산 단풍", "본문", 1, now.minus(Duration.ofDays(2)));
        long c = posts.published(me, "비공개 산행 메모", "본문", 1, now.minus(Duration.ofDays(3)));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE', first_public_at = NULL WHERE id = ?", c);
        jdbc.update("INSERT INTO tag (name) VALUES ('등산')");
        for (long p : new long[] {a, b, c}) {
            jdbc.update("INSERT INTO post_tag (post_id, tag_id, position) SELECT ?, id, 0 FROM tag WHERE name = '등산'", p);
        }
        long s = jdbc.queryForObject("INSERT INTO series (member_id, title) VALUES (?, '서울 근교 산') RETURNING id", Long.class, me);
        jdbc.update("INSERT INTO series_post (series_id, post_id, position) VALUES (?, ?, 1), (?, ?, 2)", s, a, s, b);
        String tag = head("/tags/등산");
        assertThat(tag).contains("<title>#등산 글 모음 · 글그린</title>")
                .contains("<meta name=\"description\" content=\"글그린에서 &#39;등산&#39; 태그로 쓴 글 2편. 최근 글: 「북한산 백운대 코스」, 「가을 설악산 단풍」\">")
                .contains("<meta property=\"og:title\" content=\"#등산 글 모음 · 글그린\">").doesNotContain("비공개 산행")
                .contains("naver-site-verification").contains("google-site-verification");
        assertThat(head("/hikerkim/series/" + s)).contains("등산하는김씨님의 시리즈 「서울 근교 산」 2편: 「북한산 백운대 코스」, 「가을 설악산 단풍」")
                .contains("og-cat.jpg");
        jdbc.update("UPDATE series SET description = '서울에서 당일로 다녀오는 산', cover_url = '/media/cover.webp' WHERE id = ?", s);
        assertThat(head("/hikerkim/series/" + s)).contains("<meta name=\"description\" content=\"서울에서 당일로 다녀오는 산\">")
                .contains("/media/cover.webp\">");
        assertThat(head("/hikerkim")).contains("<title>등산하는김씨의 블로그 · 글그린</title>")
                .contains("<meta name=\"description\" content=\"주말마다 산에 오르고 기록해요\">").doesNotContain("비공개 산행");
        jdbc.update("UPDATE member SET bio = NULL WHERE id = ?", me);
        assertThat(head("/hikerkim")).contains("등산하는김씨님의 글그린 블로그. 최근 글: 「북한산 백운대 코스」, 「가을 설악산 단풍」");
    }

    private String head(String url) throws Exception {
        String html = mockMvc.perform(get(url)).andReturn().getResponse().getContentAsString();
        return html.substring(0, html.indexOf("</head>"));
    }
}
