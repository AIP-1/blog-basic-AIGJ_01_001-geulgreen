package com.team.blog.tag.application.icon;

import com.team.blog.post.application.JobLock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 056: 30분마다 AI로 태그 아이콘 다듬기(AI 연결이 없으면 아무것도 안 함, 테스트는 끈다). */
@Component
@ConditionalOnProperty(name = "blog.tag-icons.ai-job-enabled", havingValue = "true", matchIfMissing = true)
public class TagIconAiJob {

    private final TagIconService icons;
    private final JobLock jobLock;

    public TagIconAiJob(TagIconService icons, JobLock jobLock) {
        this.icons = icons;
        this.jobLock = jobLock;
    }

    @Scheduled(fixedDelayString = "${blog.tag-icons.ai-delay:30m}", initialDelayString = "2m")
    public void run() {
        jobLock.runExclusively("tag-icons:ai-lock", Duration.ofMinutes(20), icons::refineWithAi);
    }
}
