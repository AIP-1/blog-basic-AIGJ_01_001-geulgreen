package com.team.blog.account.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 074 인스타식 계정(강성찬 개인 확장). 끄면 가입·비밀번호 찾기는 링크 방식만, 주소는 못 바꾸고(옛 주소 넘김은 유지),
 * 닉네임은 공통 규칙(30일에 한 번), 블로그 머리말은 예전 모양으로 돌아간다.
 *
 * @param codeTtl          메일 6자리 코드 유효 시간
 * @param codeMaxAttempts  코드 하나로 틀릴 수 있는 횟수(넘으면 코드가 지워진다)
 * @param codePerMinute    이메일당 1분 발송 한도
 * @param codePerDay       이메일당 하루 발송 한도
 * @param codeIpPerHour    IP당 1시간 발송 한도
 * @param changeWindow     주소·닉네임 변경 횟수를 세는 기간(이 기간에 두 번)
 * @param handleHold       바꾼 옛 주소를 본인만 되돌릴 수 있게 잡아 두는 기간
 */
@ConfigurationProperties("blog.insta-account")
public record InstaAccountProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("10m") Duration codeTtl,
        @DefaultValue("5") int codeMaxAttempts,
        @DefaultValue("1") int codePerMinute,
        @DefaultValue("10") int codePerDay,
        @DefaultValue("20") int codeIpPerHour,
        @DefaultValue("14d") Duration changeWindow,
        @DefaultValue("14d") Duration handleHold) {
}
