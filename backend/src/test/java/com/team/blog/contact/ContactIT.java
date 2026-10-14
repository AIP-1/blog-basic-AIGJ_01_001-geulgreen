package com.team.blog.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.contact.application.InquiryPurgeStep;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 090 문의하기(강성찬 개인 확장): 양식·저장·횟수 제한, 관리자 알림·문의함·답장 메일. */
class ContactIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InquiryPurgeStep purge;

    @Autowired
    com.team.blog.admin.application.AdminProperties adminProperties;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private static RequestPostProcessor ip(String ip) {
        return r -> {
            r.setRemoteAddr(ip);
            return r;
        };
    }

    private static MockHttpServletRequestBuilder send(String kind, String title, String email) {
        return post("/contact").param("kind", kind).param("title", title).param("content", "로그인이 안 돼요\n도와주세요")
                .param("email", email).with(csrf());
    }

    private String page(String url, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(url).with(who)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void guestAndMemberSendAndAdminReplies() throws Exception {
        long admin = member("inqboss");
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", admin);
        long user = member("inquser");

        // 꼬리말 링크가 사이트 양식으로, 회원은 이메일이 채워지고 바꿀 수 없음
        String guestForm = mockMvc.perform(get("/contact")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(guestForm).contains("href=\"/contact\"").contains("계정·로그인").contains("답장을 보낼 때만 써요")
                .doesNotContain("readonly=\"readonly\"");
        assertThat(page("/contact", TestAuth.member(user))).contains("value=\"inquser@example.com\"").contains("readonly");

        // 잘못된 값은 양식을 다시 보여 줌
        assertThat(mockMvc.perform(send("ACCOUNT", "제목", "not-an-email").with(ip("198.51.100.1")))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString())
                .contains("답장 받을 이메일을 바르게 적어 주세요").contains("value=\"제목\"");
        mockMvc.perform(send("NOPE", "제목", "a@example.com").with(ip("198.51.100.1"))).andExpect(status().isBadRequest());

        mockMvc.perform(send("BUG", "비회원 문의", "Guest@Example.com").with(ip("198.51.100.1")))
                .andExpect(redirectedUrl("/contact/done"));
        // 회원이 보낸 다른 주소는 무시하고 계정 이메일로
        mockMvc.perform(send("ACCOUNT", "회원 문의", "other@example.com").with(TestAuth.member(user)).with(ip("198.51.100.2")))
                .andExpect(redirectedUrl("/contact/done"));
        assertThat(jdbc.queryForList("SELECT email FROM inquiry ORDER BY id", String.class))
                .containsExactly("guest@example.com", "inquser@example.com");
        assertThat(mockMvc.perform(get("/contact/done")).andReturn().getResponse().getContentAsString()).contains("문의를 보냈어요");

        // 관리자에게 알림 2개, 문의함은 관리자만
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE receiver_id = ? AND type = 'INQUIRY'",
                Integer.class, admin)).isEqualTo(2);
        assertThat(page("/api/notifications", TestAuth.member(admin))).contains("새 문의가 왔어요").contains("/admin/inquiries");
        mockMvc.perform(get("/admin/inquiries").with(TestAuth.member(user))).andExpect(status().isNotFound());
        assertThat(page("/admin/inquiries", TestAuth.admin(admin))).contains("비회원 문의").contains("회원 문의").contains("새 문의");

        long id = jdbc.queryForObject("SELECT id FROM inquiry WHERE title = '비회원 문의'", Long.class);
        mockMvc.perform(post("/admin/inquiries/{id}/reply", id).param("reply", "고쳤어요").with(csrf()).with(TestAuth.member(user)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/admin/inquiries/{id}/reply", id).param("reply", " ").with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/admin/inquiries/{id}/reply", id).param("reply", "고쳤어요. 다시 해 보세요").with(csrf())
                .with(TestAuth.admin(admin))).andExpect(redirectedUrl("/admin/inquiries/" + id));
        assertThat(jdbc.queryForObject("SELECT status FROM inquiry WHERE id = ?", String.class, id)).isEqualTo("DONE");
        assertThat(page("/admin/inquiries/" + id, TestAuth.admin(admin))).contains("처리 완료").contains("고쳤어요. 다시 해 보세요")
                .doesNotContain("답장 보내고 처리 완료");
        var mail = mailpit.awaitMessagesTo("guest@example.com", 1).get(0);
        assertThat(mail.subject()).contains("비회원 문의");
        assertThat(mail.text()).contains("고쳤어요. 다시 해 보세요").contains("로그인이 안 돼요");

        // 탈퇴 정리: 그 회원의 문의만 지움
        purge.purge(user);
        assertThat(jdbc.queryForList("SELECT title FROM inquiry", String.class)).containsExactly("비회원 문의");
    }

    /** 관리자 지정 주소(개인 주소)는 어느 화면에도 나오지 않는다. */
    @Test
    void adminEmailsNeverRendered() throws Exception {
        assertThat(adminProperties.emails()).isNotEmpty();
        long admin = member("inqpriv");
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", admin);
        for (String url : java.util.List.of("/", "/contact", "/contact/done", "/terms", "/settings", "/admin", "/admin/inquiries")) {
            String html = page(url, TestAuth.admin(admin));
            for (String e : adminProperties.emails()) {
                assertThat(html).as(url).doesNotContainIgnoringCase(e.strip());
            }
        }
        assertThat(page("/", r -> r)).doesNotContain("mailto:");
    }

    @Test
    void rateLimitedPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(send("OTHER", "문의 " + i, "spam@example.com").with(ip("203.0.113.9")))
                    .andExpect(redirectedUrl("/contact/done"));
        }
        assertThat(mockMvc.perform(send("OTHER", "여섯 번째", "spam@example.com").with(ip("203.0.113.9")))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString()).contains("한 시간 뒤에");
        mockMvc.perform(send("OTHER", "다른 IP", "spam@example.com").with(ip("203.0.113.10"))).andExpect(redirectedUrl("/contact/done"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inquiry", Integer.class)).isEqualTo(6);
    }
}
