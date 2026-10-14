package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.discovery.application.SearchSuggestService;
import com.team.blog.discovery.application.SearchTerms;
import com.team.blog.support.IntegrationTestBase;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/** 094 강성찬 개인 확장: 오타 교정·비슷한 글. 캐시는 매번 새로 읽게 한다. */
@TestPropertySource(properties = "blog.search.suggest-cache-ttl=0s")
class SearchSuggestIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-01T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SearchSuggestService suggest;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private long post(long author, String title, String body, int minutes) {
        return posts.published(author, title, body, 1, T.plusSeconds(60L * minutes));
    }

    private String page(String q) throws Exception {
        return mockMvc.perform(get("/search").param("q", q)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void typoGetsCorrectionAndCorrectedResults() throws Exception {
        long a = member("sgauthor");
        long tx = post(a, "트랜잭션 격리 수준 정리", "본문", 1);
        post(a, "스프링 부트 시작하기", "본문", 2);
        assertThat(suggest.correct(SearchTerms.parse("트랜잭선"), null)).contains("트랜잭션");
        assertThat(suggest.correct(SearchTerms.parse("스프링부트"), null)).contains("스프링 부트");
        assertThat(suggest.correct(SearchTerms.parse("스프링 부투"), null)).contains("스프링 부트");
        assertThat(suggest.correct(SearchTerms.parse("sgauthr"), null)).contains("sgauthor");
        assertThat(suggest.correct(SearchTerms.parse("완전히무관한말"), null)).isEmpty();
        String html = page("트랜잭선");
        assertThat(html).contains("혹시").contains("&#39;트랜잭션&#39;").contains("찾으셨나요?")
                .contains("/posts/" + tx).contains("검색 결과");
        assertThat(page("트랜잭션")).doesNotContain("찾으셨나요?");
    }

    @Test
    void similarPostsOnlyFromPublicPostsAndNotAlreadyShown() throws Exception {
        long a = member("sgsim");
        long title = post(a, "레디스 캐시 전략", "본문", 1);
        long body = post(a, "운영 메모", "세션은 레디스에 두었다", 2);
        long hidden = post(a, "레디스 비공개 글", "본문", 3);
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", hidden);
        List<Long> ids = suggest.similar(List.of("레디스"), null, Set.of(), 4);
        assertThat(ids).containsExactly(title, body);
        assertThat(suggest.similar(List.of("레디스"), null, Set.of(title), 4)).containsExactly(body);
        assertThat(suggest.similar(List.of("레디즈"), null, Set.of(), 4)).contains(title);
        assertThat(page("레디즈 운영")).contains("비슷한 글").doesNotContain("/posts/" + hidden);
    }
}
