package com.team.blog.writing.application;

import com.team.blog.tag.application.suggest.AiTagProperties;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 고른 글을 Gemini에게 고쳐 받기(059). 021의 연결 설정(키·주소·모델·시간 한도)을 쓴다. 자체 Ollama는 느리고 한국어 문장이
 * 어색해 쓰지 않는다. 답은 앞뒤 코드 울타리·따옴표만 벗겨 돌려준다(화면이 글자로만 넣는다).
 */
@Component
public class WritingAiClient {

    static final String PROMPT = """
            아래 [글]을 요청대로 고쳐서 고친 글만 답해 줘.
            규칙: 설명·인사·제목·따옴표 없이 고친 글만. 원래 글의 언어로, 줄바꿈과 Markdown 서식(굵게·목록·링크 등)은 살려 줘.
            요청: %s
            [글]
            %s""";

    // 085 맞춤법 검사: 고친 글 전체 대신 틀린 곳만 받는다(본문을 통째로 덮어쓰지 않게)
    static final String SPELL_PROMPT = """
            아래 [글]에서 맞춤법·띄어쓰기가 틀린 곳만 찾아 줘.
            규칙: 한 줄에 하나씩 "틀린 부분 => 고친 부분" 형식으로만 답해 줘. 틀린 부분은 [글]에 있는 그대로 몇 낱말만 적어 줘.
            설명·번호·따옴표는 쓰지 말고, 코드·링크 주소·Markdown 표시는 고치지 마. 틀린 곳이 없으면 "없음"이라고만 답해 줘.
            [글]
            %s""";

    private static final Logger log = LoggerFactory.getLogger(WritingAiClient.class);

    private final AiTagProperties ai;
    private final JsonMapper jsonMapper;
    private final RestClient gemini;

    public WritingAiClient(AiTagProperties ai, JsonMapper jsonMapper) {
        this.ai = ai;
        this.jsonMapper = jsonMapper;
        JdkClientHttpRequestFactory f = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(ai.gemini().timeout()).build());
        f.setReadTimeout(ai.gemini().timeout());
        this.gemini = RestClient.builder().requestFactory(f).baseUrl(ai.gemini().baseUrl()).build();
    }

    public boolean configured() {
        return !ai.gemini().apiKey().isBlank();
    }

    /** 고친 글. 키가 없거나 실패·빈 답이면 빈 값. */
    public Optional<String> rewrite(String request, String text) {
        return ask(PROMPT.formatted(request, text), 0.4).map(WritingAiClient::clean).filter(s -> !s.isBlank());
    }

    /** 085 맞춤법 검사 답(줄마다 "틀린 => 고친"). 키가 없거나 실패하면 빈 값. */
    public Optional<String> spelling(String text) {
        return ask(SPELL_PROMPT.formatted(text), 0.1);
    }

    private Optional<String> ask(String prompt, double temperature) {
        if (!configured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of("maxOutputTokens", 2048, "temperature", temperature));
            String res = gemini.post().uri("/v1beta/models/{model}:generateContent", ai.gemini().model())
                    .header("x-goog-api-key", ai.gemini().apiKey()).contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().onStatus(HttpStatusCode::isError, (req, r) -> {
                        // Google 오류 본문(이유 코드·설명)만 붙인다. 보낸 글은 오류 본문에 들어 있지 않다
                        String why = new String(r.getBody().readNBytes(400), StandardCharsets.UTF_8);
                        throw new RestClientException("gemini " + r.getStatusCode() + " " + why.replaceAll("\\s+", " "));
                    }).body(String.class);
            String answer = jsonMapper.readTree(res).path("candidates").path(0).path("content").path("parts").path(0)
                    .path("text").asString("");
            return Optional.of(answer);
        } catch (Exception e) {
            // 왜 안 되는지 운영 로그로 알 수 있게(키·글 내용은 남기지 않음)
            log.warn("Gemini 요청 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** 앞뒤 공백, 답 전체를 감싼 코드 울타리(```)·따옴표만 벗긴다. 너무 길면 자른다. */
    static String clean(String raw) {
        String s = raw == null ? "" : raw.strip();
        if (s.startsWith("```") && s.endsWith("```") && s.length() >= 6) {
            s = s.substring(s.indexOf('\n') < 0 ? 3 : s.indexOf('\n') + 1, s.length() - 3).strip();
        }
        if (s.length() >= 2 && "\"“'‘「".indexOf(s.charAt(0)) >= 0 && "\"”'’」".indexOf(s.charAt(s.length() - 1)) >= 0) {
            s = s.substring(1, s.length() - 1).strip();
        }
        return s.length() > 6000 ? s.substring(0, 6000) : s;
    }
}
