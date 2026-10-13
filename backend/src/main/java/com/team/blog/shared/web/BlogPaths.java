package com.team.blog.shared.web;

import java.util.Set;
/**
 * 076 강성찬 개인 확장: 블로그 주소 링크를 만드는 한 곳. 보통은 {@code /{handle}}, 사이트 경로·예약어와 같은 아이디(076 전에 생긴 것)는
 * 그 경로가 사이트 화면이라 예전 {@code /@{handle}}을 쓴다. 화면에서는 {@code ${paths.blog(handle)}}.
 */
public class BlogPaths {

    private static volatile Set<String> reserved = Set.of();

    /** {@code HandleService}가 시작할 때 예약어(사이트 맨 앞 경로 포함)를 넣는다. */
    public static void reserve(Set<String> words) {
        reserved = Set.copyOf(words);
    }

    public static boolean needsAt(String handle) {
        return handle != null && reserved.contains(handle);
    }

    public static String of(Object handle) {
        String h = String.valueOf(handle);
        return (needsAt(h) ? "/@" : "/") + h;
    }

    /** 템플릿용({@code ${paths.blog(h)}}, LayoutModelAdvice). */
    public String blog(String handle) {
        return of(handle);
    }
}
