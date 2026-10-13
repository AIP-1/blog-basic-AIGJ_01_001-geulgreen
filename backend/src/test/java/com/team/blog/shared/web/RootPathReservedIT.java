package com.team.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.account.application.HandleService;
import com.team.blog.account.domain.HandleRules;
import com.team.blog.support.IntegrationTestBase;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 076: 블로그 주소가 {@code /{handle}}이라, 사이트의 맨 앞 경로 칸(컨트롤러·정적 파일 폴더·보안/운영 경로)은 모두 예약어여야 한다.
 * 새 맨 앞 경로를 만들고 {@code blog.account.handle.reserved}에 넣지 않으면 여기서 실패한다.
 */
class RootPathReservedIT extends IntegrationTestBase {

    /** 컨트롤러가 아닌 곳에서 오는 맨 앞 경로(Spring Security·OAuth2·운영). */
    private static final List<String> FRAMEWORK_ROOTS = List.of("login", "logout", "oauth2", "error", "actuator");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapping;

    @Autowired
    HandleService handleService;

    /** 정적 자원 경로({@code /webjars/**}, {@code /fonts/**} 등). */
    @Autowired
    List<org.springframework.web.servlet.handler.AbstractUrlHandlerMapping> urlMappings;

    @Test
    void everyRootPathThatLooksLikeAHandleIsReserved() throws Exception {
        Set<String> roots = new TreeSet<>(FRAMEWORK_ROOTS);
        mapping.getHandlerMethods().keySet().forEach(info -> info.getPatternValues().forEach(p -> roots.add(first(p))));
        urlMappings.forEach(m -> m.getHandlerMap().keySet().forEach(p -> roots.add(first(p))));
        for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath*:static/*")) {
            roots.add(r.getFilename());
        }
        List<String> missing = roots.stream().filter(s -> s != null && !s.startsWith("@") && !s.startsWith("{"))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(HandleRules::isValidFormat)
                .filter(s -> !handleService.isRouteOrReserved(s))
                .toList();
        assertThat(missing).as("blog.account.handle.reserved에 넣어야 할 맨 앞 경로").isEmpty();
    }

    private static String first(String pattern) {
        String p = pattern.startsWith("/") ? pattern.substring(1) : pattern;
        int slash = p.indexOf('/');
        return slash < 0 ? p : p.substring(0, slash);
    }
}
