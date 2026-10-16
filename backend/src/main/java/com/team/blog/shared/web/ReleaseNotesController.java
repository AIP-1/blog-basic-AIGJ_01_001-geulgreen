package com.team.blog.shared.web;

import com.team.blog.post.markdown.ContentRenderer;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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

    private static final Pattern VERSION = Pattern.compile("\\A## \\[([0-9.]+)]");
    /** 처음과 한 번 더 불러올 때 보여 줄 버전 수(110, 강성찬 개인 확장). */
    static final int PAGE = 10;

    private final ContentRenderer renderer;
    private final RestClient github;
    private volatile Notes notes; // 배포마다 jar가 바뀌므로 처음 한 번만 렌더링

    /** 머리말과 버전마다 하나씩 그린 조각(id="v1.60.0"인 section). */
    record Notes(String intro, List<String> versions, List<String> sections) {
    }

    public ReleaseNotesController(ContentRenderer renderer) {
        this.renderer = renderer;
        Duration timeout = Duration.ofSeconds(10);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build());
        factory.setReadTimeout(timeout);
        this.github = RestClient.builder().requestFactory(factory).baseUrl(ASSET_BASE).build();
    }

    /**
     * 112 강성찬 개인 확장: 처음에는 최신 {@value #PAGE}개만 그리고, 나머지는 스크롤하면 {@code /releases/more}로 붙인다.
     * 스크립트가 없으면 [더 보기]가 {@code ?upto=}로 더 많이 그린다.
     */
    @GetMapping("/releases")
    public String releases(@RequestParam(defaultValue = "" + PAGE) int upto, Model model) {
        Notes n = notes();
        int shown = Math.min(Math.max(PAGE, upto), n.sections().size());
        model.addAttribute("releaseIntro", n.intro());
        model.addAttribute("releaseHtml", String.join("", n.sections().subList(0, shown)));
        model.addAttribute("releaseNext", shown < n.sections().size() ? shown : null);
        // 스크립트 없이 [더 보기]를 누르면 새로 보인 첫 버전으로 내려간다
        model.addAttribute("releaseNextId", shown < n.sections().size() ? "v" + n.versions().get(shown) : null);
        return "releases";
    }

    /** 다음 묶음(조각 HTML). 끝이면 빈 글. */
    @GetMapping(value = "/releases/more", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String more(@RequestParam int from) {
        List<String> sections = notes().sections();
        int start = Math.max(0, Math.min(from, sections.size()));
        return String.join("", sections.subList(start, Math.min(start + PAGE, sections.size())));
    }

    private Notes notes() {
        if (notes == null) {
            // 제목(# 릴리스 노트)은 화면이 h1로 따로 둔다. "025~036" 같은 범위가 취소선이 되지 않게 ~를 글자로 바꾼다.
            // ponytail: CHANGELOG에 취소선(~~)이나 코드 속 ~를 쓰기 시작하면 이 치환을 다시 볼 것
            String md = readChangelog().replaceFirst("\\A# [^\\n]*\\n", "").replace("~", "\\~");
            String[] parts = md.split("(?m)^(?=## \\[)");
            String intro = parts[0].startsWith("## [") ? "" : render(parts[0]);
            List<String> versions = new ArrayList<>();
            List<String> sections = new ArrayList<>();
            for (String part : parts) {
                Matcher v = VERSION.matcher(part);
                if (v.find()) {
                    versions.add(v.group(1));
                    sections.add("<section class=\"release\" id=\"v" + v.group(1) + "\">" + render(part) + "</section>");
                }
            }
            notes = new Notes(intro, List.copyOf(versions), List.copyOf(sections));
        }
        return notes;
    }

    private String render(String md) {
        String withTokens = ASSET_IMAGE.matcher(md).replaceAll("RELIMG-$1-$2-RELIMG");
        return TOKEN.matcher(renderer.render(withTokens).html())
                .replaceAll("<img src=\"/releases/img/$1/$2\" alt=\"\" loading=\"lazy\">");
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
