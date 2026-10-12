package com.team.blog.tag.application.icon;

import com.team.blog.tag.application.suggest.AiTagProperties;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 056 태그 아이콘 AI(강성찬 개인 확장): 태그 이름만 보내고(공개 글의 태그라 공개 정보) 목록에서 아이콘 키 하나를 고르게 한다.
 * 021의 연결 설정(Gemini 키, Ollama 주소)을 그대로 쓴다. 연결이 안 되면 빈 값.
 */
@Component
public class TagIconAi {

    private final AiTagProperties ai;
    private final TagIconProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient gemini;
    private final RestClient ollama;

    public TagIconAi(AiTagProperties ai, TagIconProperties properties, JsonMapper jsonMapper) {
        this.ai = ai;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.gemini = client(ai.gemini().baseUrl(), ai.gemini().timeout());
        this.ollama = client(ai.ollama().baseUrl(), ai.ollama().timeout());
    }

    private static RestClient client(String base, java.time.Duration timeout) {
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(timeout).build());
        f.setReadTimeout(timeout);
        return RestClient.builder().requestFactory(f).baseUrl(base).build();
    }

    /** AI를 쓸 수 있게 설정돼 있는지(키나 Ollama 사용 설정). */
    public boolean available() {
        return properties.aiEnabled() && (!ai.gemini().apiKey().isBlank() || properties.useOllama());
    }

    static String prompt(String tagName) {
        String list = TagIconCatalog.ICONS.entrySet().stream().map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.joining(", "));
        return "블로그 태그 \"" + tagName + "\"의 주제에 가장 어울리는 아이콘을 아래 목록에서 하나만 골라 영어 키만 답해. "
                + "어울리는 게 없으면 hash.\n목록: " + list;
    }

    /** AI의 날 답(연결 실패면 빈 값). */
    public Optional<String> ask(String tagName) {
        String prompt = prompt(tagName);
        if (!ai.gemini().apiKey().isBlank()) {
            try {
                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of("maxOutputTokens", 10, "temperature", 0));
                String res = gemini.post().uri("/v1beta/models/{model}:generateContent", ai.gemini().model())
                        .header("x-goog-api-key", ai.gemini().apiKey()).contentType(MediaType.APPLICATION_JSON).body(body)
                        .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> {
                            throw new RestClientException("gemini " + r.getStatusCode());
                        }).body(String.class);
                return Optional.of(jsonMapper.readTree(res).path("candidates").path(0).path("content").path("parts").path(0)
                        .path("text").asString(""));
            } catch (Exception e) {
                if (!properties.useOllama()) {
                    return Optional.empty();
                }
            }
        }
        if (!properties.useOllama()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of("model", ai.ollama().model(), "prompt", prompt, "stream", false,
                    "options", Map.of("num_predict", 10, "temperature", 0));
            String res = ollama.post().uri("/api/generate").contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> {
                        throw new RestClientException("ollama " + r.getStatusCode());
                    }).body(String.class);
            return Optional.of(jsonMapper.readTree(res).path("response").asString(""));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
