package com.team.blog.connect.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: AI 연결 키 삭제(097). 글 정리보다 먼저 키부터 막는다. */
@Component
public class AiKeyPurgeStep implements WithdrawalPurgeStep {

    private final AiKeyService keys;

    public AiKeyPurgeStep(AiKeyService keys) {
        this.keys = keys;
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public void purge(long memberId) {
        keys.purgeWithdrawn(memberId);
    }
}
