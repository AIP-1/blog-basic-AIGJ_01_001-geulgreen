package com.team.blog.discovery.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 044 RSS 구독(강성찬 개인 확장). */
@ConfigurationProperties("blog.rss")
public record RssProperties(@DefaultValue("true") boolean enabled, @DefaultValue("20") int size) {
}
