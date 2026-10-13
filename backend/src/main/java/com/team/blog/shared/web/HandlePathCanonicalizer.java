package com.team.blog.shared.web;

import com.team.blog.account.application.HandleService;
import com.team.blog.account.domain.HandleRules;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

/**
 * 블로그 주소 경로(가장 앞 순서로 실행).
 * <ul>
 *   <li>076 강성찬 개인 확장: 블로그 주소는 {@code /{handle}/**}(@ 없음). 맨 앞 칸이 주소 형식이고 사이트 경로(예약어)가 아니면
 *       안쪽에서 {@code /@{handle}/**}로 바꿔 넘긴다 — 컨트롤러·인터셉터는 예전 경로 그대로 둔다.</li>
 *   <li>예전 {@code /@{handle}/**} GET·HEAD는 새 주소로 301(나머지 경로·쿼리 유지). 폼 POST는 그대로 받는다.
 *       사이트 경로와 같은 아이디는 {@code /@}로 계속 연다({@link BlogPaths}).</li>
 *   <li>주소 부분에 대문자가 있으면 소문자로 301(08 §6). 존재 여부는 보지 않는다.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HandlePathCanonicalizer extends OncePerRequestFilter {

    private static final String OLD_PREFIX = "/@";

    private final HandleService handleService;

    public HandlePathCanonicalizer(HandleService handleService) {
        this.handleService = handleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI().substring(contextPath.length());
        boolean old = path.startsWith(OLD_PREFIX);
        int start = old ? OLD_PREFIX.length() : 1;
        int end = path.indexOf('/', start);
        String rawSegment = end < 0 ? path.substring(start) : path.substring(start, end);
        String rest = end < 0 ? "" : path.substring(end);
        String segment;
        try {
            segment = UriUtils.decode(rawSegment, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            segment = rawSegment;
        }
        String lower = segment.toLowerCase(Locale.ROOT);
        boolean read = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        if (!old && (!HandleRules.isValidFormat(lower) || handleService.isRouteOrReserved(lower))) {
            chain.doFilter(request, response);
            return;
        }
        // 076 전에 생긴, 사이트 경로와 같은 아이디는 예전 /@주소로 계속 연다(넘기지 않음, 링크도 BlogPaths가 /@로 만든다)
        boolean keepOld = old && handleService.isRouteOrReserved(lower) && lower.equals(segment);
        if (read && !keepOld && (old || !lower.equals(segment))) {
            String query = request.getQueryString();
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader(HttpHeaders.LOCATION, contextPath + BlogPaths.of(UriUtils.encodePathSegment(lower, StandardCharsets.UTF_8))
                    + rest + (query == null ? "" : "?" + query));
            return;
        }
        if (old) {
            chain.doFilter(request, response);
            return;
        }
        String inner = contextPath + OLD_PREFIX + rawSegment + rest;
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return inner;
            }

            @Override
            public StringBuffer getRequestURL() {
                StringBuffer url = new StringBuffer(super.getRequestURL());
                url.setLength(url.length() - super.getRequestURI().length());
                return url.append(inner);
            }

            @Override
            public String getServletPath() {
                return inner.substring(contextPath.length());
            }
        }, response);
    }
}
