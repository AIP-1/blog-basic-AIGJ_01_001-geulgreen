package com.team.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** 강성찬 개인 확장: css·js 주소에 내용 지문을 붙여 브라우저가 1년 보관하고, 내용이 바뀌면 주소가 바뀌어 새로 받는다. */
class StaticCacheIT extends IntegrationTestBase {

    @Test
    void cssAndJsLinksCarryFingerprintAndAreCachedForAYear() throws Exception {
        String html = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher css = Pattern.compile("href=\"(/css/site-[0-9a-f]{32}\\.css)\"").matcher(html);
        Matcher js = Pattern.compile("src=\"(/js/menu-[0-9a-f]{32}\\.js)\"").matcher(html);
        assertThat(css.find()).isTrue();
        assertThat(js.find()).isTrue();
        for (String url : new String[] {css.group(1), js.group(1)}) {
            mockMvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
        }
        // 지문이 틀리면 옛 파일을 내주지 않는다
        mockMvc.perform(get("/css/site-00000000000000000000000000000000.css")).andExpect(status().isNotFound());
    }

    /** 템플릿에 지문 없는 주소(src="/js/…")를 쓰면 1년 묵은 파일이 남는다. @{...}로 써야 한다. */
    @Test
    void templatesLinkCssAndJsThroughThymeleaf() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("../frontend/src/main/resources/templates"))) {
            files.filter(p -> p.toString().endsWith(".html")).forEach(p -> {
                try {
                    assertThat(Files.readString(p)).as(p.toString()).doesNotContainPattern("[^:](src|href)=\"/(css|js)/");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }
}
