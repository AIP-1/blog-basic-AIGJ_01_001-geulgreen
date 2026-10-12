package com.team.blog.schedule.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 042: 1분마다 시각이 된 예약 발행(테스트는 끈다). */
@Component
@ConditionalOnProperty(name = "blog.schedule.job-enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleJob {

    private final ScheduleService schedules;

    public ScheduleJob(ScheduleService schedules) {
        this.schedules = schedules;
    }

    @Scheduled(fixedDelayString = "${blog.schedule.job-delay:60s}", initialDelayString = "30s")
    public void run() {
        if (schedules.enabled()) {
            schedules.runDue();
        }
    }
}
