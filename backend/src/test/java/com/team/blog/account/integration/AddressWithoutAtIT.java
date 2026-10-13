package com.team.blog.account.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** 076 강성찬 개인 확장: 블로그 주소는 /{주소}. 예전 /@ 주소는 301, 폼 POST는 그대로, 사이트 경로는 주소로 보지 않음. */
class AddressWithoutAtIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private MvcResult get200(String path) throws Exception {
        return mockMvc.perform(get(path)).andReturn();
    }

    @Test
    void oldAtAddressesMovePermanentlyKeepingPathAndQuery() throws Exception {
        long me = members.active("kim755030", "김민서");
        long id = posts.published(me, "제목", "본문", 1, clock.instant());
        MvcResult old = get200("/@kim755030/posts/" + id + "?comment=1");
        assertThat(old.getResponse().getStatus()).isEqualTo(301);
        assertThat(old.getResponse().getHeader("Location")).isEqualTo("/kim755030/posts/" + id + "?comment=1");
        assertThat(get200("/@Kim755030").getResponse().getHeader("Location")).isEqualTo("/kim755030");
        String blog = get200("/kim755030").getResponse().getContentAsString();
        assertThat(blog).contains("href=\"/kim755030/posts/" + id + "\"").doesNotContain("href=\"/@");
        assertThat(get200("/kim755030/posts/" + id).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void oldFormPostsStillWorkAndSiteRoutesAreNotHandles() throws Exception {
        long owner = members.active("kim755030", "김민서");
        long fan = members.active("fan123", "팬이에요");
        assertThat(mockMvc.perform(post("/@kim755030/follow").with(csrf()).with(TestAuth.member(fan)))
                .andReturn().getResponse().getStatus()).isEqualTo(303);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM follow WHERE follower_id = ? AND followee_id = ?",
                Integer.class, fan, owner)).isEqualTo(1);
        assertThat(get200("/login").getResponse().getStatus()).isEqualTo(200);
        assertThat(get200("/nobody99").getResponse().getStatus()).isEqualTo(404);
    }
}
