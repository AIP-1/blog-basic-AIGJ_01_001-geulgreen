package com.team.blog.shared.web;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 098 후속 관리자 목록 한 쪽(강성찬 개인 확장): 검색·필터는 각 조회의 WHERE로, 쪽은 LIMIT·OFFSET으로 DB에서 나누고 건수는 count로 센다.
 * 이전·다음 쪽 주소는 지금 주소에서 page만 바꿔 만든다(검색·필터 유지).
 */
public record AdminList<T>(List<T> rows, int page, int pages, long total, String prevUrl, String nextUrl) {

    static final int SIZE = 20;

    /** {@code sql}은 ORDER BY·LIMIT 없는 SELECT(WHERE까지), {@code orderBy}는 정렬, {@code args}는 sql의 ? 값. */
    public static <T> AdminList<T> query(JdbcTemplate jdbc, String sql, String orderBy, RowMapper<T> mapper, int page, Object... args) {
        Long counted = jdbc.queryForObject("SELECT count(*) FROM (" + sql + ") counted", Long.class, args);
        long total = counted == null ? 0 : counted;
        int pages = (int) Math.max(1, (total + SIZE - 1) / SIZE);
        int p = Math.min(Math.max(page, 1), pages);
        List<Object> all = new ArrayList<>(List.of(args));
        all.add((p - 1) * SIZE);
        List<T> rows = jdbc.query(sql + " ORDER BY " + orderBy + " LIMIT " + SIZE + " OFFSET ?", mapper, all.toArray());
        return new AdminList<>(rows, p, pages, total, p > 1 ? url(p - 1) : null, p < pages ? url(p + 1) : null);
    }

    /** ILIKE ... ESCAPE '\' 용 패턴(빈 값이면 모두). */
    public static String like(String q) {
        return "%" + (q == null ? "" : q.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")) + "%";
    }

    private static String url(int page) {
        var u = ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("page", page).build();
        return u.getPath() + "?" + u.getQuery(); // 호스트 없이(프록시 뒤 http/https 섞임 방지)
    }

    public boolean empty() {
        return rows.isEmpty();
    }
}
