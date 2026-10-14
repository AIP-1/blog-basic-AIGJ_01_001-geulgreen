package com.team.blog.shared.event;

/** 096 강성찬 개인 확장: 새 신고가 들어왔다(같은 사람이 다시 신고한 것은 제외). 텔레그램 운영 알림용. */
public record ReportReceived(String targetType, String reasonLabel) implements DomainEvent {
}
