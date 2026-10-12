package com.team.blog.guestbook.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 방명록 삭제. */
@Component
public class GuestbookPurgeStep implements WithdrawalPurgeStep {

    private final GuestbookService guestbook;

    public GuestbookPurgeStep(GuestbookService guestbook) {
        this.guestbook = guestbook;
    }

    @Override
    public int order() {
        return 18;
    }

    @Override
    public void purge(long memberId) {
        guestbook.purgeWithdrawn(memberId);
    }
}
