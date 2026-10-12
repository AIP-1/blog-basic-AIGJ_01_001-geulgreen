package com.team.blog.guestbook.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 040 방명록. */
@ConfigurationProperties("blog.guestbook")
public record GuestbookProperties(@DefaultValue("true") boolean enabled, @DefaultValue("300") int maxLength,
                                  @DefaultValue("20") int perHour, @DefaultValue("30") int pageSize) {
}
