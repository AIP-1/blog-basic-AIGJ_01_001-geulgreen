package com.team.blog.discovery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.discovery.application.RecommendedPostQuery;
import com.team.blog.post.application.PostCard;
import com.team.blog.support.IntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 077 홈 "추천 글"(강성찬 개인 확장): 내 태그와 같은 새 사람 글 먼저, 모자라면 최근 7일 조회수, 내 글·비공개는 빼고 5개. */
class RecommendedPostIT extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-10-13T03:00:00Z");

    @Autowired
    RecommendedPostQuery query;

    @Autowired
    JdbcTemplate jdbc;

    private void tag(long postId, String name) {
        jdbc.update("INSERT INTO tag (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
        jdbc.update("INSERT INTO post_tag (post_id, tag_id, position) SELECT ?, id, 0 FROM tag WHERE name = ?", postId, name);
    }

    private List<String> titles(long viewer) {
        return query.forMember(viewer).stream().map(PostCard::title).toList();
    }

    @Test
    void sameTagNewPeopleFirstThenRecentViews() {
        clock.set(NOW);
        long me = members.localMember("rpme", "나작가", "rpme@example.com", "Blog#2026ok", true);
        long followed = members.localMember("rpfol", "팔로우함", "rpfol@example.com", "Blog#2026ok", true);
        long stranger = members.localMember("rpnew", "새사람", "rpnew@example.com", "Blog#2026ok", true);
        tag(posts.published(me, "내 등산 글", "본문", 1, NOW.minus(Duration.ofDays(2))), "등산");
        tag(posts.published(followed, "팔로우한 사람 등산", "본문", 1, NOW.minus(Duration.ofDays(1))), "등산");
        tag(posts.published(stranger, "새 사람 등산", "본문", 1, NOW.minus(Duration.ofDays(3))), "등산");
        long popular = posts.published(stranger, "조회 많은 글", "본문", 1, NOW.minus(Duration.ofDays(1)));
        jdbc.update("UPDATE post SET view_count = 999 WHERE id = ?", popular);
        long hidden = posts.published(stranger, "비공개 글", "본문", 1, NOW.minus(Duration.ofDays(1)));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE', view_count = 5000 WHERE id = ?", hidden);
        jdbc.update("INSERT INTO follow (follower_id, followee_id) VALUES (?, ?)", me, followed);

        List<String> mine = titles(me);
        assertThat(mine.get(0)).isEqualTo("새 사람 등산");
        assertThat(mine).contains("조회 많은 글").doesNotContain("내 등산 글", "비공개 글").hasSizeLessThanOrEqualTo(5);
        assertThat(mine.indexOf("조회 많은 글")).isLessThan(mine.indexOf("팔로우한 사람 등산"));
        // 비회원: 기록이 없으니 최근 7일 조회수 순
        assertThat(titles(-1).get(0)).isEqualTo("조회 많은 글");
    }
}
