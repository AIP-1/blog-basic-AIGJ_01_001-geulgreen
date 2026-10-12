package com.team.blog.mission.application;

import com.team.blog.shared.event.PostPublished;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 057: 글이 처음 발행되면 미션 글인지 보고 완료로 적는다. */
@Component
public class MissionListeners {

    private final MissionService missions;

    public MissionListeners(MissionService missions) {
        this.missions = missions;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PostPublished e) {
        if (missions.enabled()) {
            missions.onPublished(e.postId(), e.authorId(), e.visibility());
        }
    }
}
