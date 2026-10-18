package com.team.blog.shared.event;

/** 111 후속(강성찬 개인 확장): 함께 쓰기 초대를 보냄({@code invite}) 또는 참여를 요청함. 알림이 받는 사람에게 알린다. */
public record ClubNotice(long clubId, long receiverId, long actorId, boolean invite) implements DomainEvent {
}
