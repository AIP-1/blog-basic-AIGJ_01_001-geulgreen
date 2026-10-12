package com.team.blog.mission.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 057: 매일 0시 1분(한국 시간) 오늘의 주제를 AI로 만든다(AI가 없으면 내장 주제). 테스트는 끈다. */
@Component
@ConditionalOnProperty(name = "blog.missions.topic-job-enabled", havingValue = "true", matchIfMissing = true)
public class DailyTopicJob {

    private final MissionService missions;

    public DailyTopicJob(MissionService missions) {
        this.missions = missions;
    }

    @Scheduled(cron = "${blog.missions.topic-cron:0 1 0 * * *}", zone = "Asia/Seoul")
    public void run() {
        if (missions.enabled()) {
            missions.prepareWithAi(missions.today());
        }
    }
}
