package com.team.blog.post.application;

import java.util.Locale;

/** 내 글 관리 탭(41 §2). 063 강성찬 개인 확장: 기본은 발행 글(원래 임시글). */
public enum ManageTab {
    PUBLISHED, DRAFTS, TRASH;

    public static ManageTab of(String raw) {
        if (raw == null) {
            return PUBLISHED;
        }
        try {
            return valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PUBLISHED;
        }
    }

    public String param() {
        return name().toLowerCase(Locale.ROOT);
    }
}
