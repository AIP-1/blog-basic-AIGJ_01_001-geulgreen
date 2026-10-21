package com.team.blog.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

/** 097 AI 연결(강성찬 개인 확장): 설정에서 키를 받고, 그 키로 /mcp 도구를 불러 내 임시글만 만들고 고친다. */
class McpIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private String issueKey(long memberId) throws Exception {
        String page = mockMvc.perform(post("/settings/connections/ai").param("name", "Claude Code").with(csrf())
                .with(TestAuth.member(memberId))).andReturn().getResponse().getContentAsString();
        Matcher m = Pattern.compile("ggk_[A-Za-z0-9_-]{43}").matcher(page);
        assertThat(m.find()).isTrue();
        assertThat(page).contains("채팅·메신저·글·스크린샷에 붙이지 말고");
        return m.group();
    }

    private MockHttpServletResponse rpc(String key, String body) throws Exception {
        var req = post("/mcp").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON,
                MediaType.TEXT_EVENT_STREAM).content(body);
        if (key != null) {
            req.header("Authorization", "Bearer " + key);
        }
        return mockMvc.perform(req).andReturn().getResponse();
    }

    private String call(String key, String tool, String args) throws Exception {
        return rpc(key, "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool
                + "\",\"arguments\":" + args + "}}").getContentAsString();
    }

    @Test
    void settingsListsKeysAndSnippetIsUserScope() throws Exception {
        long me = member("mcplist");
        String key = issueKey(me);
        String settings = mockMvc.perform(get("/settings").with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString();
        assertThat(settings).contains("Claude Code · 마지막 사용 아직 없음").contains("키 관리").doesNotContain(key);
        String page = mockMvc.perform(get("/settings/connections").with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString();
        assertThat(page).contains("--transport http --scope user geulgreen");
    }

    @Test
    void keySpeaksMcpWithoutCookies() throws Exception {
        String key = issueKey(member("mcpproto"));
        // 원문은 DB에 없다
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_key WHERE key_hash = ?", Integer.class, key)).isZero();

        // 키가 없거나 틀리면 401, 쿠키·세션 없음
        MockHttpServletResponse noKey = rpc(null, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}");
        assertThat(noKey.getStatus()).isEqualTo(401);
        assertThat(noKey.getHeader("WWW-Authenticate")).startsWith("Bearer");
        assertThat(rpc("ggk_wrong", "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}").getStatus()).isEqualTo(401);

        MockHttpServletResponse init = rpc(key, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
                + "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"t\",\"version\":\"1\"}}}");
        assertThat(init.getStatus()).isEqualTo(200);
        assertThat(init.getContentAsString()).contains("\"protocolVersion\":\"2025-06-18\"").contains("\"tools\"");
        assertThat(init.getHeader("Set-Cookie")).isNull();
        assertThat(rpc(key, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}").getStatus()).isEqualTo(202);
        assertThat(rpc(key, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}").getContentAsString())
                .contains("create_draft", "update_draft", "list_drafts", "list_tags_and_categories")
                .doesNotContain("publish");
        assertThat(rpc(key, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"nope\"}").getContentAsString()).contains("-32601");
        assertThat(rpc(key, "{not json").getContentAsString()).contains("-32700");
        assertThat(mockMvc.perform(get("/mcp").header("Authorization", "Bearer " + key)).andReturn().getResponse()
                .getStatus()).isEqualTo(405);
    }

    @Test
    void keyLetsAiWriteOnlyMyDrafts() throws Exception {
        long me = member("mcpme");
        long other = member("mcpother");
        String key = issueKey(me);

        // 임시글 만들기: 상태 DRAFT, 태그 정규화
        String created = call(key, "create_draft",
                "{\"title\":\"오늘의 개발 일지\",\"content_md\":\"# 한 일\\n- MCP 서버\",\"tags\":[\"#MCP\",\"개발 일지\"]}");
        assertThat(created).contains("\"isError\":false").contains("/write/");
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE author_id = ?", Long.class, me);
        assertThat(jdbc.queryForObject("SELECT status FROM post WHERE id = ?", String.class, postId)).isEqualTo("DRAFT");
        assertThat(created).contains("\"mcp\"", "\"개발-일지\"");

        // 고치기, 목록, 태그
        assertThat(call(key, "update_draft", "{\"post_id\":" + postId + ",\"title\":\"고친 제목\"}"))
                .contains("\"isError\":false").contains("고친 제목");
        assertThat(call(key, "list_drafts", "{}")).contains("고친 제목");
        assertThat(call(key, "list_tags_and_categories", "{}")).contains("개발-일지");

        // 남의 글·발행한 글은 고칠 수 없다, 잘못된 입력은 isError
        long othersDraft = posts.draft(other, "남의 글", "본문", 0);
        assertThat(call(key, "update_draft", "{\"post_id\":" + othersDraft + ",\"title\":\"x\"}"))
                .contains("\"isError\":true").contains("NOT_FOUND");
        long published = posts.published(me, "발행한 글", "본문", 1, java.time.Instant.now());
        assertThat(call(key, "update_draft", "{\"post_id\":" + published + ",\"title\":\"x\"}")).contains("NOT_A_DRAFT");
        assertThat(call(key, "create_draft", "{\"title\":\"x\"}")).contains("\"isError\":true");
        assertThat(call(key, "publish_post", "{}")).contains("UNKNOWN_TOOL");
        assertThat(jdbc.queryForObject("SELECT title FROM post WHERE id = ?", String.class, othersDraft)).isEqualTo("남의 글");

        // 설정 화면에 키 이름·사용 시각이 보이고, 끊으면 401
        String page = mockMvc.perform(get("/settings/connections").with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString();
        assertThat(page).contains("Claude Code").contains("마지막 사용").doesNotContain(key);
        long keyId = jdbc.queryForObject("SELECT id FROM ai_key WHERE member_id = ?", Long.class, me);
        mockMvc.perform(post("/settings/connections/ai/{id}/revoke", keyId).with(csrf()).with(TestAuth.member(other)))
                .andReturn();
        assertThat(rpc(key, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}").getStatus()).isEqualTo(200);
        mockMvc.perform(post("/settings/connections/ai/{id}/revoke", keyId).with(csrf()).with(TestAuth.member(me)))
                .andReturn();
        assertThat(rpc(key, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}").getStatus()).isEqualTo(401);
    }

    @Test
    void keysAreLimitedPerMember() throws Exception {
        long me = member("mcplimit");
        for (int i = 0; i < 5; i++) {
            issueKey(me);
        }
        String page = mockMvc.perform(post("/settings/connections/ai").with(csrf()).with(TestAuth.member(me)))
                .andReturn().getResponse().getContentAsString();
        assertThat(page).contains("5개까지");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_key WHERE member_id = ?", Integer.class, me)).isEqualTo(5);
    }

    @Test
    void draftsGoIntoMySeriesByNameOrNumber() throws Exception {
        long me = member("mcpseries");
        long other = member("mcpseriesx");
        String key = issueKey(me);
        jdbc.update("INSERT INTO series (member_id, title) VALUES (?, ?)", other, "남의 시리즈");
        long othersSeries = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, other);

        // 이름으로: 없으면 만들고, 같은 이름이면 같은 시리즈에
        String first = call(key, "create_draft", "{\"title\":\"제작기 1\",\"content_md\":\"본문\",\"series_title\":\"글그린 제작기\"}");
        assertThat(first).contains("\"isError\":false").contains("글그린 제작기");
        call(key, "create_draft", "{\"title\":\"제작기 2\",\"content_md\":\"본문\",\"series_title\":\" 글그린 제작기 \"}");
        long seriesId = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM series_post WHERE series_id = ?", Integer.class, seriesId)).isEqualTo(2);
        assertThat(call(key, "list_tags_and_categories", "{}")).contains("\"series_id\":" + seriesId).contains("글그린 제작기");

        // 번호로 옮기기, 남의 시리즈·둘 다 주기·너무 긴 이름은 거절(임시글을 새로 만들지 않음)
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE author_id = ? AND title = '제작기 2'", Long.class, me);
        call(key, "update_draft", "{\"post_id\":" + postId + ",\"series_title\":\"다른 시리즈\"}");
        assertThat(call(key, "update_draft", "{\"post_id\":" + postId + ",\"series_id\":" + seriesId + "}"))
                .contains("\"isError\":false").contains("글그린 제작기");
        int before = jdbc.queryForObject("SELECT count(*) FROM post WHERE author_id = ?", Integer.class, me);
        assertThat(call(key, "create_draft", "{\"title\":\"x\",\"content_md\":\"y\",\"series_id\":" + othersSeries + "}"))
                .contains("SERIES_NOT_FOUND");
        assertThat(call(key, "create_draft", "{\"title\":\"x\",\"content_md\":\"y\",\"series_id\":" + seriesId
                + ",\"series_title\":\"a\"}")).contains("INVALID_REQUEST");
        assertThat(call(key, "create_draft", "{\"title\":\"x\",\"content_md\":\"y\",\"series_title\":\"" + "가".repeat(61) + "\"}"))
                .contains("SERIES_TITLE_INVALID");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE author_id = ?", Integer.class, me)).isEqualTo(before);
        // 임시글로 남는다(발행은 사람이)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE author_id = ? AND status <> 'DRAFT'", Integer.class, me)).isZero();
    }

    /** 110 강성찬 개인 확장: 임시글 썸네일. 1600x1000 PNG를 그려 내 사진으로 올리고 카드 대표 사진으로 정한다. */
    @Test
    void makesThumbnailForMyDraftOnly() throws Exception {
        long me = member("mcpthumb");
        String key = issueKey(me);
        String created = call(key, "create_draft", "{\"title\":\"썸네일 글\",\"content_md\":\"본문\",\"series_title\":\"글그린 제작기\"}");
        assertThat(created).contains("\"isError\":false");
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE author_id = ?", Long.class, me);
        String made = call(key, "make_thumbnail", "{\"post_id\":" + postId + ",\"theme\":\"ink\"}");
        assertThat(made).contains("\"isError\":false").contains("image_url");
        var cover = jdbc.queryForMap("SELECT pc.image_url, i.width, i.height, i.content_type FROM post_cover pc JOIN image i ON i.id = pc.image_id"
                + " WHERE pc.post_id = ?", postId);
        assertThat(cover).containsEntry("width", 1600).containsEntry("height", 1000).containsEntry("content_type", "image/png");
        // 발행한 글·남의 글은 안 된다
        long published = posts.published(me, "발행 글", "본문", 1, java.time.Instant.parse("2026-10-16T00:00:00Z"));
        assertThat(call(key, "make_thumbnail", "{\"post_id\":" + published + "}")).contains("NOT_A_DRAFT");
        long other = member("mcpthumbx");
        long othersDraft = posts.draft(other, "남의 임시글", "본문", 0);
        assertThat(call(key, "make_thumbnail", "{\"post_id\":" + othersDraft + "}")).contains("\"isError\":true");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post_cover WHERE post_id = ?", Integer.class, othersDraft)).isZero();
    }

    /** 111 강성찬 개인 확장: 시리즈 대표 사진을 내가 올린 사진(여기서는 만든 썸네일)으로. 남의 사진·외부 주소는 자동으로, 정리 작업은 안 지운다. */
    @Test
    void seriesCoverCanBeMyUploadedImage() throws Exception {
        long me = member("mcpcover");
        String key = issueKey(me);
        // 시리즈 밖 글로 썸네일을 만들어(내가 올린 사진) 시리즈 대표 사진으로 쓴다
        call(key, "create_draft", "{\"title\":\"시리즈 글\",\"content_md\":\"본문\",\"series_title\":\"표지 시리즈\"}");
        call(key, "create_draft", "{\"title\":\"표지 글\",\"content_md\":\"본문\"}");
        long postId = jdbc.queryForObject("SELECT id FROM post WHERE author_id = ? AND title = '표지 글'", Long.class, me);
        call(key, "make_thumbnail", "{\"post_id\":" + postId + "}");
        var img = jdbc.queryForMap("SELECT image_id, image_url FROM post_cover WHERE post_id = ?", postId);
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/manage/series/{id}/edit", sid)
                .param("title", "표지 시리즈").param("coverUrl", (String) img.get("image_url"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .with(com.team.blog.support.TestAuth.member(me)));
        var row = jdbc.queryForMap("SELECT cover_url, cover_image_id FROM series WHERE id = ?", sid);
        assertThat(row.get("cover_image_id")).isEqualTo(img.get("image_id"));
        assertThat((String) row.get("cover_url")).isNotBlank();
        // 이름만 고쳐도(지금 대표 사진 주소 그대로) 사진은 남는다
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/manage/series/{id}/edit", sid)
                .param("title", "표지 시리즈 2").param("coverUrl", (String) row.get("cover_url"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .with(com.team.blog.support.TestAuth.member(me)));
        assertThat(jdbc.queryForObject("SELECT cover_image_id FROM series WHERE id = ?", Long.class, sid)).isEqualTo(img.get("image_id"));
        assertThat(seriesService.referencedIds(java.util.List.of((Long) img.get("image_id")))).contains((Long) img.get("image_id"));
        // 남의 시리즈 사진으로는 못 쓴다: 다른 사람이 내 사진 주소를 넣으면 자동
        long other = member("mcpcoverx");
        jdbc.update("INSERT INTO series (member_id, title) VALUES (?, '남의 시리즈')", other);
        long otherSid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, other);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/manage/series/{id}/edit", otherSid)
                .param("title", "남의 시리즈").param("coverUrl", (String) img.get("image_url"))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .with(com.team.blog.support.TestAuth.member(other)));
        assertThat(jdbc.queryForMap("SELECT cover_url, cover_image_id FROM series WHERE id = ?", otherSid))
                .containsEntry("cover_url", null).containsEntry("cover_image_id", null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.team.blog.series.application.SeriesService seriesService;
}
