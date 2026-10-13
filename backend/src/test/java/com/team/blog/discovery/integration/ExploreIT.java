package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 079 탐색(강성찬 개인 확장): 비회원도 인기 태그·새로 온 작가·추천 글을 한 화면에서, 위 메뉴에 탐색 링크. */
class ExploreIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-13T03:00:00Z");

    @Test
    void guestSeesDiscoverySectionsAndMenuLink() throws Exception {
        clock.set(NOW);
        long author = members.localMember("exwriter", "탐색작가", "exwriter@example.com", "Blog#2026ok", true);
        posts.published(author, "탐색에서 보일 글", "본문", 1, NOW.minus(Duration.ofDays(1)));
        String page = mockMvc.perform(get("/explore")).andReturn().getResponse().getContentAsString();
        assertThat(page).contains("<h1>탐색</h1>").contains("새로 온 작가").contains("추천 글").contains("탐색에서 보일 글")
                .contains("href=\"/explore\" aria-current=\"page\"");
        assertThat(mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString()).contains("href=\"/explore\"");
    }
}
