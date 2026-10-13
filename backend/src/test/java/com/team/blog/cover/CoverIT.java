package com.team.blog.cover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.cover.application.CoverService;
import com.team.blog.support.ImageFlow;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** 055 카드 대표 사진(강성찬 개인 확장). */
class CoverIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-12T00:00:00Z");

    @Autowired
    CoverService covers;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private org.springframework.test.web.servlet.ResultActions setCover(long who, long postId, String url) throws Exception {
        return mockMvc.perform(post("/api/posts/{id}/cover", postId).with(csrf()).with(TestAuth.member(who))
                .contentType(MediaType.APPLICATION_JSON).content("{\"imageUrl\":\"" + url + "\"}"));
    }

    @Test
    void ownUploadedImageBecomesCardAndShareImage() throws Exception {
        long me = member("coverme");
        long other = member("coverother");
        long postId = posts.published(me, "사진 없는 글", "본문", 1, T);
        ImageFlow.Uploaded mine = new ImageFlow(mockMvc).uploadPostWebp(me);
        ImageFlow.Uploaded theirs = new ImageFlow(mockMvc).uploadPostWebp(other);
        mockMvc.perform(get("/api/posts/{id}/cover", postId).with(TestAuth.member(me))).andExpect(status().isNoContent());
        // 남의 사진·외부 주소는 안 되고, 남의 글에는 못 정한다
        setCover(me, postId, theirs.url()).andExpect(status().isBadRequest());
        setCover(me, postId, "https://example.com/a.png").andExpect(status().isBadRequest());
        setCover(other, postId, theirs.url()).andExpect(status().isNotFound());
        setCover(me, postId, mine.url()).andExpect(status().isOk());
        // 카드(목록 API)에 대표 사진의 썸네일, 공유 미리보기에 원본
        String list = mockMvc.perform(get("/api/posts")).andReturn().getResponse().getContentAsString();
        assertThat(list).contains(mine.thumbUrl());
        String detail = mockMvc.perform(get("/coverme/posts/{id}", postId)).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("property=\"og:image\" content=\"" + mine.url() + "\"");
        // 정리 작업은 대표 사진을 지우지 않는다
        assertThat(covers.referencedIds(List.of(mine.imageId(), theirs.imageId()))).containsExactly(mine.imageId());
        // 자동으로 되돌리면 카드 사진 없음
        mockMvc.perform(delete("/api/posts/{id}/cover", postId).with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().isNoContent());
        assertThat(mockMvc.perform(get("/api/posts")).andReturn().getResponse().getContentAsString()).doesNotContain(mine.thumbUrl());
    }
}
