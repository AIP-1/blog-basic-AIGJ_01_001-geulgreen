package com.team.blog.shared.event;

/** 089 강성찬 개인 확장: 댓글 좋아요가 실제로 지워짐. */
public record CommentUnliked(long commentId, long likerId, long authorId) implements DomainEvent {
}
