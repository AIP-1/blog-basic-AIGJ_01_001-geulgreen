package com.team.blog.shared.web;

import com.team.blog.post.markdown.ContentRenderer;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * 릴리스 노트(071, 강성찬 개인 확장). 빌드 때 jar에 넣은 CHANGELOG.md를 글 본문과 같은 렌더러로 보여 준다.
 * 전/후 사진은 GitHub Release 첨부라 글 렌더러가 외부 이미지로 막는다: 우리 주소(/releases/img/…)로 바꿔 <img>로 넣고,
 * 그 주소는 GitHub에서 받아 그대로 내준다(CSP는 'self' 그대로).
 */
@Controller
public class ReleaseNotesController {

    static final String ASSET_BASE = "https://github.com/practicech/blog/releases/download/";
    private static final Pattern ASSET_IMAGE = Pattern.compile(
            "!\\[\\]\\(" + Pattern.quote(ASSET_BASE) + "(v\\d+\\.\\d+\\.\\d+)/([A-Za-z0-9._-]+\\.(?:png|jpg))\\)");
    private static final Pattern TOKEN = Pattern.compile("RELIMG-(v\\d+\\.\\d+\\.\\d+)-([A-Za-z0-9._-]+\\.(?:png|jpg))-RELIMG");

    private final ContentRenderer renderer;
    private final RestClient github;
    private volatile String html; // 배포마다 jar가 바뀌므로 처음 한 번만 렌더링

    public ReleaseNotesController(ContentRenderer renderer) {
        this.renderer = renderer;
        Duration timeout = Duration.ofSeconds(10);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build());
        factory.setReadTimeout(timeout);
        this.github = RestClient.builder().requestFactory(factory).baseUrl(ASSET_BASE).build();
    }

    @GetMapping("/releases")
    public String releases(Model model) {
        if (html == null) {
            // 제목(# 릴리스 노트)은 화면이 h1로 따로 둔다. "025~036" 같은 범위가 취소선이 되지 않게 ~를 글자로 바꾼다.
            // ponytail: CHANGELOG에 취소선(~~)이나 코드 속 ~를 쓰기 시작하면 이 치환을 다시 볼 것
            String md = readChangelog().replaceFirst("\\A# [^\\n]*\\n", "").replace("~", "\\~");
            md = ASSET_IMAGE.matcher(md).replaceAll("RELIMG-$1-$2-RELIMG");
            html = TOKEN.matcher(renderer.render(md).html())
                    .replaceAll("<img src=\"/releases/img/$1/$2\" alt=\"\" loading=\"lazy\">");
        }
        model.addAttribute("releaseHtml", html);
        return "releases";
    }

    /** 릴리스 첨부 사진. 태그·파일 이름은 위 모양만 받는다(다른 주소로 못 가게). 한 번 올린 첨부는 바뀌지 않아 오래 캐시한다. */
    @GetMapping("/releases/img/{tag:v\\d+\\.\\d+\\.\\d+}/{file:[A-Za-z0-9._-]+\\.(?:png|jpg)}")
    public ResponseEntity<byte[]> image(@PathVariable String tag, @PathVariable String file) {
        byte[] body;
        try {
            body = github.get().uri("{tag}/{file}", tag, file).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok().contentType(file.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()).body(body);
    }

    private static String readChangelog() {
        ClassPathResource file = new ClassPathResource("releases/CHANGELOG.md");
        if (!file.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try (InputStream in = file.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("CHANGELOG.md를 읽지 못함", e);
        }
    }
}
