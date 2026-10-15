package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 036 홈 "새로 온 작가"(강성찬 개인 확장): 최근 14일 안에 첫 공개 글을 올린 사람과 그 첫 글. */
class NewcomerIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-09T03:00:00Z");

    @Test
    void showsOnlyMembersWhoseFirstPublicPostIsRecent() throws Exception {
        clock.set(NOW);
        long fresh = members.localMember("ncfresh", "새작가", "ncfresh@example.com", "Blog#2026ok", true);
        long old = members.localMember("ncold", "오랜작가", "ncold@example.com", "Blog#2026ok", true);
        posts.published(fresh, "반가워요 첫 글", "본문", 1, NOW.minus(Duration.ofDays(3)));
        posts.published(fresh, "두 번째 글", "본문", 1, NOW.minus(Duration.ofDays(1)));
        posts.published(old, "한 달 전 첫 글", "본문", 1, NOW.minus(Duration.ofDays(30)));
        posts.published(old, "어제 글", "본문", 1, NOW.minus(Duration.ofDays(1)));
        String home = mockMvc.perform(get("/explore")).andReturn().getResponse().getContentAsString();
        // 10-15: 탐색 사람 추천 칸에 "새 작가" 표시(첫 공개 글이 최근 14일 안인 사람만)
        String people = home.substring(home.indexOf("class=\"explore-people\""), home.indexOf("</aside>"));
        assertThat(people).contains(">ncfresh</span>");
        assertThat(people.split("person-new", -1)).hasSize(2);
        assertThat(people.indexOf(">ncfresh</span>")).isLessThan(people.indexOf("person-new"));
        // 086: 새로 온 작가는 탐색 [추천] 첫 화면에만(홈·트렌딩 탭에는 없음)
        assertThat(mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString()).doesNotContain("newcomers-heading");
        assertThat(mockMvc.perform(get("/explore").param("tab", "trending")).andReturn().getResponse().getContentAsString())
                .doesNotContain("newcomers-heading");
    }
}
