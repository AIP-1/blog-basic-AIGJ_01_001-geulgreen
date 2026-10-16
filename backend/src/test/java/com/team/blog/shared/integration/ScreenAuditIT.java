package com.team.blog.shared.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;

/** 사이트 점검(10-14) 후속: 오류 화면 머리말, 빈 방명록, 기본 SEO 메타. */
class ScreenAuditIT extends IntegrationTestBase {

    @Test
    void notFoundPageKeepsLoggedInHeader() throws Exception {
        long me = members.localMember("auditme", "점검작가", "auditme@example.com", "Blog#2026ok", true);
        String html = mockMvc.perform(get("/no-such-page-here").with(TestAuth.member(me)))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("볼 수 없는 페이지예요").contains("class=\"profile-menu\"").doesNotContain("href=\"/signup\"");
    }

    @Test
    void emptyGuestbookHasNoEmptyListAndPagesHaveDefaultMeta() throws Exception {
        members.localMember("auditgb", "방명록주인", "auditgb@example.com", "Blog#2026ok", true);
        String html = mockMvc.perform(get("/auditgb/guestbook")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("아직 방명록이 없어요").doesNotContain("class=\"note-list\"");
        String terms = mockMvc.perform(get("/terms")).andReturn().getResponse().getContentAsString();
        assertThat(terms).contains("<meta name=\"description\"").contains("<meta property=\"og:title\" content=\"글그린\">")
                .contains("/terms\">").contains("og-default.png").contains("naver-site-verification");
    }

    @Test
    void homeHasSearchTitleSiteNameAndWebSiteData() throws Exception {
        String home = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(home).contains("<title>글그린 블로그 - 매일 쓴 글이 잔디처럼 자라는 곳</title>")
                .contains("<meta property=\"og:site_name\" content=\"글그린\">")
                .contains("<meta property=\"og:title\" content=\"글그린 블로그 - 매일 쓴 글이 잔디처럼 자라는 곳\">")
                .contains("매일 쓴 글이 잔디처럼 쌓이는 블로그예요")
                .contains("\"@type\":\"WebSite\",\"name\":\"글그린\",\"alternateName\":\"글그린 블로그\"")
                .contains("naver-site-verification").contains("google-site-verification");
        String terms = mockMvc.perform(get("/terms")).andReturn().getResponse().getContentAsString();
        assertThat(terms).contains("og:site_name").doesNotContain("application/ld+json").doesNotContain("잔디처럼 쌓이는");
    }
}
