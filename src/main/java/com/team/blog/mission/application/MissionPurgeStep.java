package com.team.blog.mission.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 미션 글 연결과 034 때의 미션(글 정리 10 다음). */
@Component
public class MissionPurgeStep implements WithdrawalPurgeStep {

    private final MissionService missions;

    public MissionPurgeStep(MissionService missions) {
        this.missions = missions;
    }

    @Override
    public int order() {
        return 16;
    }

    @Override
    public void purge(long memberId) {
        missions.purgeWithdrawn(memberId);
    }
}
