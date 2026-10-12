package com.team.blog.series.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 041 시리즈. */
@ConfigurationProperties("blog.series")
public record SeriesProperties(@DefaultValue("true") boolean enabled) {
}
