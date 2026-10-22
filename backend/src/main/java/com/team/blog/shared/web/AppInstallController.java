package com.team.blog.shared.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

/**
 * 앱 설치(강성찬 개인 확장, PWA): 휴대폰 홈 화면에 앱처럼 설치하는 웹 앱 정보(manifest).
 * 아이콘 주소는 내용 지문이 붙은 주소(/images/app-icon-192-3f2a….png)로 내보내 아이콘을 바꾸면 주소도 바뀌게 한다.
 * 색 값은 theme.css(밝은 테마)와 같다: 시작 화면 배경은 --color-bg, 상단 막대는 머리글의 --color-surface. manifest는 CSS 변수를 못 읽어 값을 그대로 적는다.
 */
@RestController
public class AppInstallController {

    static final MediaType MANIFEST = MediaType.parseMediaType("application/manifest+json");
    private static final String BG = "#FBF7F1";
    private static final String SURFACE = "#FFFFFF";

    private final ResourceUrlProvider resourceUrls;

    public AppInstallController(ResourceUrlProvider resourceUrls) {
        this.resourceUrls = resourceUrls;
    }

    @GetMapping("/manifest.webmanifest")
    public ResponseEntity<Map<String, Object>> manifest() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", "/");
        m.put("name", "글그린");
        m.put("short_name", "글그린");
        m.put("description", "좋은 글이 머무는 곳, 글그린. 매일 쓴 글이 잔디처럼 쌓이는 블로그예요.");
        m.put("lang", "ko");
        m.put("dir", "ltr");
        m.put("start_url", "/");
        m.put("scope", "/");
        m.put("display", "standalone");
        m.put("background_color", BG);
        m.put("theme_color", SURFACE);
        m.put("categories", List.of("social", "education", "lifestyle"));
        m.put("icons", List.of(
                icon("/images/app-icon-192.png", "192x192", "any"),
                icon("/images/app-icon-512.png", "512x512", "any"),
                icon("/images/app-icon-maskable-512.png", "512x512", "maskable")));
        // 홈 화면 아이콘을 길게 누르면 나오는 바로 가기
        m.put("shortcuts", List.of(
                Map.of("name", "새 글 쓰기", "short_name", "새 글", "url", "/write"),
                Map.of("name", "오늘의 글감", "short_name", "글감", "url", "/missions")));
        return ResponseEntity.ok().contentType(MANIFEST).cacheControl(CacheControl.noCache()).body(m);
    }

    private Map<String, Object> icon(String path, String sizes, String purpose) {
        String url = resourceUrls.getForLookupPath(path);
        return Map.of("src", url == null ? path : url, "sizes", sizes, "type", "image/png", "purpose", purpose);
    }
}
