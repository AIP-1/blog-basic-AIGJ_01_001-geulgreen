package com.team.blog.series.application;

import com.team.blog.account.application.WithdrawalPurgeStep;
import org.springframework.stereotype.Component;

/** 탈퇴 정리: 시리즈 삭제(글 정리 10 다음). */
@Component
public class SeriesPurgeStep implements WithdrawalPurgeStep {

    private final SeriesService series;

    public SeriesPurgeStep(SeriesService series) {
        this.series = series;
    }

    @Override
    public int order() {
        return 19;
    }

    @Override
    public void purge(long memberId) {
        series.purgeWithdrawn(memberId);
    }
}
