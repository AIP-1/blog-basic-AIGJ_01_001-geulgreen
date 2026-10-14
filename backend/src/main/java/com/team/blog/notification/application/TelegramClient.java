package com.team.blog.notification.application;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 096 텔레그램 Bot API. 토큰이 주소에 들어가므로 실패는 예외 종류만 남긴다(주소·메시지를 로그에 찍지 않음).
 * 실패해도 던지지 않는다: 알림 저장 같은 원래 동작을 막지 않는다.
 */
@Component
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final TelegramProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient client;

    public TelegramClient(TelegramProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        factory.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().requestFactory(factory).baseUrl(properties.apiBaseUrl()).build();
    }

    public void send(long chatId, String text) {
        call("sendMessage", Map.of("chat_id", chatId, "text", text, "link_preview_options", Map.of("is_disabled", true)));
    }

    public void setWebhook(String url, String secret) {
        call("setWebhook", Map.of("url", url, "secret_token", secret, "allowed_updates", List.of("message")));
    }

    /** 봇 아이디(t.me 링크용). 실패하면 null. */
    public String username() {
        JsonNode result = call("getMe", Map.of());
        return result == null ? null : result.path("username").asString(null);
    }

    private JsonNode call(String method, Map<String, Object> body) {
        try {
            String response = client.post().uri("/bot{token}/{method}", properties.botToken(), method)
                    .contentType(MediaType.APPLICATION_JSON).body(jsonMapper.writeValueAsString(body)).retrieve().body(String.class);
            return response == null ? null : jsonMapper.readTree(response).path("result");
        } catch (RestClientException | JacksonException e) {
            log.warn("텔레그램 {} 실패: {}", method, e.getClass().getSimpleName());
            return null;
        }
    }
}
