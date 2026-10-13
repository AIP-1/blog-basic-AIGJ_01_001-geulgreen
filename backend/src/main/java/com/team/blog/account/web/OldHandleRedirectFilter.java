package com.team.blog.account.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 074 강성찬 개인 확장: 바꾸기 전 주소({@code /@옛주소/...})로 오면 지금 주소로 302. 그 주소를 다른 회원이 쓰고 있거나
 * 주인이 탈퇴했으면 넘기지 않는다(평소처럼 그 블로그나 404). 끄기 스위치를 꺼도 이미 바꾼 주소는 계속 넘긴다.
 */
@Component
public class OldHandleRedirectFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbc;

    public OldHandleRedirectFilter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return !request.getRequestURI().startsWith("/@") || !("GET".equals(method) || "HEAD".equals(method));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        int slash = uri.indexOf('/', 2);
        String old = slash < 0 ? uri.substring(2) : uri.substring(2, slash);
        if (old.length() <= 39) {
            List<String> current = jdbc.queryForList("""
                    SELECT m.handle FROM handle_history h JOIN member m ON m.id = h.member_id
                    WHERE h.old_handle = ? AND m.status <> 'WITHDRAWN'
                      AND NOT EXISTS (SELECT 1 FROM member o WHERE o.handle = h.old_handle)""", String.class, old);
            if (!current.isEmpty()) {
                String query = request.getQueryString();
                response.sendRedirect(com.team.blog.shared.web.BlogPaths.of(current.get(0)) + (slash < 0 ? "" : uri.substring(slash))
                        + (query == null ? "" : "?" + query));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
