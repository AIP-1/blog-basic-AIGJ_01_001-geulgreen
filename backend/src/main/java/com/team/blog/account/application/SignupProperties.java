package com.team.blog.account.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 10-25 chan 가입 칸 줄이기(강성찬 개인 확장). {@code simple}이면 가입 화면에 블로그 주소·비밀번호 확인 칸이 없고,
 * 주소는 이메일 앞부분으로 만들어 준다. 끄면 두 칸이 돌아오고 예전처럼 주소 검사·비밀번호 일치 검사를 한다.
 */
@ConfigurationProperties("blog.account.signup")
public record SignupProperties(@DefaultValue("true") boolean simple) {
}
