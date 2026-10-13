package com.team.blog.post.integration;

import static com.team.blog.post.integration.PostTestSupport.writer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;

/** 004 T315: 편집 화면·내 글 최소 목록(research R-11, FR-019, FR-023). */
class EditorPageIT extends IntegrationTestBase {

    @Test
    void editorShowsCurrentContentEscaped() throws Exception {
        long me = writer(members, "editorpage");
        long postId = posts.draft(me, "</script><script>alert(1)</script>", "<b>본문</b>", 3);
        String html = mockMvc.perform(get("/write/{id}", postId).with(TestAuth.member(me)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("<script>alert(1)</script>").doesNotContain("<b>본문</b>");
        assertThat(html).contains("data-state=").contains("&quot;version&quot;:3").contains("&lt;b&gt;본문&lt;/b&gt;");
        assertThat(html).contains("/js/editor/autosave-").contains("id=\"save-status\"");
        // 068 후속: 글쓰기 머리말(나가기·저장 상태·버튼)이 맨 위, 사진·표는 본문 위 도구줄
        assertThat(html).contains("class=\"editor-exit\"").contains("class=\"editor-tools\"");
        assertThat(html.indexOf("class=\"editor-bar\"")).isLessThan(html.indexOf("id=\"post-title\""));
        // 058 강성찬 개인 확장: 글을 드래그하면 뜨는 서식 창
        assertThat(html).contains("/js/editor/format-toolbar-").contains("id=\"format-hint\"")
                .contains("data-writing-ai=\"false\"").doesNotContain("J AI에게 부탁"); // 059: Gemini 키가 없으면 AI 칸 없음
        // 060 보이는 대로 쓰기: 변환 도구와 편집 칸 스크립트(Markdown 칸은 그대로 남아 저장·자동 저장에 쓰인다)
        assertThat(html).contains("/js/editor/visual-editor-").contains("/webjars/marked/").contains("/webjars/turndown/")
                .contains("id=\"post-content\"");
        // 064·065 강성찬 개인 확장: / 명령 안내와 [표] 버튼
        assertThat(html).contains("줄 처음에 / 를 쓰면").contains("id=\"table-insert\"");
        // 임시글에는 [변경 취소]가 없다
        assertThat(html).doesNotContain("discard-working-copy");
    }

    /** 060: 보이는 대로 쓰기 칸이 쓰는 라이브러리를 우리 서버가 내준다(CSP script-src 'self'). */
    @Test
    void visualEditorLibrariesAreServed() throws Exception {
        for (String path : java.util.List.of("/webjars/marked/15.0.12/marked.min.js", "/webjars/turndown/7.2.0/dist/turndown.js",
                "/webjars/turndown-plugin-gfm/1.0.2/dist/turndown-plugin-gfm.js", "/js/editor/visual-editor.js")) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void othersDraftAndTrashedDraftAre404AndGuestIsSentToLogin() throws Exception {
        long me = writer(members, "editorown");
        long other = writer(members, "editoroth");
        long othersPost = posts.draft(other, "남의 글", "", 0);
        long trashed = posts.draft(me, "휴지통", "", 0);
        posts.trash(trashed);
        mockMvc.perform(get("/write/{id}", othersPost).with(TestAuth.member(me))).andExpect(status().isNotFound());
        mockMvc.perform(get("/write/{id}", trashed).with(TestAuth.member(me))).andExpect(status().isNotFound());
        mockMvc.perform(get("/write/{id}", 999999).with(TestAuth.member(me))).andExpect(status().isNotFound());
        mockMvc.perform(get("/write/{id}", othersPost).with(TestAuth.admin(me))).andExpect(status().isNotFound());
        mockMvc.perform(get("/write/{id}", othersPost)).andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/login?redirect=%2Fwrite%2F" + othersPost));
    }

    @Test
    void myPostsListsOwnPostsWithBadges() throws Exception {
        long me = writer(members, "listowner");
        long other = writer(members, "listother");
        posts.draft(me, "", "", 0);
        posts.draft(me, "<i>임시</i>", "", 0);
        long published = posts.published(me, "발행 글", "본문", 2, java.time.Instant.parse("2026-10-01T00:00:00Z"));
        posts.workingCopy(published, "고치는 중", "", 3);
        posts.draft(other, "남의 임시글", "", 0);
        long trashed = posts.draft(me, "휴지통 글", "", 0);
        posts.trash(trashed);

        // 011: 임시글 탭(063부터 기본 탭은 발행 글)
        String html = mockMvc.perform(get("/manage/posts").param("tab", "drafts").with(TestAuth.member(me)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("(제목 없음)").contains("&lt;i&gt;임시&lt;/i&gt;").contains("임시글 2")
                .doesNotContain("남의 임시글").doesNotContain("휴지통 글");
        String publishedTab = mockMvc.perform(get("/manage/posts").param("tab", "published").with(TestAuth.member(me)))
                .andReturn().getResponse().getContentAsString();
        assertThat(publishedTab).contains("발행 글").contains("수정 중");
        assertThat(html).contains("action=\"/write\"").contains("href=\"/manage/posts\"");
        mockMvc.perform(get("/manage/posts")).andExpect(status().isSeeOther());
    }
}
