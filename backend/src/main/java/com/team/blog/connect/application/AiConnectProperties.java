package com.team.blog.connect.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 097 AI 연결. 한 사람의 키 수, 키 하나의 분당 요청 수. */
@ConfigurationProperties("blog.ai-connect")
public record AiConnectProperties(@DefaultValue("true") boolean enabled, @DefaultValue("5") int maxKeys,
                                  @DefaultValue("30") int perMinute) {
}
