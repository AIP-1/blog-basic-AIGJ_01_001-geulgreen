package com.team.blog.shared.event;

/** 대기 중이던 팔로우 요청이 끝남(수락·거절·취소, 073). 받은 사람의 요청 알림에서 그 사람을 뺄 뿐 새 알림은 없다. */
public record FollowRequestClosed(long requesterId, long targetId) implements DomainEvent {
}
