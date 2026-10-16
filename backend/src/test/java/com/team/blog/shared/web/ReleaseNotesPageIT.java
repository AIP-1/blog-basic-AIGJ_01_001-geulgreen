package com.team.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/** 071 릴리스 노트(강성찬 개인 확장): CHANGELOG.md를 화면으로, 모든 화면 꼬리말에서 링크. 070 가입 화면에 "모두 동의". */
class ReleaseNotesPageIT extends IntegrationTestBase {

    @Test
    void releasesPageRendersChangelogAndFooterLinksIt() throws Exception {
        String html = mockMvc.perform(get("/releases")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<h1>릴리스 노트</h1>").contains("[1.0.0]")
                .contains("025~036").doesNotContain("<del>");
        // 전/후 사진(GitHub Release 첨부)은 우리 주소의 <img>로, 외부 이미지 대체글([이미지])로 바뀌지 않는다
        assertThat(html).contains("<img src=\"/releases/img/v1.59.0/v1590-home-before.png\"")
                .doesNotContain("[이미지] https://github.com/practicech/blog/releases/download/");
        assertThat(mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString()).contains("href=\"/releases\"");
    }

    @Test
    void releaseImageOnlyAcceptsTagAndFileShapes() throws Exception {
        mockMvc.perform(get("/releases/img/latest/x.png")).andExpect(status().isNotFound());
        mockMvc.perform(get("/releases/img/v1.0.0/x.gif")).andExpect(status().isNotFound());
    }

    @Test
    void signupPageGroupsAgreementsUnderAgreeAll() throws Exception {
        String html = mockMvc.perform(get("/signup")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-agree-all").contains("name=\"agreeTerms\"").contains("name=\"agreePrivacy\"");
    }
}
