package com.team.blog.series.application;

import com.team.blog.shared.event.PostPublished;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 113 강성찬 개인 확장: 발행(한 글이든 여러 글 한꺼번에든 같은 사건)되면 미션 글을 글쓴이의 미션 시리즈에 넣는다. */
@Component
public class SeriesListeners {

    private final SeriesService series;

    public SeriesListeners(SeriesService series) {
        this.series = series;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PostPublished e) {
        if (series.enabled()) {
            series.addMissionPost(e.authorId(), e.postId());
        }
    }
}
