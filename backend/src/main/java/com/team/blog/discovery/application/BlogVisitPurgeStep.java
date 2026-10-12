package com.team.blog.discovery.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 방문자 수·고정 글(051·054, 강성찬 개인 확장). */
@Component
public class BlogVisitPurgeStep implements WithdrawalPurgeStep {

    private final JdbcTemplate jdbc;

    public BlogVisitPurgeStep(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int order() {
        return 14;
    }

    @Override
    public void purge(long memberId) {
        jdbc.update("DELETE FROM blog_visit_daily WHERE member_id = ?", memberId);
        jdbc.update("DELETE FROM post_pin WHERE member_id = ?", memberId);
    }
}
