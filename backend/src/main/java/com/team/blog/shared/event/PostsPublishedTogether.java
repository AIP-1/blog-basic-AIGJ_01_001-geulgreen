package com.team.blog.shared.event;

import java.util.List;

/**
 * 113 후속 강성찬 개인 확장: 내 글에서 임시글 여러 개를 한 번에 발행했다. 팔로워에게는 글마다가 아니라 한 번만 알린다.
 * {@code publicPostIds}는 전체 공개로 발행된 글(발행 순서대로).
 */
public record PostsPublishedTogether(long authorId, List<Long> publicPostIds) implements DomainEvent {

    private static final ThreadLocal<Boolean> BATCH = ThreadLocal.withInitial(() -> false);

    /** 이 스레드가 한 번에 발행하는 중인지(그동안 글마다 오는 {@link PostPublished}의 새 글 알림은 건너뛴다). */
    public static boolean batching() {
        return BATCH.get();
    }

    public static void begin() {
        BATCH.set(true);
    }

    public static void end() {
        BATCH.remove();
    }
}
