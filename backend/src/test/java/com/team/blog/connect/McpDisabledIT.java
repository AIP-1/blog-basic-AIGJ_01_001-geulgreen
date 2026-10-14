package com.team.blog.connect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** 097: 끄면 /mcp와 설정 > 연결이 없다. */
@TestPropertySource(properties = "blog.ai-connect.enabled=false")
class McpDisabledIT extends IntegrationTestBase {

    @Test
    void switchedOff() throws Exception {
        long me = members.localMember("mcpoff", "mcpoff", "mcpoff@example.com", "Blog#2026ok", true);
        assertThat(mockMvc.perform(post("/mcp").content("{}")).andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mockMvc.perform(get("/settings/connections").with(TestAuth.member(me))).andReturn().getResponse()
                .getStatus()).isEqualTo(404);
        assertThat(mockMvc.perform(get("/settings").with(TestAuth.member(me))).andReturn().getResponse()
                .getContentAsString()).doesNotContain("href=\"/settings/connections\"");
    }
}
