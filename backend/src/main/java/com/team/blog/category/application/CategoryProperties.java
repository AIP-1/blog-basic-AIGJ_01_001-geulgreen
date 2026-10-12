package com.team.blog.category.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 037 카테고리. */
@ConfigurationProperties("blog.categories")
public record CategoryProperties(@DefaultValue("true") boolean enabled, @DefaultValue("20") int maxPerBlog,
                                 @DefaultValue("60") int listSize) {
}
