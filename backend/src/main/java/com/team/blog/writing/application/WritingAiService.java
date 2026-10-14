package com.team.blog.writing.application;

import com.team.blog.account.application.AiConsentService;
import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.error.RateLimitedException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * AI로 글 다듬기(059): 판정 순서 기능 꺼짐·키 없음 → 로그인·쓰기 가능 → 자기 글(404) → 전체 공개 글만 → 동의 → 글 길이 →
 * 하루 횟수(429) → Gemini. 제안만 돌려주고 저장하지 않는다. 글 내용은 로그에 남기지 않는다. 085 맞춤법 검사도 같은 판정을 쓴다.
 */
@Service
public class WritingAiService {

    /** 고치는 방법. {@code ASK}는 글쓴이가 적은 부탁을 그대로 쓴다. */
    public enum Action {
        IMPROVE("뜻은 그대로 두고 더 자연스럽고 읽기 좋게 다듬어 줘."),
        FIX("맞춤법·띄어쓰기·문법만 바로잡아 줘. 내용과 말투는 그대로 둬."),
        SHORTER("핵심만 남겨 더 짧게 줄여 줘."),
        EASIER("누구나 이해하게 쉬운 말로 풀어 써 줘."),
        ASK(null);

        private final String instruction;

        Action(String instruction) {
            this.instruction = instruction;
        }
    }

    private static final int MAX_FIXES = 50;

    public record Request(String text, String action, String ask, String visibility) {
    }

    private final JdbcTemplate jdbc;
    private final AccountGuard accountGuard;
    private final AiConsentService consent;
    private final RedisRateLimiter rateLimiter;
    private final WritingAiClient client;
    private final WritingAiProperties properties;

    public WritingAiService(JdbcTemplate jdbc, AccountGuard accountGuard, AiConsentService consent, RedisRateLimiter rateLimiter,
                            WritingAiClient client, WritingAiProperties properties) {
        this.jdbc = jdbc;
        this.accountGuard = accountGuard;
        this.consent = consent;
        this.rateLimiter = rateLimiter;
        this.client = client;
        this.properties = properties;
    }

    /** 서식 창에 AI 칸을 보일지(기능이 켜져 있고 Gemini 키가 있을 때). */
    public boolean available() {
        return properties.enabled() && client.configured();
    }

    /** 085 글 점검을 쓸지(꺼지면 글쓰기 화면이 예전 미리보기로 돌아간다). 맞춤법 검사는 여기에 {@link #available()}까지 맞아야 한다. */
    public boolean checkEnabled() {
        return properties.checkEnabled();
    }

    public String rewrite(Optional<CurrentUser> current, long postId, Request request) {
        CurrentUser me = guard(current, postId, request.visibility());
        Action action;
        try {
            action = Action.valueOf(String.valueOf(request.action()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PostContentException("INVALID_REQUEST");
        }
        String text = request.text() == null ? "" : request.text();
        if (text.isBlank() || text.codePointCount(0, text.length()) > properties.maxChars()) {
            throw new PostContentException("WRITING_AI_TEXT_LENGTH");
        }
        String ask = request.ask() == null ? "" : request.ask().strip();
        if (action == Action.ASK && (ask.isEmpty() || ask.codePointCount(0, ask.length()) > properties.maxAskChars())) {
            throw new PostContentException("WRITING_AI_ASK_REQUIRED");
        }
        acquire(me);
        return client.rewrite(action == Action.ASK ? ask : action.instruction, text)
                .orElseThrow(() -> new PostContentException("WRITING_AI_UNAVAILABLE"));
    }

    /** 틀린 곳 하나: 본문에 그대로 있는 {@code before}를 {@code after}로. */
    public record Fix(String before, String after) {
    }

    /** 맞춤법 검사 결과. 글이 길어 앞부분만 봤으면 {@code truncated}. */
    public record Spelling(List<Fix> fixes, boolean truncated) {
    }

    /** 085 맞춤법 검사: 본문 앞 {@code checkMaxChars}자를 보내고 틀린 곳만 돌려준다(제안만, 저장하지 않음). 판정 순서는 {@link #rewrite}와 같다. */
    public Spelling spelling(Optional<CurrentUser> current, long postId, Request request) {
        if (!properties.checkEnabled()) {
            throw new PostContentException("WRITING_AI_UNAVAILABLE");
        }
        CurrentUser me = guard(current, postId, request.visibility());
        String text = request.text() == null ? "" : request.text();
        if (text.isBlank()) {
            throw new PostContentException("WRITING_CHECK_EMPTY");
        }
        boolean truncated = text.codePointCount(0, text.length()) > properties.checkMaxChars();
        if (truncated) {
            text = text.substring(0, text.offsetByCodePoints(0, properties.checkMaxChars()));
        }
        acquire(me);
        String answer = client.spelling(text).orElseThrow(() -> new PostContentException("WRITING_AI_UNAVAILABLE"));
        return new Spelling(parseFixes(answer, text), truncated);
    }

    /** 답의 줄마다 "틀린 => 고친". 본문에 없는 것·같은 것·겹치는 것은 버린다(AI가 지어낸 곳은 화면에 안 보이게). */
    static List<Fix> parseFixes(String answer, String text) {
        Map<String, Fix> fixes = new LinkedHashMap<>();
        for (String line : answer.lines().toList()) {
            String[] parts = line.strip().replaceFirst("^(?:[-*•]\\s+|\\d+[.)]\\s+)", "").split("\\s*(?:=>|→)\\s*", 2);
            if (parts.length < 2) {
                continue;
            }
            String before = unquote(parts[0]);
            String after = unquote(parts[1]);
            if (!before.isEmpty() && !before.equals(after) && text.contains(before) && fixes.size() < MAX_FIXES) {
                fixes.putIfAbsent(before, new Fix(before, after));
            }
        }
        return List.copyOf(fixes.values());
    }

    private static String unquote(String s) {
        return s.strip().replaceAll("^[\"“'‘「]+|[\"”'’」]+$", "").strip();
    }

    // 기능 꺼짐·키 없음 → 로그인·쓰기 가능 → 자기 글(404) → 전체 공개 글만 → 동의
    private CurrentUser guard(Optional<CurrentUser> current, long postId, String visibility) {
        if (!available()) {
            throw new PostContentException("WRITING_AI_UNAVAILABLE");
        }
        CurrentUser me = accountGuard.requireWritable(current);
        record Owned(String status, String visibility) {
        }
        Owned post = jdbc.query("SELECT status, visibility FROM post WHERE id = ? AND author_id = ? AND deleted_at IS NULL",
                        (rs, n) -> new Owned(rs.getString(1), rs.getString(2)), postId, me.memberId())
                .stream().findFirst().orElseThrow(NotFoundException::new);
        // 021과 같은 규칙: 고르고 있는 공개 범위가 전체 공개이고, 발행된 글이면 지금도 전체 공개일 때만 외부 AI로 보낸다
        boolean publicPost = "PUBLIC".equals(visibility)
                && ("DRAFT".equals(post.status()) || "PUBLIC".equals(post.visibility()));
        if (!publicPost) {
            throw new PostContentException("WRITING_AI_PUBLIC_ONLY");
        }
        if (!consent.hasConsent(me.memberId())) {
            throw new PostContentException("WRITING_AI_CONSENT_REQUIRED");
        }
        return me;
    }

    // 다듬기와 맞춤법 검사가 하루 횟수를 함께 쓴다(429)
    private void acquire(CurrentUser me) {
        RedisRateLimiter.Result r = rateLimiter.tryAcquire(RedisRateLimiter.key("writing:ai", String.valueOf(me.memberId())),
                properties.perDay(), Duration.ofDays(1));
        if (!r.allowed()) {
            throw new RateLimitedException(r.retryAfterSeconds());
        }
    }
}
