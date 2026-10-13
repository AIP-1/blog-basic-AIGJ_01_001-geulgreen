package com.team.blog.shared.event;

import java.time.Instant;

/** 비공개 계정에 팔로우 요청이 새로 생김(073). 받는 사람에게 알림(017)이 간다. */
public record FollowRequested(long requesterId, long targetId, Instant requestedAt) implements DomainEvent {
}
