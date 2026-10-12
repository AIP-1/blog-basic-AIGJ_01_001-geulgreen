package com.team.blog.interaction.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.team.blog.account.application.ProfileAvatar;
import java.time.Instant;
import java.util.List;

/**
 * 댓글 하나의 보이는 모습(21 §6). 상태가 탈퇴·삭제이거나 남이 보는 숨김이면 {@code content}·{@code author}는 null이다(FR-014).
 *
 * @param replyTo  "@닉네임에게" 대상(답글의 답글일 때만, 탈퇴했으면 nickname이 "탈퇴한 사용자")
 * @param replies  최상위일 때 처음 답글 3개(또는 around로 펼친 만큼)
 * @param secret   043 비밀 댓글(강성찬 개인 확장). 볼 수 없는 사람에게는 state가 SECRET이고 내용·글쓴이가 없다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentView(long id, Long parentId, String state, String content, Author author, ReplyTo replyTo,
                          Instant createdAt, boolean edited, boolean mine, boolean canReply, boolean canEdit,
                          boolean canDelete, boolean canReport, Integer replyCount, List<CommentView> replies,
                          String repliesNextCursor, boolean secret) {

    public record Author(String handle, String nickname, String profileImageUrl, boolean isPostAuthor) {

        public ProfileAvatar avatar() {
            return new ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }

    public record ReplyTo(String handle, String nickname) {
    }

    public boolean normal() {
        return "NORMAL".equals(state);
    }
}
