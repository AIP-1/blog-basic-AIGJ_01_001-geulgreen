package com.team.blog.account.insta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.team.blog.account.application.NicknameChangeService;
import com.team.blog.shared.error.NicknameChangeTooSoonException;
import com.team.blog.support.Browser;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** 074 인스타식 계정(강성찬 개인 확장): 메일 코드 가입·코드 비밀번호 찾기, 아이디 로그인, 주소 바꾸기·옛 주소 넘김, 닉네임 14일에 두 번. */
class InstaAccountIT extends IntegrationTestBase {

    private static final String PASSWORD = "Blog#2026ok";
    private static final Pattern CODE = Pattern.compile("(?<!\\d)(\\d{6})(?!\\d)");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    NicknameChangeService nicknameChangeService;

    private String codeFrom(String email) {
        var mails = mailpit.awaitMessagesTo(email, 1);
        Matcher m = CODE.matcher(mails.get(mails.size() - 1).body());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private MvcResult changeHandle(long memberId, String handle) throws Exception {
        return mockMvc.perform(put("/api/me/handle").with(csrf()).with(TestAuth.member(memberId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"" + handle + "\"}")).andReturn();
    }

    @Test
    void signupWithEmailCodeCreatesVerifiedAccountWithoutLinkMail() throws Exception {
        Browser browser = new Browser(mockMvc);
        assertThat(browser.perform(get("/signup")).getResponse().getContentAsString())
                .contains("인증 코드 받기", "Google로 계속하기", "스팸함도 확인해 주세요");
        assertThat(browser.perform(post("/api/signup/code").with(csrf()).param("email", "new@example.com"))
                .getResponse().getStatus()).isEqualTo(200);
        String code = codeFrom("new@example.com");
        assertThat(mailpit.awaitMessagesTo("new@example.com", 1).get(0).fromName()).isEqualTo("글그린"); // 보내는 이름
        assertThat(mailpit.awaitMessagesTo("new@example.com", 1).get(0).subject()).startsWith("[글그린] 인증 코드");
        // 1분 안에 다시 요청하면 막힘
        assertThat(browser.perform(post("/api/signup/code").with(csrf()).param("email", "new@example.com"))
                .getResponse().getStatus()).isEqualTo(429);
        String wrong = code.equals("000000") ? "111111" : "000000";
        assertThat(browser.perform(post("/api/signup/code/verify").with(csrf()).param("email", "new@example.com")
                .param("code", wrong)).getResponse().getStatus()).isEqualTo(400);
        assertThat(browser.perform(post("/api/signup/code/verify").with(csrf()).param("email", "new@example.com")
                .param("code", code)).getResponse().getStatus()).isEqualTo(200);

        MvcResult result = browser.perform(post("/signup").with(csrf()).param("email", "New@Example.com")
                .param("handle", "newbie").param("password", PASSWORD).param("passwordConfirm", PASSWORD)
                .param("nickname", "새사람").param("agreeTerms", "true").param("agreePrivacy", "true").param("ageGroup", "OVER14"));
        assertThat(result.getResponse().getStatus()).isEqualTo(303);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/");
        assertThat(jdbc.queryForObject("SELECT email_verified_at IS NOT NULL FROM auth_identity WHERE email = 'new@example.com'",
                Boolean.class)).isTrue();
        assertThat(mailpit.countAfterQuietPeriod("new@example.com")).isEqualTo(1); // 코드 메일만, 인증 링크 없음
    }

    @Test
    void codeIsDroppedAfterTooManyWrongTriesAndExistingEmailIsRejected() throws Exception {
        members.localMember("taken", "있는사람", "taken@example.com", PASSWORD, true);
        assertThat(mockMvc.perform(post("/api/signup/code").with(csrf()).param("email", "taken@example.com"))
                .andReturn().getResponse().getStatus()).isEqualTo(409);

        mockMvc.perform(post("/api/signup/code").with(csrf()).param("email", "try@example.com"));
        String code = codeFrom("try@example.com");
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/signup/code/verify").with(csrf()).param("email", "try@example.com").param("code", wrong));
        }
        assertThat(mockMvc.perform(post("/api/signup/code/verify").with(csrf()).param("email", "try@example.com")
                .param("code", code)).andReturn().getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void loginWithHandleOrEmail() throws Exception {
        members.localMember("loginme", "로그인", "login@example.com", PASSWORD, true);
        assertThat(mockMvc.perform(post("/login").with(csrf()).param("email", "LoginMe").param("password", PASSWORD))
                .andReturn().getResponse().getHeader("Location")).doesNotContain("error");
        assertThat(mockMvc.perform(post("/login").with(csrf()).param("email", "login@example.com").param("password", PASSWORD))
                .andReturn().getResponse().getHeader("Location")).doesNotContain("error");
        assertThat(mockMvc.perform(post("/login").with(csrf()).param("email", "loginme").param("password", "Wrong#2026x"))
                .andReturn().getResponse().getHeader("Location")).contains("error");
    }

    @Test
    void passwordResetWithMailCode() throws Exception {
        long id = members.localMember("resetme", "재설정", "reset@example.com", PASSWORD, true);
        String page = mockMvc.perform(post("/password/forgot").with(csrf()).param("email", "reset@example.com"))
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("메일로 받은 6자리 코드");
        String code = codeFrom("reset@example.com");
        MvcResult result = mockMvc.perform(post("/password/reset-code").with(csrf()).param("email", "reset@example.com")
                .param("code", code).param("password", "New#2026pass").param("passwordConfirm", "New#2026pass")).andReturn();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/login?reset");
        assertThat(mockMvc.perform(post("/login").with(csrf()).param("email", "resetme").param("password", "New#2026pass"))
                .andReturn().getResponse().getHeader("Location")).doesNotContain("error");
        // 같은 코드는 다시 못 씀
        assertThat(mockMvc.perform(post("/password/reset-code").with(csrf()).param("email", "reset@example.com")
                .param("code", code).param("password", "Next#2026pass").param("passwordConfirm", "Next#2026pass"))
                .andReturn().getResponse().getStatus()).isEqualTo(400);
        assertThat(id).isPositive();
    }

    @Test
    void handleChangeRedirectsOldAddressHoldsItAndAllowsTwoPerWindow() throws Exception {
        clock.set(Instant.parse("2026-10-13T00:00:00Z"));
        long me = members.localMember("oldname", "주인", "me@example.com", PASSWORD, true);
        long other = members.localMember("someone", "남남이", "other@example.com", PASSWORD, true);

        assertThat(changeHandle(me, "newname").getResponse().getContentAsString()).contains("\"handle\":\"newname\"");
        assertThat(jdbc.queryForObject("SELECT handle FROM member WHERE id = ?", String.class, me)).isEqualTo("newname");
        // 옛 주소는 새 주소로 넘김(하위 경로·쿼리 유지)
        MvcResult redirect = mockMvc.perform(get("/oldname/guestbook?x=1")).andReturn();
        assertThat(redirect.getResponse().getStatus()).isEqualTo(301);
        assertThat(redirect.getResponse().getRedirectedUrl()).isEqualTo("/newname/guestbook?x=1");
        assertThat(mockMvc.perform(get("/newname")).andReturn().getResponse().getContentAsString())
                .contains("게시물", "팔로워", "팔로잉");
        // 잡아 둔 동안 남은 못 가져감
        assertThat(changeHandle(other, "oldname").getResponse().getStatus()).isEqualTo(400);
        assertThat(mockMvc.perform(get("/api/handles/availability").param("handle", "oldname"))
                .andReturn().getResponse().getContentAsString()).contains("\"available\":false");

        // 두 번째 변경: 본인은 옛 주소로 되돌릴 수 있음, 세 번째는 막힘
        assertThat(changeHandle(me, "oldname").getResponse().getStatus()).isEqualTo(200);
        assertThat(changeHandle(me, "thirdname").getResponse().getStatus()).isEqualTo(409);

        // 기간이 지나면 다시 바꿀 수 있고, 잡아 둔 기간이 끝난 옛 주소는 남이 가져가며 넘김도 끝남
        clock.advance(Duration.ofDays(15));
        assertThat(changeHandle(me, "thirdname").getResponse().getStatus()).isEqualTo(200);
        assertThat(mockMvc.perform(get("/oldname")).andReturn().getResponse().getStatus()).isEqualTo(301);
        clock.advance(Duration.ofDays(15));
        assertThat(changeHandle(other, "oldname").getResponse().getStatus()).isEqualTo(200);
        assertThat(mockMvc.perform(get("/oldname")).andReturn().getResponse().getStatus()).isEqualTo(200);
        // 이메일 가입자는 소셜 접두어 주소를 못 고름
        assertThat(changeHandle(me, "go-thirdx").getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void permanentlyHeldOldHandleNeverGoesToSomeoneElse() throws Exception {
        clock.set(Instant.parse("2026-10-13T00:00:00Z"));
        long me = members.localMember("likekarina", "주인", "me@example.com", PASSWORD, true);
        long other = members.localMember("someone", "남남이", "other@example.com", PASSWORD, true);
        // 지금 쓰는 동안은 평소 블로그 주소 그대로
        assertThat(mockMvc.perform(get("/likekarina")).andReturn().getResponse().getStatus()).isEqualTo(200);

        assertThat(changeHandle(me, "newname").getResponse().getStatus()).isEqualTo(200);
        clock.advance(Duration.ofDays(60));
        assertThat(changeHandle(other, "likekarina").getResponse().getStatus()).isEqualTo(400);
        MvcResult redirect = mockMvc.perform(get("/likekarina/posts/71")).andReturn();
        assertThat(redirect.getResponse().getStatus()).isEqualTo(301);
        assertThat(redirect.getResponse().getRedirectedUrl()).isEqualTo("/newname/posts/71");
        // 본인은 되돌릴 수 있음
        assertThat(changeHandle(me, "likekarina").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void socialMemberDropsThePrefixAndCanGoBack() throws Exception {
        long g = members.active("go-ksc", "구글사람");
        members.addSocialIdentity(g, "GOOGLE", "g-ksc", "ksc@example.com", Instant.now());
        assertThat(changeHandle(g, "ksc").getResponse().getContentAsString()).contains("\"handle\":\"ksc\"");
        assertThat(mockMvc.perform(get("/go-ksc")).andReturn().getResponse().getRedirectedUrl()).isEqualTo("/ksc");
        assertThat(changeHandle(g, "gi-ksc").getResponse().getStatus()).isEqualTo(400);
        assertThat(changeHandle(g, "go-ksc").getResponse().getContentAsString()).contains("\"handle\":\"go-ksc\"");
    }

    @Test
    void nicknameCanChangeTwicePerFourteenDays() {
        clock.set(Instant.parse("2026-10-13T00:00:00Z"));
        long id = members.active("nickme", "처음");
        Instant first = clock.instant();
        nicknameChangeService.change(id, "두번째");
        clock.advance(Duration.ofDays(1));
        nicknameChangeService.change(id, "세번째");
        assertThatThrownBy(() -> nicknameChangeService.change(id, "네번째"))
                .isInstanceOfSatisfying(NicknameChangeTooSoonException.class,
                        e -> assertThat(e.getNextAllowedAt()).isEqualTo(first.plus(Duration.ofDays(14))));
        clock.advance(Duration.ofDays(13));
        nicknameChangeService.change(id, "네번째");
        assertThat(jdbc.queryForObject("SELECT nickname FROM member WHERE id = ?", String.class, id)).isEqualTo("네번째");
    }
}
