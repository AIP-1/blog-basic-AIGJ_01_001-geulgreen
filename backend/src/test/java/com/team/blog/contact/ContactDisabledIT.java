package com.team.blog.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** 090: 문의 양식을 끄면 꼬리말이 사이트 문의 주소 mailto로 돌아가고, 관리자 지정 주소는 쓰지 않는다. */
@TestPropertySource(properties = "blog.contact.enabled=false")
class ContactDisabledIT extends IntegrationTestBase {

    @Test
    void fallsBackToSiteContactEmail() throws Exception {
        String home = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(home).contains("mailto:blogeulgreen@gmail.com").doesNotContain("href=\"/contact\"")
                .doesNotContainIgnoringCase("private.admin@example.com");
        assertThat(mockMvc.perform(get("/contact")).andReturn().getResponse().getStatus()).isEqualTo(404);
    }
}
