package com.team.blog.connect.web;

import com.team.blog.account.infra.RedisRateLimiter;
import com.team.blog.connect.application.AiConnectProperties;
import com.team.blog.connect.application.AiKeyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * 097 {@code /mcp} 전용 보안 체인(기본 체인보다 먼저). 쿠키·세션·CSRF 없이 연결 키로만 들어온다 — 브라우저 쿠키로는
 * 부를 수 없으니 CSRF 걱정이 없고, CORS를 열지 않아 다른 사이트 스크립트는 키 헤더를 붙여 보낼 수 없다.
 */
@Configuration(proxyBeanMethods = false)
public class McpSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http, AiKeyService keys, AiConnectProperties properties,
                                                      RedisRateLimiter rateLimiter) {
        http.securityMatcher("/mcp")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .requestCache(cache -> cache.disable())
                .addFilterBefore(new McpKeyFilter(keys, properties, rateLimiter), AuthorizationFilter.class)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        return http.build();
    }
}
