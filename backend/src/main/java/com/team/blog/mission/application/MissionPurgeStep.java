package com.team.blog.mission.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 미션 글 연결과 034 때의 미션, 111 모임(글 정리 10 다음). */
@Component
public class MissionPurgeStep implements WithdrawalPurgeStep {

    private final MissionService missions;
    private final ClubService clubs;

    public MissionPurgeStep(MissionService missions, ClubService clubs) {
        this.missions = missions;
        this.clubs = clubs;
    }

    @Override
    public int order() {
        return 16;
    }

    @Override
    public void purge(long memberId) {
        missions.purgeWithdrawn(memberId);
        clubs.purgeWithdrawn(memberId);
    }
}
