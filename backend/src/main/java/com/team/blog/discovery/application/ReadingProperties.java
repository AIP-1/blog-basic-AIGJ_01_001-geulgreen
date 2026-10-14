package com.team.blog.discovery.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 글 화면 읽기 도우미(강성찬 개인 확장): 038 이전·다음 글, 080 관심 있을 만한 글, 039 목차, 052 읽기 진행 막대, 053 공유하기.
 * {@code kakaoJsKey}: 092 후속 카카오톡 공유용 카카오 JavaScript 키(KAKAO_JS_KEY). 비면 카카오톡 항목과 SDK를 안 쓴다.
 */
@ConfigurationProperties("blog.reading")
public record ReadingProperties(@DefaultValue("true") boolean navigationEnabled,
                                @DefaultValue("true") boolean tocEnabled, @DefaultValue("true") boolean progressEnabled,
                                @DefaultValue("true") boolean shareEnabled, @DefaultValue("") String kakaoJsKey) {

    public boolean kakaoEnabled() {
        return shareEnabled && kakaoJsKey != null && !kakaoJsKey.isBlank();
    }
}
