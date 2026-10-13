package com.team.blog.privacy.web;

import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.privacy.application.PrivateAccountService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 비공개 계정(073)의 블로그 아래 주소 전부(글·방명록·시리즈·팔로워 목록·짧은 기록·RSS, 같은 회원의 API)를 수락받지 않은 사람에게 막는다.
 * 블로그 첫 화면(잠긴 화면을 그림)과 팔로우·친구 버튼만 연다. 화면 주소는 블로그 첫 화면으로 보내고, API는 404.
 */
@Configuration
public class PrivateBlogInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private static final Set<String> OPEN = Set.of("", "/follow", "/friend");

    private final BlogOwnerResolver ownerResolver;
    private final PrivateAccountService privacy;
    private final CurrentUserProvider currentUserProvider;

    public PrivateBlogInterceptor(BlogOwnerResolver ownerResolver, PrivateAccountService privacy,
                                  CurrentUserProvider currentUserProvider) {
        this.ownerResolver = ownerResolver;
        this.privacy = privacy;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/@*", "/@*/**", "/api/members/*/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!privacy.enabled()) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean api = path.startsWith("/api/members/");
        String rest = api ? path.substring("/api/members/".length()) : path.substring(2);
        int slash = rest.indexOf('/');
        String handle = URLDecoder.decode(slash < 0 ? rest : rest.substring(0, slash), StandardCharsets.UTF_8);
        String sub = slash < 0 ? "" : rest.substring(slash);
        if (OPEN.contains(sub)) {
            return true;
        }
        var owner = ownerResolver.resolve(handle);
        if (owner.isEmpty() || privacy.canSee(currentUserProvider.current(), owner.get().memberId())) {
            return true;
        }
        if (api) {
            throw new NotFoundException();
        }
        response.sendRedirect(request.getContextPath() + "/" + owner.get().handle());
        return false;
    }
}
