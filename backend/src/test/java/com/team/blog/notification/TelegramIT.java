package com.team.blog.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.notification.application.TelegramClient;
import com.team.blog.notification.application.TelegramService;
import com.team.blog.shared.event.MemberSignedUp;
import com.team.blog.shared.event.ReportReceived;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 096 텔레그램 알림(강성찬 개인 확장): 설정의 [텔레그램 연결] → 봇 /start 코드 → 저장, 관리자에게만 운영 알림. */
@TestPropertySource(properties = "blog.telegram.bot-token=test-token")
class TelegramIT extends IntegrationTestBase {

    @MockitoBean
    TelegramClient client;

    @Autowired
    TelegramService telegram;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEventPublisher events;

    private String webhook(String secret, String text, long chatId) throws Exception {
        String body = "{\"message\":{\"chat\":{\"id\":" + chatId + ",\"type\":\"private\"},\"text\":\"" + text + "\"}}";
        return String.valueOf(mockMvc.perform(post("/api/telegram/webhook").header("X-Telegram-Bot-Api-Secret-Token", secret)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus());
    }

    @Test
    void adminLinksAndGetsOperationalMessages() throws Exception {
        when(client.username()).thenReturn("testbot");
        long admin = members.localMember("tgboss", "tgboss", "tgboss@example.com", "Blog#2026ok", true);
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", admin);
        long user = members.localMember("tguser", "tguser", "tguser@example.com", "Blog#2026ok", true);

        // 관리자 설정 화면에만 칸이 보인다
        assertThat(mockMvc.perform(get("/settings").with(TestAuth.admin(admin))).andReturn().getResponse().getContentAsString())
                .contains("텔레그램 알림").contains("/settings/telegram/link");
        assertThat(mockMvc.perform(get("/settings").with(TestAuth.member(user))).andReturn().getResponse().getContentAsString())
                .doesNotContain("/settings/telegram/link");

        String location = mockMvc.perform(post("/settings/telegram/link").with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().isSeeOther()).andReturn().getResponse().getRedirectedUrl();
        assertThat(location).startsWith("https://t.me/testbot?start=");
        String code = location.substring(location.indexOf('=') + 1);

        // 확인값이 틀리면 거절, 맞으면 연결(코드는 한 번만)
        assertThat(webhook("wrong", "/start " + code, 777)).isEqualTo("403");
        assertThat(webhook(telegram.webhookSecret(), "/start " + code, 777)).isEqualTo("200");
        verify(client).send(eq(777L), contains("연결됐어요"));
        assertThat(telegram.linked(admin)).isTrue();
        assertThat(webhook(telegram.webhookSecret(), "/start " + code, 888)).isEqualTo("200");
        verify(client).send(eq(888L), contains("만료"));

        events.publishEvent(new MemberSignedUp(user, "LOCAL"));
        verify(client).send(eq(777L), contains("새 회원 가입: tguser"));
        events.publishEvent(new ReportReceived("COMMENT", "스팸·광고"));
        verify(client).send(eq(777L), contains("새 신고: 댓글, 스팸·광고"));

        // 끊으면 더 보내지 않는다
        mockMvc.perform(post("/settings/telegram/unlink").with(csrf()).with(TestAuth.admin(admin))).andExpect(status().isSeeOther());
        assertThat(telegram.linked(admin)).isFalse();
        events.publishEvent(new ReportReceived("POST", "기타"));
        verify(client, never()).send(anyLong(), contains("새 신고: 글"));
    }
}
