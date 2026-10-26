package com.team.blog.shared.event;

/** 10-26 보낸 회원이 문의에 다시 답함(강성찬 개인 확장): 알림이 관리자에게 알린다. */
public record InquiryFollowedUp(long inquiryId) implements DomainEvent {
}
