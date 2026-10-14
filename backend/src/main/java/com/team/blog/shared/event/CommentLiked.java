package com.team.blog.shared.event;

/** 089 강성찬 개인 확장: 댓글 좋아요가 실제로 생김. 알림이 커밋 후 구독한다. */
public record CommentLiked(long commentId, long postId, long likerId, long authorId) implements DomainEvent {
}
