package com.team.blog.mission.application;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 10-29 방장이 연결한 AI 부르기: OpenAI(Chat Completions)·Gemini(generateContent)·Claude(Messages) 세 곳을 HTTP로 직접.
 * 연결할 때는 모델 목록(돈이 들지 않음)으로 키가 맞는지만 본다. 키·답은 로그에 남기지 않는다.
 */
@Component
public class ClubAiClient {

    /** 화면에 그대로 보여 줄 실패 이유. */
    public static class AiException extends RuntimeException {
        public AiException(String message) {
            super(message);
        }
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ClubAiClient.class);
    private final ClubAiProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient http;

    public ClubAiClient(ClubAiProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
        f.setReadTimeout(properties.timeout());
        this.http = RestClient.builder().requestFactory(f).build();
    }

    /** 키가 맞는지(모델 목록을 받아 본다). 틀리면 {@link AiException}. */
    public void check(String provider, String key) {
        call(() -> switch (provider) {
            case "OPENAI" -> http.get().uri(properties.openaiBaseUrl() + "/v1/models").header("Authorization", "Bearer " + key)
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> fail(r.getStatusCode().value(), "")).body(String.class);
            case "GEMINI" -> http.get().uri(properties.geminiBaseUrl() + "/v1beta/models").header("x-goog-api-key", key)
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> fail(r.getStatusCode().value(), "")).body(String.class);
            case "CLAUDE" -> http.get().uri(properties.claudeBaseUrl() + "/v1/models").header("x-api-key", key)
                    .header("anthropic-version", "2026-10-21")
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> fail(r.getStatusCode().value(), "")).body(String.class);
            default -> throw new AiException("ChatGPT, Gemini, Claude 중에서 골라 주세요");
        });
    }

    /** 질문 하나를 보내고 답 글을 받는다. */
    public String ask(String provider, String key, String prompt) {
        String res = call(() -> switch (provider) {
            case "OPENAI" -> http.post().uri(properties.openaiBaseUrl() + "/v1/chat/completions").header("Authorization", "Bearer " + key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", properties.openaiModel(), "max_completion_tokens", 4000,
                            "messages", List.of(Map.of("role", "user", "content", prompt))))
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> fail(r.getStatusCode().value(), body(r))).body(String.class);
            case "GEMINI" -> http.post().uri(properties.geminiBaseUrl() + "/v1beta/models/{model}:generateContent", properties.geminiModel())
                    .header("x-goog-api-key", key).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                            "generationConfig", Map.of("maxOutputTokens", 800, "temperature", 0.9)))
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> fail(r.getStatusCode().value(), body(r))).body(String.class);
            case "CLAUDE" -> http.post().uri(properties.claudeBaseUrl() + "/v1/messages").header("x-api-key", key)
                    .header("anthropic-version", "2026-10-21").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", properties.claudeModel(), "max_tokens", 4000, "output_config", Map.of("effort", "low"),
                            "messages", List.of(Map.of("role", "user", "content", prompt))))
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> fail(r.getStatusCode().value(), body(r))).body(String.class);
            default -> throw new AiException("ChatGPT, Gemini, Claude 중에서 골라 주세요");
        });
        return text(provider, res);
    }

    String text(String provider, String res) {
        JsonNode root = jsonMapper.readTree(res == null ? "{}" : res);
        return switch (provider) {
            case "OPENAI" -> root.path("choices").path(0).path("message").path("content").asString("");
            case "GEMINI" -> root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString("");
            default -> {
                StringBuilder b = new StringBuilder();
                root.path("content").forEach(block -> {
                    if ("text".equals(block.path("type").asString(""))) {
                        b.append(block.path("text").asString(""));
                    }
                });
                yield b.toString();
            }
        };
    }

    private static String body(org.springframework.http.client.ClientHttpResponse r) {
        try {
            return new String(r.getBody().readNBytes(2000), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return "";
        }
    }

    /** 상태 코드를 방장이 알아들을 말로. 402·크레딧 부족은 충전 안내, 401·403은 키 문제, 429는 한도. */
    static void fail(int status, String body) {
        String b = body.toLowerCase(java.util.Locale.ROOT);
        if (status == 402 || b.contains("insufficient_quota") || b.contains("credit balance")) {
            throw new AiException("AI 계정에 남은 크레딧이 없어요. 결제 화면에서 충전한 뒤 다시 해 보세요");
        }
        if (status == 401 || status == 403) {
            throw new AiException("키가 맞지 않거나 지워졌어요. 키를 다시 확인해 주세요");
        }
        if (status == 429) {
            throw new AiException("AI 쪽 사용 한도에 걸렸어요. 잠시 뒤 다시 해 보세요");
        }
        throw new AiException("AI가 답하지 않았어요. 잠시 뒤 다시 해 보세요");
    }

    private static String call(java.util.function.Supplier<String> request) {
        try {
            return request.get();
        } catch (AiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            fail(e.getStatusCode().value(), e.getResponseBodyAsString());
            return "";
        } catch (RuntimeException e) {
            LOG.warn("방 글감 AI 호출 실패: {}", e.toString()); // 키는 헤더에만 있어 메시지에 들어가지 않는다
            throw new AiException("AI에 연결하지 못했어요. 잠시 뒤 다시 해 보세요");
        }
    }
}
