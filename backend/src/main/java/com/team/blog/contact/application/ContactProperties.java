package com.team.blog.contact.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 090 문의하기. {@code perHour}: 한 IP가 한 시간에 보낼 수 있는 문의 수(스팸 막기). */
@ConfigurationProperties("blog.contact")
public record ContactProperties(@DefaultValue("true") boolean enabled, @DefaultValue("5") int perHour) {
}
