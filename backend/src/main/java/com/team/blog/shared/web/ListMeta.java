package com.team.blog.shared.web;

import java.util.List;

/**
 * 글이 아닌 화면(태그·시리즈·블로그 홈)의 검색·미리보기 문구. 레이아웃이 모델의 {@code listMeta}로 기본 문구 대신 그린다.
 * 공개 글에서 나온 값만 넣는다.
 *
 * @param imageUrl 없으면 null(기본 그림). {@code /}로 시작하면 레이아웃이 사이트 주소를 붙인다.
 */
public record ListMeta(String title, String description, String imageUrl) {

    /** 검색 결과에 보통 보이는 길이. */
    static final int DESCRIPTION_MAX = 150;

    public ListMeta {
        description = trim(description);
    }

    /** 공백을 한 칸으로 모으고 너무 길면 자른다. */
    static String trim(String text) {
        String t = text == null ? "" : text.strip().replaceAll("\\s+", " ");
        return t.codePointCount(0, t.length()) <= DESCRIPTION_MAX ? t
                : t.substring(0, t.offsetByCodePoints(0, DESCRIPTION_MAX - 1)) + "…";
    }

    /** "「가」, 「나」, 「다」"처럼 앞의 글 제목 몇 개. */
    public static String titles(List<String> titles, int max) {
        return String.join(", ", titles.stream().limit(max).map(t -> "「" + t + "」").toList());
    }
}
