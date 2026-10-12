package com.team.blog.writing.application;

import com.team.blog.account.application.AiConsentService;
import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.error.RateLimitedException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * AI로 글 다듬기(059): 판정 순서 기능 꺼짐·키 없음 → 로그인·쓰기 가능 → 자기 글(404) → 전체 공개 글만 → 동의 → 글 길이 →
 * 하루 횟수(429) → Gemini. 제안만 돌려주고 저장하지 않는다. 글 내용은 로그에 남기지 않는다.
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

    public String rewrite(Optional<CurrentUser> current, long postId, Request request) {
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
        boolean publicPost = "PUBLIC".equals(request.visibility())
                && ("DRAFT".equals(post.status()) || "PUBLIC".equals(post.visibility()));
        if (!publicPost) {
            throw new PostContentException("WRITING_AI_PUBLIC_ONLY");
        }
        if (!consent.hasConsent(me.memberId())) {
            throw new PostContentException("WRITING_AI_CONSENT_REQUIRED");
        }
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
        RedisRateLimiter.Result r = rateLimiter.tryAcquire(RedisRateLimiter.key("writing:ai", String.valueOf(me.memberId())),
                properties.perDay(), Duration.ofDays(1));
        if (!r.allowed()) {
            throw new RateLimitedException(r.retryAfterSeconds());
        }
        return client.rewrite(action == Action.ASK ? ask : action.instruction, text)
                .orElseThrow(() -> new PostContentException("WRITING_AI_UNAVAILABLE"));
    }
}
