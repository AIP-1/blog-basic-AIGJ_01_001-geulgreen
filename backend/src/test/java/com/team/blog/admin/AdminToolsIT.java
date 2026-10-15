package com.team.blog.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.admin.application.AdminBootstrap;
import com.team.blog.admin.application.AdminProperties;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 045~050 관리자 화면 확장(강성찬 개인 확장). */
class AdminToolsIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-12T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private String page(String url, org.springframework.test.web.servlet.request.RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(url).with(who)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void onlyAdminsSeeTools() throws Exception {
        long user = member("plainuser");
        long admin = member("boss");
        for (String url : List.of("/admin", "/admin/content", "/admin/guestbook", "/admin/notices", "/admin/logs")) {
            mockMvc.perform(get(url).with(TestAuth.member(user))).andExpect(status().isNotFound());
            mockMvc.perform(get(url).with(TestAuth.admin(admin))).andExpect(status().isOk());
        }
        mockMvc.perform(post("/admin/notices").param("message", "몰래 공지").with(csrf()).with(TestAuth.member(user)))
                .andExpect(status().isNotFound());
        posts.published(user, "대시보드 글", "본문", 1, clock.instant().minusSeconds(60));
        String dash = page("/admin", TestAuth.admin(admin));
        assertThat(dash).contains("관리 대시보드").contains("전체 회원").contains("처리할 신고·문의").contains("최근 30일 추이")
                .contains("기록이 쌓이는 중이에요").contains("대시보드 글");
    }

    @Test
    void insightsDrawThirtyDayTrendsAndPopularPosts() throws Exception {
        long user = member("viewwriter");
        long admin = member("boss9");
        long postId = posts.published(user, "많이 본 글", "본문", 1, clock.instant().minusSeconds(60));
        java.time.LocalDate today = java.time.LocalDate.ofInstant(clock.instant(), java.time.ZoneId.of("Asia/Seoul"));
        jdbc.update("INSERT INTO post_view_daily (post_id, view_date, views) VALUES (?, ?, 7), (?, ?, 3)",
                postId, java.sql.Date.valueOf(today), postId, java.sql.Date.valueOf(today.minusDays(40)));
        String dash = page("/admin", TestAuth.admin(admin));
        String day = today.getMonthValue() + "/" + today.getDayOfMonth();
        assertThat(dash).contains("오늘 조회").contains("class=\"trend-line\"").contains(day + " 조회 7회").contains(day + " · 7회")
                .contains("많이 본 글").contains("@viewwriter").contains("어제 이 시각 추정").doesNotContain("표로 보기");
    }

    @Test
    void systemPageShowsServersIntegrationsAndSwitchesToAdminsOnly() throws Exception {
        long user = member("plainuser2");
        long admin = member("boss10");
        mockMvc.perform(get("/admin/system").with(TestAuth.member(user))).andExpect(status().isNotFound());
        String page = page("/admin/system", TestAuth.admin(admin));
        assertThat(page).contains("앱 서버").contains("지금 답한 서버").contains("외부 연동").contains("Gemini 태그 추천")
                .contains("기능 스위치").contains("admin.system").contains("contact");
    }

    @Test
    void adminListsPageTwentyRowsAndKeepFiltersInPageLinks() throws Exception {
        long admin = member("boss11");
        for (int i = 1; i <= 25; i++) {
            jdbc.update("INSERT INTO site_notice (message, active, created_by) VALUES (?, false, ?)", "점검 공지 " + i, admin);
        }
        String first = page("/admin/notices?q=점검", TestAuth.admin(admin));
        assertThat(first).contains("1 / 2쪽").contains("점검 공지 25").doesNotContain("점검 공지 5<").contains("page=2");
        String second = page("/admin/notices?q=점검&page=2", TestAuth.admin(admin));
        assertThat(second).contains("2 / 2쪽").contains("점검 공지 5<").doesNotContain("점검 공지 25");
        assertThat(page("/admin/notices?q=없는말", TestAuth.admin(admin))).contains("조건에 맞는 공지가 없어요");
    }

    @Test
    void hideAndUnhideWithoutReportIsLogged() throws Exception {
        long user = member("writer1");
        long admin = member("boss2");
        long postId = posts.published(user, "문제 있는 글", "본문", 1, T);
        mockMvc.perform(post("/admin/posts/{id}/hide", postId).param("reason", "SPAM").with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT hidden_reason FROM post WHERE id = ?", String.class, postId)).isEqualTo("SPAM");
        mockMvc.perform(get("/writer1/posts/{id}", postId)).andExpect(status().isNotFound());
        mockMvc.perform(post("/admin/posts/{id}/unhide", postId).with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/writer1/posts/{id}", postId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).with(TestAuth.member(user))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"content\":\"광고 댓글\"}"))
                .andExpect(status().isCreated());
        long commentId = jdbc.queryForObject("SELECT id FROM comment", Long.class);
        mockMvc.perform(post("/admin/comments/{id}/hide", commentId).param("reason", "SPAM").with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT comment_count FROM post WHERE id = ?", Integer.class, postId)).isZero();
        mockMvc.perform(post("/admin/comments/{id}/unhide", commentId).with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT comment_count FROM post WHERE id = ?", Integer.class, postId)).isEqualTo(1);
        String logs = page("/admin/logs", TestAuth.admin(admin));
        assertThat(logs).contains("POST_HIDE").contains("POST_UNHIDE").contains("COMMENT_HIDE").contains("boss2");
        assertThat(page("/admin/content?q=문제", TestAuth.admin(admin))).contains("문제 있는 글");
        assertThat(page("/admin/members?q=writer1@example.com", TestAuth.admin(admin))).contains("@writer1").contains("writer1@example.com");
    }

    @Test
    void noticeShowsOnEveryPageAndGuestbookCanBeCleaned() throws Exception {
        long admin = member("boss3");
        long guest = member("guest3");
        long owner = member("owner3");
        mockMvc.perform(post("/admin/notices").param("message", "오늘 밤 점검해요").param("linkUrl", "javascript:alert(1)")
                .with(csrf()).with(TestAuth.admin(admin))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/admin/notices").param("message", "오늘 밤 점검해요").param("linkUrl", "/boss3")
                .with(csrf()).with(TestAuth.admin(admin))).andExpect(status().is3xxRedirection());
        assertThat(mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString()).contains("📢 오늘 밤 점검해요");
        long noticeId = jdbc.queryForObject("SELECT id FROM site_notice WHERE active", Long.class);
        mockMvc.perform(post("/admin/notices/{id}/off", noticeId).with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        assertThat(mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString()).doesNotContain("오늘 밤 점검해요");
        mockMvc.perform(post("/owner3/guestbook").param("content", "도배 방명록").with(csrf()).with(TestAuth.member(guest)))
                .andExpect(status().is3xxRedirection());
        assertThat(page("/admin/guestbook", TestAuth.admin(admin))).contains("도배 방명록");
        long entry = jdbc.queryForObject("SELECT id FROM guestbook_entry", Long.class);
        mockMvc.perform(post("/admin/guestbook/{id}/delete", entry).with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guestbook_entry WHERE owner_id = ?", Integer.class, owner)).isZero();
    }

    @Test
    void bootstrapPromotesVerifiedConfiguredEmailOnly() {
        long verified = members.localMember("chanadmin", "찬찬", "Chan.Admin@example.com".toLowerCase(), "Blog#2026ok", true);
        long unverified = members.localMember("notyet", "아직", "notyet@example.com", "Blog#2026ok", false);
        AdminBootstrap bootstrap = new AdminBootstrap(jdbc,
                new AdminProperties(List.of(" CHAN.ADMIN@example.com ", "notyet@example.com"), true));
        assertThat(bootstrap.promote()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT role FROM member WHERE id = ?", String.class, verified)).isEqualTo("ADMIN");
        assertThat(jdbc.queryForObject("SELECT role FROM member WHERE id = ?", String.class, unverified)).isEqualTo("USER");
        assertThat(bootstrap.promote()).isZero();
    }
}
