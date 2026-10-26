package com.team.blog.account.integration;

import static com.team.blog.account.integration.AuthTestSupport.PASSWORD;
import static com.team.blog.account.integration.AuthTestSupport.signup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.account.application.MailSender;
import com.team.blog.support.Browser;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.MailpitClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

/** 001 T121: 이메일 가입(US1, FR-005~FR-008, FR-012, FR-013). */
class EmailSignupIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    MailSender mailSender;

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void signupCreatesUnverifiedLocalAccountLogsInAndSendsVerificationMailAfterCommit() throws Exception {
        Browser browser = new Browser(mockMvc);
        browser.perform(get("/signup"));
        String before = browser.sessionCookieValue();

        MvcResult result = browser.perform(signup("  Kim.Min@Example.COM ", "kimmin", "김민"));

        assertThat(result.getResponse().getStatus()).isEqualTo(303);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/signup/verify-sent");
        assertThat(browser.sessionCookieValue()).isNotNull().isNotEqualTo(before);

        Map<String, Object> identity = jdbc.queryForMap("SELECT * FROM auth_identity");
        assertThat(identity).containsEntry("provider", "LOCAL")
                .containsEntry("provider_user_id", "kim.min@example.com")
                .containsEntry("email", "kim.min@example.com");
        assertThat(identity.get("email_verified_at")).isNull();
        assertThat((String) identity.get("password_hash")).startsWith("{bcrypt}").doesNotContain(PASSWORD);
        long memberId = ((Number) identity.get("member_id")).longValue();
        assertThat(jdbc.queryForList("SELECT type FROM member_agreement WHERE member_id = ?", String.class, memberId))
                .containsExactlyInAnyOrder("TERMS", "PRIVACY", "AGE14");
        assertThat(jdbc.queryForMap("SELECT handle, nickname, nickname_changed_at FROM member WHERE id = ?", memberId))
                .containsEntry("handle", "kimmin").containsEntry("nickname", "김민").containsEntry("nickname_changed_at", null);

        // 로그인된 상태
        assertThat(browser.perform(get("/signup/verify-sent")).getResponse().getStatus()).isEqualTo(200);

        List<MailpitClient.Mail> mails = mailpit.awaitMessagesTo("kim.min@example.com", 1);
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).token("/auth/verify")).isPresent();
        assertThat(mails.get(0).body()).contains("24");
    }

    /** 화면 순서(이메일 → 닉네임 → 주소 → 비밀번호 → 약관)대로 안내한다. 이메일만 보낸 요청에 약관 안내를 먼저 보이지 않는다. */
    @Test
    void errorsFollowTheScreenOrderSoTermsComeLast() throws Exception {
        String emailOnly = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/signup")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("email", "early@x.com")).andReturn().getResponse().getContentAsString();
        assertThat(emailOnly).doesNotContain("만 14세 이상 확인과 필수 약관 2개에 모두 동의해 주세요");
        String noPassword = mockMvc.perform(signup("early@x.com", "early1", "", "", "가나다", false, false))
                .andReturn().getResponse().getContentAsString();
        assertThat(noPassword).doesNotContain("만 14세 이상 확인과 필수 약관 2개에 모두 동의해 주세요");
        String onlyTermsMissing = mockMvc.perform(signup("early@x.com", "early1", PASSWORD, PASSWORD, "가나다", false, false))
                .andReturn().getResponse().getContentAsString();
        assertThat(onlyTermsMissing).contains("만 14세 이상 확인과 필수 약관 2개에 모두 동의해 주세요");
        assertThat(count("SELECT count(*) FROM member")).isZero();
    }

    @Test
    void bothAgreementsAreRequired() throws Exception {
        MvcResult onlyTerms = mockMvc.perform(signup("a1@x.com", "aaa111", PASSWORD, PASSWORD, "가나다", true, false)).andReturn();
        MvcResult onlyPrivacy = mockMvc.perform(signup("a1@x.com", "aaa111", PASSWORD, PASSWORD, "가나다", false, true)).andReturn();
        assertThat(onlyTerms.getResponse().getStatus()).isEqualTo(400);
        assertThat(onlyPrivacy.getResponse().getStatus()).isEqualTo(400);
        assertThat(onlyTerms.getResponse().getContentAsString()).contains("만 14세 이상 확인과 필수 약관 2개에 모두 동의해 주세요");
        assertThat(count("SELECT count(*) FROM member")).isZero();
    }


    @Test
    void sameEmailDifferingOnlyInCaseOrSpacesIsRejected() throws Exception {
        mockMvc.perform(signup("dup@x.com", "dupone", "중복하나"));
        MvcResult again = mockMvc.perform(signup("  DUP@X.com ", "duptwo", "중복둘")).andReturn();

        assertThat(again.getResponse().getStatus()).isEqualTo(400);
        String body = again.getResponse().getContentAsString();
        assertThat(body).contains("이미 가입된 이메일이에요.").contains("href=\"/login\"").contains("[로그인]")
                .contains("href=\"/password/forgot\"").contains("[비밀번호 찾기]");
        assertThat(count("SELECT count(*) FROM member")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM auth_identity")).isEqualTo(1);
    }

    @Test
    void withdrawingAccountsEmailGetsRestoreGuidance() throws Exception {
        mockMvc.perform(signup("bye@x.com", "byebye", "탈퇴예정"));
        jdbc.update("UPDATE member SET status = 'WITHDRAWN', withdrawn_at = now()");
        MvcResult again = mockMvc.perform(signup("bye@x.com", "byebye2", "다시가입")).andReturn();
        assertThat(again.getResponse().getStatus()).isEqualTo(400);
        assertThat(again.getResponse().getContentAsString()).contains("탈퇴 신청한 계정이 있어요. 로그인하면 복구할 수 있어요");
        assertThat(count("SELECT count(*) FROM member")).isEqualTo(1);
    }

    @Test
    void passwordViolationIsRejectedAndNotEchoedBack() throws Exception {
        String bad = "short1!";
        MvcResult result = mockMvc.perform(signup("pw@x.com", "pwtest", bad, bad, "비번", true, true)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("최대 16자").doesNotContain(bad);
        MvcResult tooLong = mockMvc.perform(signup("pw@x.com", "pwtest", "Abcdefgh1!abcdefg", "Abcdefgh1!abcdefg",
                "비번", true, true)).andReturn();
        assertThat(tooLong.getResponse().getContentAsString()).contains("비밀번호는 최대 16자까지 쓸 수 있어요")
                .doesNotContain("Abcdefgh1!abcdefg");
        MvcResult mismatch = mockMvc.perform(signup("pw@x.com", "pwtest", PASSWORD, PASSWORD + "x", "비번", true, true)).andReturn();
        assertThat(mismatch.getResponse().getStatus()).isEqualTo(400);
        assertThat(mismatch.getResponse().getContentAsString()).contains("비밀번호와 비밀번호 확인이 달라요");
        assertThat(count("SELECT count(*) FROM member")).isZero();
    }

    @Test
    void handleAndNicknameRulesOf002Apply() throws Exception {
        MvcResult prefix = mockMvc.perform(signup("h1@x.com", "go-kim", "닉하나")).andReturn();
        assertThat(prefix.getResponse().getStatus()).isEqualTo(400);
        assertThat(prefix.getResponse().getContentAsString()).contains("가입 방법과 맞지 않는 주소예요");

        MvcResult reserved = mockMvc.perform(signup("h2@x.com", "admin", "닉둘")).andReturn();
        assertThat(reserved.getResponse().getStatus()).isEqualTo(400);
        // 10-25 chan: 가입 화면에 주소 칸이 없어 대안 버튼(admin_2)은 더 보이지 않고 안내만 남는다
        assertThat(reserved.getResponse().getContentAsString()).contains("사용할 수 없는 주소예요");

        MvcResult nickname = mockMvc.perform(signup("h3@x.com", "goodhandle", "a")).andReturn();
        assertThat(nickname.getResponse().getStatus()).isEqualTo(400);
        assertThat(nickname.getResponse().getContentAsString()).contains("한글·영문·숫자로 2~30자까지 쓸 수 있어요");

        MvcResult email = mockMvc.perform(signup("not-an-email", "goodhandle", "닉셋")).andReturn();
        assertThat(email.getResponse().getStatus()).isEqualTo(400);
        assertThat(email.getResponse().getContentAsString()).contains("올바른 이메일 주소를 입력해 주세요");
        assertThat(count("SELECT count(*) FROM member")).isZero();
    }

    @Test
    void mailFailureDoesNotUndoTheSignup() throws Exception {
        doThrow(new IllegalStateException("smtp down")).when(mailSender).send(anyString(), anyString(), anyString(), anyMap());
        MvcResult result = mockMvc.perform(signup("mailfail@x.com", "mailfail", "메일실패")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(303);
        assertThat(count("SELECT count(*) FROM auth_identity WHERE email = 'mailfail@x.com'")).isEqualTo(1);
        assertThat(mailpit.countAfterQuietPeriod("mailfail@x.com")).isZero();
    }

}
