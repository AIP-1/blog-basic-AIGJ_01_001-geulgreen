package com.team.blog.shared.security;

import org.springframework.security.authentication.AccountStatusException;

/** 10-21 chan: 14세 미만 계정이 아직 보호자 동의를 받지 못함 → 로그인 거부 + 안내. */
public class GuardianPendingAuthenticationException extends AccountStatusException {

    public GuardianPendingAuthenticationException() {
        super("GUARDIAN_PENDING");
    }
}
