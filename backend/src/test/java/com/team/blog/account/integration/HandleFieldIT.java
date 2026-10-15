package com.team.blog.account.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** 설정의 블로그 주소 칸(강성찬 개인 확장): 앞에 실제 사이트 주소가 붙고, 바꿀 수 없는 동안은 칸이 읽기 전용·버튼은 꺼진다. */
class HandleFieldIT extends IntegrationTestBase {

    @Test
    void prefixIsSiteAddressAndLockedFieldIsReadonly() throws Exception {
        long id = members.localMember("hfield", "주소칸", "hfield@example.com", "Blog#2026ok", true);
        String open = mockMvc.perform(get("/settings").with(TestAuth.member(id))).andReturn().getResponse().getContentAsString();
        // 테스트 설정의 사이트 주소는 http://localhost
        assertThat(open).contains("class=\"handle-prefix\">localhost/</span>").doesNotContain("readonly=\"readonly\"");

        for (String h : new String[] {"hfield2", "hfield3"}) {
            mockMvc.perform(put("/api/me/handle").with(csrf()).with(TestAuth.member(id))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"" + h + "\"}")).andExpect(status().isOk());
        }
        String locked = mockMvc.perform(get("/settings").with(TestAuth.member(id))).andReturn().getResponse().getContentAsString();
        assertThat(locked).contains("readonly=\"readonly\"").contains("다음 변경 가능일");
    }
}
