package com.team.blog.mission.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 10-29 방 글감 AI 추천 설정. {@code secret}은 방장 API 키를 암호화하는 서버 비밀값(배포 스크립트가 서버에서 한 번 만든다).
 * 비어 있으면 연동을 받지 않는다. 모델은 방장 돈이 덜 드는 작은 모델을 기본으로.
 */
@ConfigurationProperties("blog.club-ai")
public record ClubAiProperties(@DefaultValue("") String secret,
                               @DefaultValue("10") int dailyLimit,
                               @DefaultValue("30s") Duration timeout,
                               @DefaultValue("https://api.openai.com") String openaiBaseUrl,
                               @DefaultValue("gpt-5-mini") String openaiModel,
                               @DefaultValue("https://generativelanguage.googleapis.com") String geminiBaseUrl,
                               @DefaultValue("gemini-flash-lite-latest") String geminiModel,
                               @DefaultValue("https://api.anthropic.com") String claudeBaseUrl,
                               @DefaultValue("claude-haiku-5-5") String claudeModel) {

    public boolean enabled() {
        return secret.length() >= 16;
    }
}
