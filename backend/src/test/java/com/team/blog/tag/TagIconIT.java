package com.team.blog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        // 10-15 태그 화면: 제목·태그 수, 태그마다 아이콘·글 수·가로 카드 줄, 정렬(인기·이름·최근), 검색(10-25: 위 메뉴의 태그는 뺐다)
        String html = mockMvc.perform(get("/tags")).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("tags-total\">2개").contains("tag-section-name tone-blue").contains("#docker").contains("글 2편")
                .contains("tag-section-name tone-green").contains("class=\"tag-row\"").contains("도커 정리").contains("제주 여행");
        assertThat(html.substring(html.indexOf("<header"), html.indexOf("</header>"))).doesNotContain(">태그</a>");
        assertThat(jdbc.queryForObject("SELECT icon FROM tag_icon ti JOIN tag t ON t.id = ti.tag_id WHERE t.name = 'docker'",
                String.class)).isEqualTo("whale");
        // 인기순은 글 수가 많은 docker 먼저, 최근순은 마지막 글이 더 최근인 docker(b가 a보다 늦음) 먼저
        assertThat(html.indexOf("#docker")).isLessThan(html.indexOf("#제주"));
        // 이름순은 없다(10-15): 화면에 링크가 없고, 예전 ?sort=name 주소는 인기순으로
        assertThat(html).doesNotContain("이름순");
        String byName = mockMvc.perform(get("/tags?sort=name")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(byName).contains("aria-current=\"page\">인기순</a>");
        String recent = mockMvc.perform(get("/tags?sort=recent")).andReturn().getResponse().getContentAsString();
        assertThat(recent.indexOf("#docker")).isLessThan(recent.indexOf("#제주"));
        // 검색은 이름에 그 글자가 든 태그만
        String found = mockMvc.perform(get("/tags?q=제")).andReturn().getResponse().getContentAsString();
        assertThat(found).contains("찾은 1개").contains("#제주").doesNotContain("tag-section-name tone-blue");
        // 이어 붙일 묶음(10개 다음): 지금은 2개뿐이라 빈 조각
        String more = mockMvc.perform(get("/tags/sections?offset=10")).andReturn().getResponse().getContentAsString();
        assertThat(more).doesNotContain("tag-section");
    }

    @Test
    void tagsPageLoadsTenSectionsThenMore() throws Exception {
        long me = members.localMember("tagmany", "태그많이", "tagmany@example.com", "Blog#2026ok", true);
        for (int i = 0; i < 12; i++) {
            tag(posts.published(me, "글" + i, "본문", 1, T.minusSeconds(100 - i)), "많은태그" + i, 0);
        }
        String html = mockMvc.perform(get("/tags")).andReturn().getResponse().getContentAsString();
        assertThat(html.split("class=\"tag-section\"", -1)).hasSize(11);
        assertThat(html).contains("data-more=\"/tags/sections?sort=popular&amp;offset=10\"");
        String more = mockMvc.perform(get("/tags/sections?sort=popular&offset=10")).andReturn().getResponse().getContentAsString();
        assertThat(more.split("class=\"tag-section\"", -1)).hasSize(3);
        assertThat(more).doesNotContain("tag-sections-more").doesNotContain("<html");
    }
}
