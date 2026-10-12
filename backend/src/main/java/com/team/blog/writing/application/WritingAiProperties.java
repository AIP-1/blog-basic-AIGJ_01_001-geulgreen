package com.team.blog.writing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** AI로 글 다듬기(059). 하루 횟수, 한 번에 보낼 글 길이, 직접 부탁 길이. {@code enabled=false}면 서식 창에 AI 칸이 없다. */
@ConfigurationProperties("blog.writing-ai")
public record WritingAiProperties(@DefaultValue("true") boolean enabled, @DefaultValue("30") int perDay,
                                  @DefaultValue("2000") int maxChars, @DefaultValue("200") int maxAskChars) {
}
