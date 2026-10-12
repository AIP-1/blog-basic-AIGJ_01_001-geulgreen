package com.team.blog.discovery.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 글 화면 읽기 도우미(강성찬 개인 확장): 038 이전·다음 글과 관련 글, 039 목차. */
@ConfigurationProperties("blog.reading")
public record ReadingProperties(@DefaultValue("true") boolean navigationEnabled, @DefaultValue("3") int relatedCount,
                                @DefaultValue("true") boolean tocEnabled) {
}
