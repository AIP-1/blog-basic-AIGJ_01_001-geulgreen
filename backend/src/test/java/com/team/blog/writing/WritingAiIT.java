package com.team.blog.writing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/** 059 AI로 글 다듬기(강성찬 개인 확장): 동의·전체 공개 글만·길이·하루 횟수, 고른 글만 보내고 제안만 돌려준다. */
class WritingAiIT extends IntegrationTestBase {

    static volatile String lastRequest = "";
    static final HttpServer SERVER;

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        SERVER.createContext("/v1beta/", ex -> {
            lastRequest = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String answer = lastRequest.contains("틀린 곳만 찾아")
                    ? "1. 안되요 => 안 돼요\\n- \\\"할께요\\\" → 할게요\\n지어낸말 => 다른말\\n설명 줄\\n안되요 => 안 돼요"
                    : "```\\n**다듬은** 글이에요\\n```";
            byte[] body = ("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + answer + "\"}]}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        SERVER.start();
    }

    @DynamicPropertySource
    static void ai(DynamicPropertyRegistry registry) {
        registry.add("blog.ai.tag-suggest.gemini.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
        registry.add("blog.ai.tag-suggest.gemini.api-key", () -> "test-gemini-key");
        registry.add("blog.writing-ai.per-day", () -> "2");
    }

    @AfterAll
    static void stop() {
        SERVER.stop(0);
    }

    @Autowired
    JdbcTemplate jdbc;

    private ResultActions ask(long who, long postId, String json) throws Exception {
        return mockMvc.perform(post("/api/posts/{id}/writing-ai", postId).with(csrf()).with(TestAuth.member(who))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void rewriteNeedsConsentPublicPostAndStaysWithinDailyLimit() throws Exception {
        long me = members.localMember("writingme", "writingme", "writingme@example.com", "Blog#2026ok", true);
        long other = members.localMember("writingoth", "writingoth", "writingoth@example.com", "Blog#2026ok", true);
        long postId = posts.draft(me, "비밀 제목", "본문", 1);
        String improve = "{\"text\":\"고칠 문장이에요\",\"action\":\"improve\",\"visibility\":\"PUBLIC\"}";
        // 화면에 AI 칸 표시(키가 있을 때)
        assertThat(mockMvc.perform(get("/write/{id}", postId).with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString()).contains("data-writing-ai=\"true\"");
        // 동의 전·전체 공개가 아님·남의 글
        ask(me, postId, improve).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRITING_AI_CONSENT_REQUIRED"));
        jdbc.update("INSERT INTO member_agreement (member_id, type) VALUES (?, 'AI')", me);
        ask(me, postId, improve.replace("PUBLIC", "FRIENDS")).andExpect(jsonPath("$.code").value("WRITING_AI_PUBLIC_ONLY"));
        ask(other, postId, improve).andExpect(status().isNotFound());
        // 글 길이·부탁 내용
        ask(me, postId, "{\"text\":\"" + "가".repeat(2001) + "\",\"action\":\"fix\",\"visibility\":\"PUBLIC\"}")
                .andExpect(jsonPath("$.code").value("WRITING_AI_TEXT_LENGTH"));
        ask(me, postId, "{\"text\":\"글\",\"action\":\"ask\",\"ask\":\" \",\"visibility\":\"PUBLIC\"}")
                .andExpect(jsonPath("$.code").value("WRITING_AI_ASK_REQUIRED"));
        // 고른 글만 보내고(제목은 안 보냄), 감싼 코드 울타리는 벗겨서 제안만
        ask(me, postId, improve).andExpect(status().isOk()).andExpect(jsonPath("$.text").value("**다듬은** 글이에요"));
        assertThat(lastRequest).contains("고칠 문장이에요").contains("자연스럽고").doesNotContain("비밀 제목");
        ask(me, postId, "{\"text\":\"글\",\"action\":\"ask\",\"ask\":\"더 정중하게\",\"visibility\":\"PUBLIC\"}")
                .andExpect(status().isOk());
        assertThat(lastRequest).contains("더 정중하게");
        assertThat(jdbc.queryForObject("SELECT content_md FROM post WHERE id = ?", String.class, postId)).isEqualTo("본문");
        // 하루 2번(테스트 설정)을 넘으면 429
        ask(me, postId, improve).andExpect(status().isTooManyRequests());
    }

    /** 085 맞춤법 검사: 같은 동의·공개 규칙, 본문에 있는 틀린 곳만 돌려주고(지어낸 곳·겹침은 버림) 긴 글은 앞부분만 보낸다. */
    @Test
    void spellingReturnsOnlyFixesFoundInTheText() throws Exception {
        long me = members.localMember("spellme", "spellme", "spellme@example.com", "Blog#2026ok", true);
        long postId = posts.draft(me, "제목", "본문", 1);
        String json = "{\"text\":\"그건 안되요. 내일 할께요.\",\"visibility\":\"PUBLIC\"}";
        spell(me, postId, json).andExpect(jsonPath("$.code").value("WRITING_AI_CONSENT_REQUIRED"));
        jdbc.update("INSERT INTO member_agreement (member_id, type) VALUES (?, 'AI')", me);
        spell(me, postId, json.replace("PUBLIC", "PRIVATE")).andExpect(jsonPath("$.code").value("WRITING_AI_PUBLIC_ONLY"));
        spell(me, postId, "{\"text\":\" \",\"visibility\":\"PUBLIC\"}").andExpect(jsonPath("$.code").value("WRITING_CHECK_EMPTY"));
        spell(me, postId, json).andExpect(status().isOk())
                .andExpect(jsonPath("$.fixes.length()").value(2))
                .andExpect(jsonPath("$.fixes[0].before").value("안되요")).andExpect(jsonPath("$.fixes[0].after").value("안 돼요"))
                .andExpect(jsonPath("$.fixes[1].before").value("할께요")).andExpect(jsonPath("$.fixes[1].after").value("할게요"))
                .andExpect(jsonPath("$.truncated").value(false));
        assertThat(jdbc.queryForObject("SELECT content_md FROM post WHERE id = ?", String.class, postId)).isEqualTo("본문");
    }

    private ResultActions spell(long who, long postId, String json) throws Exception {
        return mockMvc.perform(post("/api/posts/{id}/writing-ai/spelling", postId).with(csrf()).with(TestAuth.member(who))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
