package com.team.blog.post.application;

import com.team.blog.account.application.ProfileAvatar;
import java.time.Instant;

/** 글 카드(10 §2). 본문 칸은 읽지 않는다. */
public record PostCard(long id, String url, String title, String excerpt, String thumbnailUrl, Instant firstPublicAt,
                       int commentCount, int likeCount, Author author, String firstTag) {

    public record Author(String handle, String nickname, String profileImageUrl) {

        /** 076 후속(강성찬 개인 확장): 블로그 주소(스크립트가 그리는 링크용, 예약어와 같은 옛 아이디는 /@). */
        @com.fasterxml.jackson.annotation.JsonProperty("blogUrl")
        public String blogUrl() {
            return com.team.blog.shared.web.BlogPaths.of(handle);
        }

        public ProfileAvatar avatar() {
            return new ProfileAvatar(handle, nickname, profileImageUrl);
        }
    }
}
