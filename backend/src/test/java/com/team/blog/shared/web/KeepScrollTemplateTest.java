package com.team.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 누르면 맨 위로 튀는 버튼을 막는다(강성찬 개인 확장): 빈 링크(#·javascript:)를 버튼으로 쓰지 않고, 모든 화면이 keep-scroll.js를 불러온다. */
class KeepScrollTemplateTest {

    private static final Path TEMPLATES = Path.of("../frontend/src/main/resources/templates");
    private static final Pattern DEAD_HREF = Pattern.compile("href\\s*=\\s*\"\\s*(#?|javascript:[^\"]*|@\\{#\\})\\s*\"", Pattern.CASE_INSENSITIVE);

    @Test
    void noDeadAnchorsInTemplates() throws IOException {
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            files.filter(f -> f.toString().endsWith(".html")).forEach(f -> {
                try {
                    assertThat(DEAD_HREF.matcher(Files.readString(f)).find()).as(f + "에 href=\"#\"·javascript: 링크").isFalse();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    @Test
    void layoutLoadsKeepScroll() throws IOException {
        assertThat(Files.readString(TEMPLATES.resolve("layout/base.html"))).contains("/js/keep-scroll.js");
    }
}
