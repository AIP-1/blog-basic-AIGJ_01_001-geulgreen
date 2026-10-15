package com.team.blog.post.application;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team.blog.post.domain.PostStatus;
import java.time.Instant;

/**
 * 내 글 관리 한 줄(41 §3). 본문 칸은 읽지 않는다. {@code url}은 발행 글 보기 주소.
 * 067 후속(강성찬 개인 확장): 홈과 같은 카드로 보이도록 요약·대표 사진·첫 태그를 함께 읽는다(화면 전용, API에는 내보내지 않음).
 */
public record ManageRow(long id, String title, PostStatus status, String visibility, boolean editing, boolean hidden,
                        Instant updatedAt, Instant publishedAt, Instant editedAt, Instant deletedAt, Instant purgeAt,
                        long viewCount, int likeCount, int commentCount, String url,
                        @JsonIgnore String excerpt, @JsonIgnore String thumbnailUrl, @JsonIgnore String firstTag) {

    /** 홈 카드 조각(fragments/post-card)에 넘길 모양. 작성자는 보이지 않으므로 비운다. */
    public PostCard card(String href) {
        // 휴지통 줄만 본문 미리보기를 읽으므로(PostManageQuery) 빈 글 판정도 휴지통에서만
        boolean emptyTrashed = deletedAt != null && status == PostStatus.DRAFT && (excerpt == null || excerpt.isBlank());
        String shown;
        if (title != null && !title.isBlank()) {
            shown = title;
        } else {
            shown = emptyTrashed ? "빈 임시글" : "(제목 없음)";
        }
        return new PostCard(id, href, shown, excerpt == null ? "" : excerpt, thumbnailUrl, publishedAt, commentCount, likeCount,
                null, firstTag);
    }
}
