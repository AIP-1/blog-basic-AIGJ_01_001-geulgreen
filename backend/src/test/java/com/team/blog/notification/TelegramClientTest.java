package com.team.blog.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.team.blog.notification.application.TelegramClient;
import com.team.blog.notification.application.TelegramProperties;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** 096: Bot API 요청 모양(주소에 토큰, JSON 본문)과 응답 읽기, 실패해도 던지지 않음. */
class TelegramClientTest {

    @Test
    void callsBotApi() throws Exception {
        List<String> seen = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            seen.add(ex.getRequestURI().getPath() + " " + new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = "{\"ok\":true,\"result\":{\"username\":\"testbot\"}}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(ex.getRequestURI().getPath().endsWith("sendMessage") ? 400 : 200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
        try {
            TelegramClient client = new TelegramClient(new TelegramProperties(true, "123:abc",
                    "http://127.0.0.1:" + server.getAddress().getPort(), "https://example.com", Duration.ofMinutes(10),
                    Duration.ofSeconds(2)), JsonMapper.builder().build());
            assertThat(client.username()).isEqualTo("testbot");
            client.send(42, "안녕"); // 400이어도 던지지 않는다
            assertThat(seen.get(0)).startsWith("/bot123:abc/getMe");
            assertThat(seen.get(1)).startsWith("/bot123:abc/sendMessage").contains("\"chat_id\":42").contains("안녕");
        } finally {
            server.stop(0);
        }
    }
}
