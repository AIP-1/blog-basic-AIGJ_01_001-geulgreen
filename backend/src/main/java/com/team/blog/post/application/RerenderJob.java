package com.team.blog.post.application;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일, 그리고 104부터 배포해서 켜질 때도 다시 렌더링(버전을 올린 배포 뒤 남은 글 정리). 실행 잠금으로 서버 한 대만. 테스트는 끈다. */
@Component
@ConditionalOnProperty(name = "blog.markdown.rerender.enabled", havingValue = "true", matchIfMissing = true)
public class RerenderJob {

    static final String LOCK = "post:rerender-lock";

    private final RerenderService rerenderService;
    private final JobLock jobLock;

    public RerenderJob(RerenderService rerenderService, JobLock jobLock) {
        this.rerenderService = rerenderService;
        this.jobLock = jobLock;
    }

    @Scheduled(cron = "${blog.markdown.rerender.cron:0 10 5 * * *}", zone = "Asia/Seoul")
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        jobLock.runExclusively(LOCK, Duration.ofMinutes(30), rerenderService::runAll);
    }
}
