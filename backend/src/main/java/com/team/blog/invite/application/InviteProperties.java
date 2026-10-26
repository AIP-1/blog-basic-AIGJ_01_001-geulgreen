package com.team.blog.invite.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 10-25 chan 친구 초대(강성찬 개인 확장). 끄면 {@code /i/...}·{@code /me/invite}가 사라지고 프로필 메뉴의 [친구 초대]도 숨는다.
 * 함께 쓰기 방의 초대 칸은 예전 {@code /missions/clubs/join/{코드}} 링크로 돌아간다. 가입·로그인 뒤 이동은 초대 화면이
 * 세션에 경로를 적어야만 바뀌므로 따로 끌 분기가 없다.
 */
@ConfigurationProperties("blog.invite")
public record InviteProperties(@DefaultValue("true") boolean enabled) {
}
