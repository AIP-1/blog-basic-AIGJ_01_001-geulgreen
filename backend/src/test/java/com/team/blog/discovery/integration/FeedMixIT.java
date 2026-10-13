package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 081 피드(강성찬 개인 확장): 팔로우한 글 4개마다 추천 1개, 끝나면 "모두 봤어요" 뒤 추천, 팔로우가 없으면 처음부터 추천. */
class FeedMixIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private static final Instant T = Instant.parse("2026-10-13T03:00:00Z");

    private static List<String> titles(String json) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("\"title\":\"([^\"]+)\"").matcher(json);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    @Test
    void mixesRecommendationsEveryFourFollowedPosts() throws Exception {
        clock.set(T);
        long me = members.localMember("fmme", "피드나", "fmme@example.com", "Blog#2026ok", true);
        long friend = members.localMember("fmfriend", "친구", "fmfriend@example.com", "Blog#2026ok", true);
        long other = members.localMember("fmother", "다른작가", "fmother@example.com", "Blog#2026ok", true);
        posts.published(me, "내 글", "본문", 1, T.minusSeconds(100));
        for (int i = 0; i < 3; i++) {
            posts.published(other, "추천" + i, "본문", 1, T.minusSeconds(50 + i));
        }
        // 팔로우가 없으면 처음부터 추천 글, 내 글은 빠짐
        String none = mockMvc.perform(get("/api/feed").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        assertThat(titles(none)).containsExactlyInAnyOrder("추천0", "추천1", "추천2");
        assertThat(none).contains("\"dividerBefore\":null");

        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?)", me, friend);
        for (int i = 0; i < 5; i++) {
            posts.published(friend, "친구" + i, "본문", 1, T.minusSeconds(i));
        }
        String json = mockMvc.perform(get("/api/feed").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        List<String> t = titles(json);
        assertThat(t.subList(0, 4)).containsExactly("친구0", "친구1", "친구2", "친구3");
        assertThat(t.get(4)).startsWith("추천");
        assertThat(t.get(5)).isEqualTo("친구4");
        assertThat(t.subList(6, t.size())).allMatch(s -> s.startsWith("추천")).hasSize(2);
        assertThat(t).doesNotContain("내 글");
        String html = mockMvc.perform(get("/feed").with(TestAuth.member(me))).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("팔로우한 글을 모두 봤어요").contains("feed-rec-badge").contains("많이 읽는 글");
        assertThat(html.indexOf("친구4")).isLessThan(html.indexOf("팔로우한 글을 모두 봤어요"));
    }
}
