package com.team.blog.contact.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 보낸 문의 삭제(답장 주소가 개인정보라서). */
@Component
public class InquiryPurgeStep implements WithdrawalPurgeStep {

    private final InquiryService inquiries;

    public InquiryPurgeStep(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @Override
    public int order() {
        return 15;
    }

    @Override
    public void purge(long memberId) {
        inquiries.purgeWithdrawn(memberId);
    }
}
