package com.team.blog.tag.application.icon;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 056 태그 아이콘. AI 다듬기는 Gemini 키가 있을 때(또는 {@code use-ollama}가 켜져 있을 때)만, 한 번에 {@code batch}개씩.
 */
@ConfigurationProperties("blog.tag-icons")
public record TagIconProperties(@DefaultValue("true") boolean enabled, @DefaultValue("true") boolean aiEnabled,
                                @DefaultValue("false") boolean useOllama, @DefaultValue("20") int batch) {
}
