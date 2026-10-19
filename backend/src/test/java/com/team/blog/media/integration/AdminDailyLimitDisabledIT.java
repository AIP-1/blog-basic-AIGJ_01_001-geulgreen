package com.team.blog.media.integration;

import static com.team.blog.support.ImageFlow.postPresignRequest;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/** 관리자 하루 한도 예외를 끄면 관리자도 하루 200장. */
@TestPropertySource(properties = "blog.image.admin-daily-limit-exempt=false")
class AdminDailyLimitDisabledIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StringRedisTemplate redis;

    @Test
    void adminHitsDailyLimitWhenSwitchOff() throws Exception {
        clock.set(Instant.parse("2026-10-07T03:00:00Z"));
        long admin = members.localMember("offadmin", "offadmin", "offadmin@example.com", "Blog#2026ok", true);
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", admin);
        redis.opsForValue().set("img:daily:" + admin + ":20261007", "200");
        mockMvc.perform(postPresignRequest(admin, "image/webp", 100, null))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("DAILY_UPLOAD_LIMIT"));
    }
}
