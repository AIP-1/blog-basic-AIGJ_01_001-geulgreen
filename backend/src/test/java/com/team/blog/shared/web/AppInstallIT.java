package com.team.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** 앱 설치(강성찬 개인 확장, PWA): 홈 화면 설치 정보(manifest)·서비스 워커·끊김 안내 화면. */
class AppInstallIT extends IntegrationTestBase {

    @Test
    void everyPageLinksManifestAndRegistersServiceWorker() throws Exception {
        String html = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<link rel=\"manifest\" href=\"/manifest.webmanifest\">")
                .containsPattern("<meta name=\"theme-color\" content=\"#[0-9A-F]{6}\" media=\"\\(prefers-color-scheme: light\\)\">")
                .containsPattern("src=\"/js/pwa-register-[0-9a-f]{32}\\.js\" defer");
    }

    @Test
    void manifestIsInstallableWithFingerprintedIcons() throws Exception {
        String body = mockMvc.perform(get("/manifest.webmanifest")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(AppInstallController.MANIFEST))
                .andExpect(jsonPath("$.name").value("글그린"))
                .andExpect(jsonPath("$.short_name").value("글그린"))
                .andExpect(jsonPath("$.start_url").value("/"))
                .andExpect(jsonPath("$.display").value("standalone"))
                .andExpect(jsonPath("$.icons[2].purpose").value("maskable"))
                .andExpect(jsonPath("$.icons[2].sizes").value("512x512"))
                .andReturn().getResponse().getContentAsString();
        // 아이콘 주소에 내용 지문이 붙고 실제로 열린다
        Matcher icon = Pattern.compile("\"(/images/app-icon-(?:192|512|maskable-512)-[0-9a-f]{32}\\.png)\"").matcher(body);
        int found = 0;
        while (icon.find()) {
            mockMvc.perform(get(icon.group(1))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
            found++;
        }
        assertThat(found).isEqualTo(3);
    }

    @Test
    void serviceWorkerAndOfflinePageAreServedFromSiteRoot() throws Exception {
        String sw = mockMvc.perform(get("/sw.js")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // 화면을 저장해 두지 않는다: 끊김 안내 화면과 그 색·아이콘만 미리 받는다
        assertThat(sw).contains("'/offline.html'").contains("req.mode === 'navigate'").doesNotContain("cache.put");
        String offline = mockMvc.perform(get("/offline.html")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(offline).contains("연결이 끊겼어요").contains("noindex").contains("href=\"/css/theme.css\"");
        mockMvc.perform(get("/css/theme.css")).andExpect(status().isOk());
    }
}
