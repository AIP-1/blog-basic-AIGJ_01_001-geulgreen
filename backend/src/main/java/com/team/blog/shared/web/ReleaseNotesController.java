package com.team.blog.shared.web;

import com.team.blog.post.markdown.ContentRenderer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** 릴리스 노트(071, 강성찬 개인 확장). 빌드 때 jar에 넣은 CHANGELOG.md를 글 본문과 같은 렌더러로 보여 준다. */
@Controller
public class ReleaseNotesController {

    private final ContentRenderer renderer;
    private volatile String html; // 배포마다 jar가 바뀌므로 처음 한 번만 렌더링

    public ReleaseNotesController(ContentRenderer renderer) {
        this.renderer = renderer;
    }

    @GetMapping("/releases")
    public String releases(Model model) {
        if (html == null) {
            // 제목(# 릴리스 노트)은 화면이 h1로 따로 둔다. "025~036" 같은 범위가 취소선이 되지 않게 ~를 글자로 바꾼다.
            // ponytail: CHANGELOG에 취소선(~~)이나 코드 속 ~를 쓰기 시작하면 이 치환을 다시 볼 것
            String md = readChangelog().replaceFirst("\\A# [^\\n]*\\n", "").replace("~", "\\~");
            html = renderer.render(md).html();
        }
        model.addAttribute("releaseHtml", html);
        return "releases";
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
