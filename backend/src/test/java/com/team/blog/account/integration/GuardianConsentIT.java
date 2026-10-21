package com.team.blog.account.integration;

import static com.team.blog.account.integration.AuthTestSupport.PASSWORD;
import static com.team.blog.account.integration.AuthTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.account.application.GuardianConsentService;
import com.team.blog.support.Browser;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.MailpitClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 10-21 chan: 14세 미만 가입 — 보호자 이름·이메일, 동의 메일, 동의 전 로그인·블로그 막기, 동의·거절·기한 지나면 지우기. */
class GuardianConsentIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    GuardianConsentService guardianConsent;

    private static MockHttpServletRequestBuilder kid(String email, String handle, String guardianName, String guardianEmail) {
        MockHttpServletRequestBuilder b = post("/signup").with(csrf()).param("email", email).param("handle", handle)
                .param("password", PASSWORD).param("passwordConfirm", PASSWORD).param("nickname", "초등학생")
                .param("agreeTerms", "true").param("agreePrivacy", "true").param("ageGroup", "UNDER14");
        if (guardianName != null) {
            b.param("guardianName", guardianName).param("guardianEmail", guardianEmail);
        }
        return b;
    }

    private String token(String guardianEmail) {
        MailpitClient.Mail mail = mailpit.awaitMessagesTo(guardianEmail, 1).get(0);
        assertThat(mail.subject()).contains("보호자 동의가 필요해요");
        assertThat(mail.body()).contains("모으는 정보").contains("보관 기간").contains("쓰는 곳");
        assertThat(mail.fromName()).isEqualTo("글그린");
        return mail.token("/auth/guardian/consent").orElseThrow();
    }

    @Test
    void guardianMustConsentBeforeLogin() throws Exception {
        // 보호자 정보가 없으면 가입을 받지 않는다
        String noGuardian = mockMvc.perform(kid("kid1@x.com", "kidone", null, null)).andReturn().getResponse().getContentAsString();
        assertThat(noGuardian).contains("보호자 이름을 적어 주세요");
        assertThat(mockMvc.perform(kid("kid1@x.com", "kidone", "엄마", "kid1@x.com")).andReturn().getResponse().getContentAsString())
                .contains("보호자 이메일은 내 이메일과 달라야 해요");
        assertThat(mockMvc.perform(kid("kid1@x.com", "kidone", "엄마", "mom@x.com")).andReturn().getResponse().getRedirectedUrl())
                .isEqualTo("/signup/guardian-sent");
        long id = jdbc.queryForObject("SELECT id FROM member WHERE handle = 'kidone'", Long.class);
        assertThat(jdbc.queryForList("SELECT type FROM member_agreement WHERE member_id = ?", String.class, id))
                .containsExactlyInAnyOrder("TERMS", "PRIVACY");
        // 동의 전: 로그인·블로그 막힘
        jdbc.update("UPDATE auth_identity SET email_verified_at = now() WHERE member_id = ?", id);
        Browser browser = new Browser(mockMvc);
        assertThat(browser.perform(login("kid1@x.com", PASSWORD)).getResponse().getHeader("Location")).isEqualTo("/login?guardian");
        assertThat(browser.perform(get("/login?guardian")).getResponse().getContentAsString()).contains("보호자 동의를 기다리고 있어요");
        assertThat(mockMvc.perform(get("/kidone")).andReturn().getResponse().getStatus()).isEqualTo(404);
        // 보호자가 메일 링크에서 동의
        String token = token("mom@x.com");
        assertThat(mockMvc.perform(get("/auth/guardian/consent").param("token", token)).andReturn().getResponse().getContentAsString())
                .contains("엄마님, 초등학생님의 가입에 동의해 주세요").contains("동의해요")
                .containsPattern("action=\"/auth/guardian/consent\">\\s*<input type=\"hidden\" name=\"_csrf\"");
        assertThat(mockMvc.perform(post("/auth/guardian/consent").with(csrf()).param("token", token).param("answer", "agree"))
                .andReturn().getResponse().getContentAsString()).contains("동의해 주셔서 고마워요");
        assertThat(jdbc.queryForList("SELECT type FROM member_agreement WHERE member_id = ?", String.class, id)).contains("GUARDIAN");
        assertThat(new Browser(mockMvc).perform(login("kid1@x.com", PASSWORD)).getResponse().getHeader("Location")).isEqualTo("/");
        assertThat(mockMvc.perform(get("/kidone")).andReturn().getResponse().getStatus()).isEqualTo(200);
        // 같은 링크는 다시 못 씀
        assertThat(mockMvc.perform(get("/auth/guardian/consent").param("token", token)).andReturn().getResponse().getContentAsString())
                .contains("링크를 쓸 수 없어요");
    }

    @Test
    void declineOrExpiryErasesTheAccount() throws Exception {
        mockMvc.perform(kid("kid2@x.com", "kidtwo", "아빠", "dad@x.com"));
        long two = jdbc.queryForObject("SELECT id FROM member WHERE handle = 'kidtwo'", Long.class);
        mockMvc.perform(post("/auth/guardian/consent").with(csrf()).param("token", token("dad@x.com")).param("answer", "decline"));
        assertErased(two);

        mockMvc.perform(kid("kid3@x.com", "kidthree", "할머니", "grandma@x.com"));
        long three = jdbc.queryForObject("SELECT id FROM member WHERE handle = 'kidthree'", Long.class);
        clock.set(clock.instant().plus(GuardianConsentService.TTL).plus(Duration.ofMinutes(1)));
        assertThat(guardianConsent.purgeExpired()).isEqualTo(1);
        assertErased(three);
    }

    private void assertErased(long id) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guardian_consent WHERE member_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_identity WHERE member_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForMap("SELECT nickname, deleted_at FROM member WHERE id = ?", id))
                .containsEntry("nickname", null).doesNotContainEntry("deleted_at", null);
    }
}
