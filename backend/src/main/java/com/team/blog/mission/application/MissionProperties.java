package com.team.blog.mission.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 057 오늘의 미션(강성찬 개인 확장, 034를 바꿈). 하루 주제 수, AI로 주제 만들기(Gemini 키가 있을 때, 또는 Ollama를 켰을 때),
 * 목록에 보일 글 수, 지난 며칠을 보여 줄지.
 */
@ConfigurationProperties("blog.missions")
public record MissionProperties(@DefaultValue("true") boolean enabled, @DefaultValue("3") int topicsPerDay,
                                @DefaultValue("true") boolean aiEnabled, @DefaultValue("false") boolean useOllama,
                                @DefaultValue("60") int feedSize, @DefaultValue("7") int historyDays) {
}
