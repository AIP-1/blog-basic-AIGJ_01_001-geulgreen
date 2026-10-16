package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 079 탐색(강성찬 개인 확장): 비회원도 인기 태그·새로 온 작가·추천 글을 한 화면에서, 위 메뉴에 탐색 링크. */
class ExploreIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-13T03:00:00Z");

    @Test
    void guestSeesDiscoverySectionsAndMenuLink() throws Exception {
        clock.set(NOW);
        long author = members.localMember("exwriter", "탐색작가", "exwriter@example.com", "Blog#2026ok", true);
        posts.published(author, "탐색에서 보일 글", "본문", 1, NOW.minus(Duration.ofDays(1)));
        String page = mockMvc.perform(get("/explore")).andReturn().getResponse().getContentAsString();
        // 10-15 정리: 글 카드가 주인공, 사람 추천은 한 칸(추천 블로그 + 새로 온 작가), 비회원은 팔로우 대신 로그인 링크, 아이디가 주 이름(082)
        assertThat(page).contains("<h1>탐색</h1>").contains("이런 사람은 어때요").contains("탐색에서 보일 글")
                .contains("class=\"post-card\"").contains("href=\"/login\"")
                .contains("href=\"/explore\" aria-current=\"page\"")
                .contains("<span>exwriter</span>").doesNotContain("탐색작가")
                // 새로 온 작가는 "새 작가" 표시로 같은 칸에, 탭 옆 "태그 전체 보기"는 위 메뉴 태그와 겹쳐 뺌
                .contains("person-new\">새 작가").doesNotContain("explore-tags-link").doesNotContain("side-pills")
                .doesNotContain("feed-recommended").doesNotContain("newcomers-heading");
        String home = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        // 홈 오른쪽 "둘러보기" 칸은 뺌(탐색은 머리말 메뉴로)
        assertThat(home).contains("href=\"/explore\"").doesNotContain("탐색에서 더 보기").doesNotContain("side-rec-list").doesNotContain("newcomers-heading")
                .doesNotContain("side-rec-posts");
        // 추천 글은 12개씩 이어 보기(커서 = 추천 위치), 잘못된 커서는 처음부터
        for (int i = 0; i < 13; i++) {
            posts.published(author, "더 많은 글" + i, "본문", 1, NOW.minus(Duration.ofHours(i + 1)));
        }
        String api = mockMvc.perform(get("/api/explore")).andReturn().getResponse().getContentAsString();
        assertThat(api).contains("\"nextCursor\":\"12\"");
        assertThat(mockMvc.perform(get("/api/explore?cursor=12")).andReturn().getResponse().getContentAsString())
                .contains("\"nextCursor\":null");
        assertThat(mockMvc.perform(get("/explore?cursor=abc")).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString()).contains("href=\"/explore\"");
    }

    @Autowired
    private JdbcTemplate jdbc;

    private void series(long owner, String title, long... postIds) {
        long id = jdbc.queryForObject("INSERT INTO series (member_id, title) VALUES (?, ?) RETURNING id", Long.class, owner, title);
        for (int i = 0; i < postIds.length; i++) {
            jdbc.update("INSERT INTO series_post (post_id, series_id, position) VALUES (?, ?, ?)", postIds[i], id, i + 1);
        }
    }

    /** 104 강성찬 개인 확장: 탐색 시리즈 탭은 모든 블로그의 공개 시리즈를 인기순(좋아요 합)·최근순으로. */
    @Test
    void seriesTabListsPublicSeriesAcrossBlogs() throws Exception {
        clock.set(NOW);
        long a = members.localMember("sera", "시리즈가", "sera@example.com", "Blog#2026ok", true);
        long b = members.localMember("serb", "시리즈나", "serb@example.com", "Blog#2026ok", true);
        long liked = posts.published(a, "좋아요 많은 글", "본문", 1, NOW.minus(Duration.ofDays(3)));
        long fresh1 = posts.published(b, "새 글 1", "본문", 1, NOW.minus(Duration.ofHours(2)));
        long fresh2 = posts.published(b, "새 글 2", "본문", 1, NOW.minus(Duration.ofHours(1)));
        long hidden = posts.published(b, "비공개 글", "본문", 1, NOW.minus(Duration.ofMinutes(5)));
        jdbc.update("UPDATE post SET like_count = 5 WHERE id = ?", liked);
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", hidden);
        series(a, "인기 시리즈", liked);
        series(b, "최근 시리즈", fresh1, fresh2);
        series(b, "숨은 시리즈", hidden);

        String popular = mockMvc.perform(get("/explore?tab=series")).andReturn().getResponse().getContentAsString();
        assertThat(popular).contains("href=\"/explore?tab=series\" aria-current=\"page\">시리즈</a>")
                .contains("sera · 글 1편").contains("serb · 글 2편").doesNotContain("숨은 시리즈")
                .doesNotContain("class=\"post-card\"").doesNotContain("이런 사람은 어때요");
        assertThat(popular.indexOf("인기 시리즈")).isLessThan(popular.indexOf("최근 시리즈"));
        String recent = mockMvc.perform(get("/explore?tab=series&sort=recent")).andReturn().getResponse().getContentAsString();
        assertThat(recent.indexOf("최근 시리즈")).isLessThan(recent.indexOf("인기 시리즈"));
        assertThat(recent).contains("href=\"/serb/series/");
    }
}
