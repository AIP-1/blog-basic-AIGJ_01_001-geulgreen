package com.team.blog.shared.web;

import java.util.List;
import java.util.function.Predicate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 098 후속 관리자 목록 한 쪽(강성찬 개인 확장): 걸러 낸 줄을 20줄씩 나누고, 이전·다음 쪽 주소는 지금 주소의 page만 바꿔 만든다.
 * ponytail: 목록은 각 조회의 최근 {@link #CAP}줄 안에서 메모리로 거르고 나눈다(학교 서버 규모). 더 많아지면 SQL OFFSET으로 바꾼다.
 */
public record AdminList<T>(List<T> rows, int page, int pages, int total, boolean capped, String prevUrl, String nextUrl) {

    public static final int CAP = 500;
    static final int SIZE = 20;

    public static <T> AdminList<T> of(List<T> all, Predicate<T> filter, int page) {
        List<T> list = all.stream().filter(filter).toList();
        int pages = Math.max(1, (list.size() + SIZE - 1) / SIZE);
        int p = Math.min(Math.max(page, 1), pages);
        List<T> rows = list.subList((p - 1) * SIZE, Math.min(p * SIZE, list.size()));
        return new AdminList<>(rows, p, pages, list.size(), all.size() >= CAP, p > 1 ? url(p - 1) : null, p < pages ? url(p + 1) : null);
    }

    private static String url(int page) {
        var u = ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("page", page).build();
        return u.getPath() + "?" + u.getQuery(); // 호스트 없이(프록시 뒤 http/https 섞임 방지)
    }

    /** 찾을 말(q)이 비었거나 어느 칸에든 들어 있으면 참(대소문자 무시). */
    public static boolean matches(String q, String... fields) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.strip().toLowerCase(java.util.Locale.ROOT);
        for (String f : fields) {
            if (f != null && f.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public boolean empty() {
        return rows.isEmpty();
    }
}
