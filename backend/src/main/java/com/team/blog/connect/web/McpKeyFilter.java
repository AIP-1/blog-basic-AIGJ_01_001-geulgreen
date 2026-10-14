package com.team.blog.connect.web;

import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.connect.application.AiConnectProperties;
import com.team.blog.connect.application.AiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 097 {@code /mcp} 앞문: 끈 상태면 404, {@code Authorization: Bearer <키>}가 없거나 틀리면 401, 키마다 분당 요청 제한(429).
 * 맞으면 키 주인을 이 요청에만 로그인시킨다(세션에 저장하지 않음). 빈(Bean)으로 등록하지 않는다 — /mcp 보안 체인에만 건다.
 */
class McpKeyFilter extends OncePerRequestFilter {

    /** 본문 한도: 글 본문 최대(약 1MB)에 JSON 여유. */
    static final long MAX_BODY = 2L * 1024 * 1024;

    private final AiKeyService keys;
    private final AiConnectProperties properties;
    private final RedisRateLimiter rateLimiter;

    McpKeyFilter(AiKeyService keys, AiConnectProperties properties, RedisRateLimiter rateLimiter) {
        this.keys = keys;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.enabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String header = request.getHeader("Authorization");
        String raw = header != null && header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7).strip() : null;
        Optional<AiKeyService.Owner> owner = keys.authenticate(raw);
        if (owner.isEmpty()) {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"geulgreen\"");
            json(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                    "연결 키가 없거나 맞지 않아요. 글그린 설정 > 연결에서 키를 발급해 주세요.");
            return;
        }
        if (request.getContentLengthLong() > MAX_BODY) {
            json(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "PAYLOAD_TOO_LARGE", "요청이 너무 커요.");
            return;
        }
        try {
            RedisRateLimiter.Result r = rateLimiter.tryAcquire(RedisRateLimiter.key("mcp:key", String.valueOf(owner.get().keyId())),
                    properties.perMinute(), Duration.ofMinutes(1));
            if (!r.allowed()) {
                response.setHeader("Retry-After", String.valueOf(r.retryAfterSeconds()));
                json(response, 429, "RATE_LIMITED", "요청이 너무 잦아요. 잠시 뒤 다시 해 주세요.");
                return;
            }
        } catch (DataAccessException e) {
            // Redis 장애면 제한 없이 통과(키 확인은 DB로 끝났다)
        }
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                String.valueOf(owner.get().memberId()), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void json(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
