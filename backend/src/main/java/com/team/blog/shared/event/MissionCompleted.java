package com.team.blog.shared.event;

/** 057 오늘의 미션 글이 처음 발행됨(강성찬 개인 확장): 알림이 친구에게 알린다. 전체 공개·친구 공개 글만 낸다. */
public record MissionCompleted(long postId, long authorId) implements DomainEvent {
}
