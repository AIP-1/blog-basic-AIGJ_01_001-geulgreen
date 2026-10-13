package com.team.blog.privacy.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 비공개 계정(073). 끄면 모두 공개 계정처럼 동작하고 설정에서도 숨는다(저장된 설정은 그대로). */
@ConfigurationProperties("blog.private-accounts")
public record PrivacyProperties(@DefaultValue("true") boolean enabled) {
}
