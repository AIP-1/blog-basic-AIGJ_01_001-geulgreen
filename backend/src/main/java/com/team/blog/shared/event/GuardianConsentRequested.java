package com.team.blog.shared.event;

import java.time.Instant;

/** 10-21 chan: 14세 미만 가입 — 보호자에게 동의 메일을 보낸다(커밋 후). {@code rawToken}은 메일 링크에만 쓴다. */
public record GuardianConsentRequested(long memberId, String guardianEmail, String guardianName, String childNickname,
                                       String rawToken, Instant expiresAt) implements DomainEvent {

    @Override
    public String toString() {
        return "GuardianConsentRequested[memberId=" + memberId + "]";
    }
}
