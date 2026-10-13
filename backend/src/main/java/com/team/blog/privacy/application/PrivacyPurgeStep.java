package com.team.blog.privacy.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 비공개 설정·팔로우 요청(073, 팔로우 정리 60 앞). */
@Component
public class PrivacyPurgeStep implements WithdrawalPurgeStep {

    private final PrivateAccountService privacy;

    public PrivacyPurgeStep(PrivateAccountService privacy) {
        this.privacy = privacy;
    }

    @Override
    public int order() {
        return 56;
    }

    @Override
    public void purge(long memberId) {
        privacy.purgeWithdrawn(memberId);
    }
}
