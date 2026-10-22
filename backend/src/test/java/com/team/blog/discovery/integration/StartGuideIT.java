package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 강성찬 개인 확장(10-16): 처음 온 사람 안내. 홈 소개 한 줄은 누구에게나, "시작하기 3단계"는 로그인했고 발행 글이 없을 때만. */
class StartGuideIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void guideShowsUntilFirstPublishedPost() throws Exception {
        clock.set(Instant.parse("2026-10-16T03:00:00Z"));
        long me = members.localMember("sgnew", "새사람", "sgnew@example.com", "Blog#2026ok", true);
        String guest = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(guest).contains("쓴 날이 블로그에 잔디처럼 쌓여요").contains("오늘의 글감 보기").doesNotContain("글그린 시작하기")
                .contains("href=\"/signup\" class=\"btn btn-primary btn-sm\">가입하고 첫 글 쓰기");

        String fresh = mockMvc.perform(get("/").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        assertThat(fresh).contains("글그린 시작하기").contains("프로필 꾸미기").contains("오늘의 글감 보기").contains("첫 글 쓰기")
                .doesNotContain("가입하고 첫 글 쓰기").doesNotContain("header-signup");

        // 임시 글·휴지통 글만 있으면 그대로, 발행하면 사라짐
        posts.draft(me, "임시", "본문", 0);
        assertThat(mockMvc.perform(get("/").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString()).contains("글그린 시작하기");
        long first = posts.published(me, "첫 글", "본문", 1, Instant.parse("2026-10-16T02:00:00Z"));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", first);
        assertThat(mockMvc.perform(get("/").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString())
                .doesNotContain("글그린 시작하기");
    }
}
