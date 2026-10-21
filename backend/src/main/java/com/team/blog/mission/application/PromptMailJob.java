package com.team.blog.mission.application;

import com.team.blog.post.application.JobLock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 10-21: 매일 저녁 8시(한국 시간) 글감 메일. 테스트는 끈다(오늘의 주제 작업과 같은 설정). */
@Component
@ConditionalOnProperty(name = "blog.missions.topic-job-enabled", havingValue = "true", matchIfMissing = true)
public class PromptMailJob {

    private final PromptMailService prompts;
    private final JobLock jobLock;

    public PromptMailJob(PromptMailService prompts, JobLock jobLock) {
        this.prompts = prompts;
        this.jobLock = jobLock;
    }

    @Scheduled(cron = "${blog.missions.prompt-mail-cron:0 0 20 * * *}", zone = "Asia/Seoul")
    public void run() {
        jobLock.runExclusively("mission:prompt-mail-lock", Duration.ofMinutes(30), prompts::sendEvening);
    }
}
