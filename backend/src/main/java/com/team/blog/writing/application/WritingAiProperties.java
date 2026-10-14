package com.team.blog.writing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * AI로 글 다듬기(059). 하루 횟수, 한 번에 보낼 글 길이, 직접 부탁 길이. {@code enabled=false}면 서식 창에 AI 칸이 없다.
 * 085 글 점검(강성찬 개인 확장): {@code checkEnabled=false}면 글쓰기 화면이 예전 미리보기로 돌아간다. 맞춤법 검사는 본문 앞 {@code checkMaxChars}자까지.
 */
@ConfigurationProperties("blog.writing-ai")
public record WritingAiProperties(@DefaultValue("true") boolean enabled, @DefaultValue("30") int perDay,
                                  @DefaultValue("2000") int maxChars, @DefaultValue("200") int maxAskChars,
                                  @DefaultValue("true") boolean checkEnabled, @DefaultValue("6000") int checkMaxChars) {
}
