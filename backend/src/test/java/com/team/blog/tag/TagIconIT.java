package com.team.blog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.tag.application.icon.TagIconCatalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 056 태그 화면·태그 아이콘(강성찬 개인 확장). */
class TagIconIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-12T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private void tag(long postId, String name, int position) {
        jdbc.update("INSERT INTO tag (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
        jdbc.update("INSERT INTO post_tag (post_id, tag_id, position) SELECT ?, id, ? FROM tag WHERE name = ?", postId, position, name);
    }

    @Test
    void rulesPickSensibleIcons() {
        assertThat(TagIconCatalog.byRule("일상").icon()).isEqualTo("leaf");
        assertThat(TagIconCatalog.byRule("spring").icon()).isEqualTo("flower");
        assertThat(TagIconCatalog.byRule("개발서적").icon()).isEqualTo("code");
        assertThat(TagIconCatalog.byRule("소설").icon()).isEqualTo("book");
        assertThat(TagIconCatalog.byRule("postgresql").icon()).isEqualTo("database");
        assertThat(TagIconCatalog.byRule("github-actions").icon()).isEqualTo("github");
        assertThat(TagIconCatalog.byRule("docker").icon()).isEqualTo("whale");
        assertThat(TagIconCatalog.byRule("제주").icon()).isEqualTo("palm");
        assertThat(TagIconCatalog.byRule("부산").icon()).isEqualTo("pin");
        assertThat(TagIconCatalog.byRule("오늘도").icon()).isEqualTo("leaf");
        // 짧은 영어 낱말은 첫머리에서만: rapid에는 api가 안 맞는다
        assertThat(TagIconCatalog.byRule("rapid").icon()).isEqualTo("hash");
        assertThat(TagIconCatalog.byRule("feedback").icon()).isEqualTo("hash");
        assertThat(TagIconCatalog.parseAiAnswer("camera")).map(TagIconCatalog.Style::icon).contains("camera");
        assertThat(TagIconCatalog.parseAiAnswer("고양이 아이콘")).isEmpty();
    }

    @Test
    void everyIconHasADrawing() throws Exception {
        String svg = Files.readString(Path.of("../frontend/src/main/resources/templates/fragments/tag-icons.html"));
        TagIconCatalog.ICONS.keySet().stream().filter(k -> !k.equals("hash"))
                .forEach(k -> assertThat(svg).as(k).contains("th:case=\"'" + k + "'\""));
    }

    @Test
    void tagsPageShowsPillsWithIconsTotalAndSort() throws Exception {
        long me = members.localMember("tagicon", "태그아이콘", "tagicon@example.com", "Blog#2026ok", true);
        long a = posts.published(me, "제주 여행", "본문", 1, T.minusSeconds(100));
        long b = posts.published(me, "도커 정리", "본문", 1, T.minusSeconds(50));
        tag(a, "제주", 0);
        tag(a, "docker", 1);
        tag(b, "docker", 0);
        jdbc.update("UPDATE post SET view_count = 100 WHERE id = ?", a);
        String html = mockMvc.perform(get("/tags")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("전체 2개").contains("tag-pill tone-blue").contains("#docker").contains("#docker 2개")
                .contains("tag-pill tone-green");
        assertThat(jdbc.queryForObject("SELECT icon FROM tag_icon ti JOIN tag t ON t.id = ti.tag_id WHERE t.name = 'docker'",
                String.class)).isEqualTo("whale");
        // 글 많은 순은 docker(2) 먼저, 많이 본 순은 조회 합 같으면… 제주(100)·docker(100) → 글 수로 docker 먼저, 이름 순은 docker → 제주
        String byName = mockMvc.perform(get("/tags?sort=name")).andReturn().getResponse().getContentAsString();
        assertThat(byName.indexOf("#docker")).isLessThan(byName.indexOf("#제주"));
        assertThat(byName).contains("이름 순");
    }
}
