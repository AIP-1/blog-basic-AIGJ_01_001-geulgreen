package com.team.blog.shared.event;

/** 090 새 문의가 들어옴(강성찬 개인 확장): 알림이 관리자에게 알린다. */
public record InquiryReceived(long inquiryId) implements DomainEvent {
}
