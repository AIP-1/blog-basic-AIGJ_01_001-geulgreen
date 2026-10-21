package com.team.blog.account.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 보호자 이름·이메일(보호자 동의 요청). */
@Component
public class GuardianConsentPurgeStep implements WithdrawalPurgeStep {

    private final JdbcTemplate jdbc;

    public GuardianConsentPurgeStep(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public void purge(long memberId) {
        jdbc.update("DELETE FROM guardian_consent WHERE member_id = ?", memberId);
    }
}
