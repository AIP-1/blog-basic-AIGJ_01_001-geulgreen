package com.team.blog.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.media.application.ImageStorage;
import com.team.blog.support.IntegrationTestBase;
import java.util.List;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 083 테스트용 긴 글(강성찬 개인 확장): 관리자 버튼으로 넣고, 다시 눌러도 겹치지 않고, 한 번에 모두 지운다. */
class SamplePostsIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ImageStorage storage;

    private int samplePosts() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM post p JOIN member m ON m.id = p.author_id
                WHERE m.handle LIKE 'test\\_%' AND p.status = 'PUBLISHED' AND p.visibility = 'PUBLIC' AND p.deleted_at IS NULL
                """, Integer.class);
    }

    @Test
    void seedIsIdempotentAndRemoveCleansEverything() throws Exception {
        long user = members.localMember("plainuser", "plainuser", "plainuser@example.com", "Blog#2026ok", true);
        long admin = members.localMember("boss", "boss", "boss@example.com", "Blog#2026ok", true);
        mockMvc.perform(post("/admin/sample-posts").with(csrf()).with(TestAuth.member(user))).andExpect(status().isNotFound());

        mockMvc.perform(post("/admin/sample-posts").with(csrf()).with(TestAuth.admin(admin))).andExpect(status().is3xxRedirection());
        int seeded = samplePosts();
        assertThat(seeded).isGreaterThanOrEqualTo(12);
        long postId = jdbc.queryForObject("""
                SELECT p.id FROM post p JOIN member m ON m.id = p.author_id WHERE m.handle = 'test_minji' ORDER BY p.id LIMIT 1
                """, Long.class);
        String html = mockMvc.perform(get("/test_minji/posts/{id}", postId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<h2").contains("<table").contains("<blockquote").contains("<pre");
        assertThat(mockMvc.perform(get("/admin").with(TestAuth.admin(admin))).andReturn().getResponse().getContentAsString())
                .contains("테스트 글 넣기").contains("모두 지우기");

        // 사진 글: 평소 업로드처럼 완료된 사진, 카드 썸네일, 본문 그림
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE thumbnail_url IS NOT NULL AND title LIKE '%산책%'",
                Integer.class)).isEqualTo(1);
        List<String> keys = jdbc.queryForList("SELECT storage_key FROM image WHERE status = 'ATTACHED' AND detached_at IS NULL", String.class);
        assertThat(keys).hasSizeGreaterThanOrEqualTo(24);
        long imagePostId = jdbc.queryForObject("SELECT id FROM post WHERE title LIKE '%산책%'", Long.class);
        assertThat(mockMvc.perform(get("/test_minji/posts/{id}", imagePostId)).andReturn().getResponse().getContentAsString())
                .contains("<img").doesNotContain("image:13-");
        assertThat(storage.inspect(keys.get(0))).isPresent();

        mockMvc.perform(post("/admin/sample-posts").with(csrf()).with(TestAuth.admin(admin))).andExpect(status().is3xxRedirection());
        assertThat(samplePosts()).isEqualTo(seeded);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM image", Integer.class)).isEqualTo(keys.size());

        mockMvc.perform(post("/admin/sample-posts/remove").with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        assertThat(samplePosts()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM member WHERE handle LIKE 'test\\_%'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_identity WHERE email LIKE '%@sample.invalid'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM image", Integer.class)).isZero();
        assertThat(storage.inspect(keys.get(0))).isEmpty();

        // 지운 뒤 다시 넣을 수 있다
        mockMvc.perform(post("/admin/sample-posts").with(csrf()).with(TestAuth.admin(admin))).andExpect(status().is3xxRedirection());
        assertThat(samplePosts()).isEqualTo(seeded);
    }
}
