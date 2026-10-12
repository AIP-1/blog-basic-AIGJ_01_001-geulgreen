package com.team.blog.mission.application;

import com.team.blog.tag.application.suggest.AiTagProperties;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 057 오늘의 주제를 AI에게 받기(강성찬 개인 확장): 021의 연결 설정(Gemini 키, Ollama 주소)을 쓴다. 보내는 것은 날짜와 요청 문장뿐.
 * 답은 "주제|안내" 줄로 받아 글자 수·형식을 검사하고, 모자라면 빈 값(호출한 쪽이 내장 주제를 쓴다).
 */
@Component
public class TopicAi {

    static final String INSTRUCTION = """
            블로그 회원들이 오늘 함께 쓸 글쓰기 주제 %d개를 추천해 줘. 날짜: %s.
            규칙: 서로 다른 분야(일상, 추억, 음식, 여행·장소, 취미·문화, 일·공부, 생각, 계절·자연 중)에서 고르고,
            누구나 쓸 수 있는 밝고 안전한 주제로(정치·종교·건강 진단·돈 투자·개인정보를 묻는 주제는 빼고).
            한 줄에 하나씩 "주제|한 줄 안내" 형식으로만 답해. 주제는 25자 이내, 안내는 60자 이내 한 문장. 번호·따옴표·마크다운 없이.
            """;

    private final AiTagProperties ai;
    private final MissionProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient gemini;
    private final RestClient ollama;

    public TopicAi(AiTagProperties ai, MissionProperties properties, JsonMapper jsonMapper) {
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

    public boolean available() {
        return properties.aiEnabled() && (!ai.gemini().apiKey().isBlank() || properties.useOllama());
    }

    /** AI가 만든 주제 {@code count}개(형식이 맞는 것만 모아 모자라면 빈 값). */
    public Optional<List<TopicPool.Topic>> topics(LocalDate day, int count) {
        if (!available()) {
            return Optional.empty();
        }
        String prompt = INSTRUCTION.formatted(count, day);
        Optional<String> answer = Optional.empty();
        if (!ai.gemini().apiKey().isBlank()) {
            answer = call(() -> {
                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of("maxOutputTokens", 400, "temperature", 0.9));
                String res = gemini.post().uri("/v1beta/models/{model}:generateContent", ai.gemini().model())
                        .header("x-goog-api-key", ai.gemini().apiKey()).contentType(MediaType.APPLICATION_JSON).body(body)
                        .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> {
                            throw new RestClientException("gemini " + r.getStatusCode());
                        }).body(String.class);
                return jsonMapper.readTree(res).path("candidates").path(0).path("content").path("parts").path(0).path("text").asString("");
            });
        }
        if (answer.isEmpty() && properties.useOllama()) {
            answer = call(() -> {
                Map<String, Object> body = Map.of("model", ai.ollama().model(), "prompt", prompt, "stream", false,
                        "options", Map.of("num_predict", 400, "temperature", 0.9));
                String res = ollama.post().uri("/api/generate").contentType(MediaType.APPLICATION_JSON).body(body)
                        .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> {
                            throw new RestClientException("ollama " + r.getStatusCode());
                        }).body(String.class);
                return jsonMapper.readTree(res).path("response").asString("");
            });
        }
        return answer.map(a -> parse(a, count)).filter(list -> list.size() == count);
    }

    private static Optional<String> call(java.util.concurrent.Callable<String> request) {
        try {
            return Optional.ofNullable(request.call()).filter(s -> !s.isBlank());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** "주제|안내" 줄을 다듬어 모은다: 번호·따옴표·마크다운 기호 제거, 주제 2~30자, 안내 0~100자. */
    public static List<TopicPool.Topic> parse(String answer, int count) {
        List<TopicPool.Topic> out = new ArrayList<>();
        for (String raw : answer.split("\\R")) {
            String line = raw.replaceAll("^[\\s\\-*•]*\\d*[.)]?\\s*", "").replaceAll("[\"'“”‘’`*_#>]", "").strip();
            int bar = line.indexOf('|');
            if (bar <= 0) {
                continue;
            }
            String title = line.substring(0, bar).strip();
            String hint = line.substring(bar + 1).strip();
            int len = title.codePointCount(0, title.length());
            if (len < 2 || len > 30 || hint.codePointCount(0, hint.length()) > 100) {
                continue;
            }
            out.add(new TopicPool.Topic(title, hint));
            if (out.size() == count) {
                break;
            }
        }
        return out;
    }
}
