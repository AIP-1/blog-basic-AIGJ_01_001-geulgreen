package com.team.blog.notification.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 096 강성찬 개인 확장: 텔레그램 알림. 토큰이 비어 있으면 조용히 꺼진다.
 *
 * @param enabled    기능 스위치(꺼도 연결해 둔 채팅 번호는 남는다)
 * @param botToken   @BotFather가 준 봇 토큰(환경 변수 TELEGRAM_BOT_TOKEN으로만)
 * @param siteOrigin 사이트 주소. https일 때만 봇 웹훅을 등록한다(로컬은 등록하지 않음)
 * @param linkTtl    [텔레그램 연결] 일회용 코드 유효 시간
 */
@ConfigurationProperties("blog.telegram")
public record TelegramProperties(@DefaultValue("true") boolean enabled,
                                 @DefaultValue("") String botToken,
                                 @DefaultValue("https://api.telegram.org") String apiBaseUrl,
                                 @DefaultValue("http://localhost:8080") String siteOrigin,
                                 @DefaultValue("10m") Duration linkTtl,
                                 @DefaultValue("5s") Duration timeout) {

    public boolean active() {
        return enabled && !botToken.isBlank();
    }
}
