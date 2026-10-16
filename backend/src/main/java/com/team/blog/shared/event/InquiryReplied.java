package com.team.blog.shared.event;

/** 099 관리자가 회원의 문의에 답장함(강성찬 개인 확장): 알림이 보낸 회원에게 알린다. */
public record InquiryReplied(long inquiryId, long memberId) implements DomainEvent {
}
