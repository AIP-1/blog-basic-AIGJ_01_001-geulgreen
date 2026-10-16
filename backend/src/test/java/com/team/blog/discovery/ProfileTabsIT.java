package com.team.blog.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 100 블로그 탭(홈·글·시리즈, 강성찬 개인 확장). */
class ProfileTabsIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-14T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private String page(String url) throws Exception {
        // 같은 테스트 안에서 앞 요청의 로그인이 남지 않게 손님을 분명히
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
        return mockMvc.perform(get(url).with(anonymous())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String page(String url, long viewer) throws Exception {
        return mockMvc.perform(get(url).with(TestAuth.member(viewer))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void addToSeries(long me, long postId, String seriesIdOrNew) throws Exception {
        mockMvc.perform(post("/posts/{id}/series", postId).param("seriesId", seriesIdOrNew).param("newTitle", "탭 시리즈")
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
    }

    @Test
    void eachTabHasItsOwnAddressAndMarksCurrent() throws Exception {
        long me = member("tabme");
        for (int i = 0; i < 6; i++) {
            posts.published(me, "탭 글 " + i, "본문", 1, T.plusSeconds(i));
        }
        String home = page("/tabme");
        assertThat(home).contains("aria-current=\"page\">홈</a>").contains("href=\"/tabme/posts\"").contains("href=\"/tabme/series\"")
                .contains("모두 보기").contains("탭 글 5").contains("탭 글 2").doesNotContain("탭 글 1<")
                // 시리즈가 없으면 홈 선반은 숨김
                .doesNotContain("id=\"shelf-series\"");
        String list = page("/tabme/posts");
        assertThat(list).contains("aria-current=\"page\">글</a>").contains("탭 글 0").contains("최신순").contains("인기순");
        String series = page("/tabme/series");
        assertThat(series).contains("aria-current=\"page\">시리즈</a>").contains("아직 묶어 둔 시리즈가 없어요");
        // 예전 목록 주소(?tag·?cursor)는 글 탭 화면
        assertThat(page("/tabme?tag=없는태그")).contains("aria-current=\"page\">글</a>");
        mockMvc.perform(get("/nobodytab/posts")).andExpect(status().isNotFound());
        mockMvc.perform(get("/nobodytab/series")).andExpect(status().isNotFound());
    }

    @Test
    void tabsShowOnlyWhatTheViewerMaySee() throws Exception {
        long me = member("tabperm");
        long visitor = member("tabvisit");
        long open = posts.published(me, "공개 탭글", "본문", 1, T);
        long hidden = posts.published(me, "비공개 탭글", "본문", 1, T.plusSeconds(10));
        long friends = posts.published(me, "친구 탭글", "본문", 1, T.plusSeconds(20));
        posts.draft(me, "임시 탭글", "본문", 0);
        addToSeries(me, open, "new");
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        addToSeries(me, hidden, String.valueOf(sid));
        addToSeries(me, friends, String.valueOf(sid));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", hidden);
        jdbc.update("UPDATE post SET visibility = 'FRIENDS', first_public_at = NULL WHERE id = ?", friends);
        for (String url : new String[] {"/tabperm", "/tabperm/posts", "/tabperm/series"}) {
            for (String html : new String[] {page(url), page(url, visitor), page(url, me)}) {
                assertThat(html).as(url).doesNotContain("비공개 탭글").doesNotContain("임시 탭글").doesNotContain("친구 탭글")
                        .contains("게시물 1");
            }
        }
        assertThat(page("/tabperm/series")).contains("글 1편");
        assertThat(page("/tabperm")).contains("id=\"shelf-series\"").contains("탭 시리즈");
        // 글 화면 시리즈 위치·이전/다음도 보이는 글만
        assertThat(page("/tabperm/posts/" + open)).contains("(1/1)").doesNotContain("다음편");
        // 비공개 계정: 수락받지 않은 사람에게는 홈 탭만(프로필 윗부분), 다른 탭은 홈으로(073 인터셉터)
        jdbc.update("INSERT INTO private_account (member_id) VALUES (?)", me);
        assertThat(page("/tabperm", visitor)).contains("비공개 계정이에요").doesNotContain("공개 탭글");
        for (String url : new String[] {"/tabperm/posts", "/tabperm/series", "/tabperm/series/" + sid}) {
            mockMvc.perform(get(url).with(TestAuth.member(visitor))).andExpect(status().is3xxRedirection());
        }
        assertThat(page("/tabperm/series", me)).contains("탭 시리즈");
    }

    @Test
    void ownerReordersSeriesAndNavigationFollows() throws Exception {
        long me = member("tabord");
        long other = member("taboth");
        long p1 = posts.published(me, "순서 하나", "본문", 1, T);
        long p2 = posts.published(me, "순서 둘", "본문", 1, T.plusSeconds(1));
        long p3 = posts.published(me, "순서 셋", "본문", 1, T.plusSeconds(2));
        addToSeries(me, p1, "new");
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        addToSeries(me, p2, String.valueOf(sid));
        addToSeries(me, p3, String.valueOf(sid));
        String move = "/tabord/series/" + sid + "/move";
        // 셋을 앞으로 → 하나, 셋, 둘
        mockMvc.perform(post(move).param("postId", String.valueOf(p3)).param("dir", "up").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        String list = page("/tabord/series/" + sid, me);
        assertThat(list.indexOf("순서 하나")).isLessThan(list.indexOf("순서 셋"));
        assertThat(list.indexOf("순서 셋")).isLessThan(list.indexOf("순서 둘"));
        // 105: 앞으로·뒤로는 주인의 [순서 바꾸기](?edit) 안에만, 읽는 화면은 1편부터·최신 글부터
        assertThat(list).contains(">순서 바꾸기</a>").contains("1편부터").doesNotContain("앞으로</button>");
        assertThat(page("/tabord/series/" + sid + "?edit=1", me)).contains("순서 셋 앞으로");
        assertThat(page("/tabord/series/" + sid + "?edit=1")).doesNotContain("앞으로</button>").doesNotContain(">순서 바꾸기</a>");
        String latest = page("/tabord/series/" + sid + "?order=latest");
        assertThat(latest.indexOf("순서 둘")).isLessThan(latest.indexOf("순서 하나"));
        assertThat(latest).contains(">3편<");
        assertThat(page("/tabord/posts/" + p3)).contains("(2/3)").contains("이전편: 순서 하나").contains("다음편: 순서 둘");
        // 맨 앞에서 더 앞으로는 그대로, 남은 못 바꾼다
        mockMvc.perform(post(move).param("postId", String.valueOf(p1)).param("dir", "up").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post(move).param("postId", String.valueOf(p1)).param("dir", "down").with(csrf()).with(TestAuth.member(other)))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForList("SELECT post_id FROM series_post WHERE series_id = ? ORDER BY position", Long.class, sid))
                .containsExactly(p1, p3, p2);
    }

    @Test
    void postsTabSortsByLikesWhenAsked() throws Exception {
        long me = member("tabpop");
        long a = posts.published(me, "좋아요 적음", "본문", 1, T.plusSeconds(10));
        long b = posts.published(me, "좋아요 많음", "본문", 1, T);
        jdbc.update("UPDATE post SET like_count = 1 WHERE id = ?", a);
        jdbc.update("UPDATE post SET like_count = 9 WHERE id = ?", b);
        String latest = page("/tabpop/posts");
        assertThat(latest.indexOf("좋아요 적음")).isLessThan(latest.indexOf("좋아요 많음"));
        String popular = page("/tabpop/posts?sort=popular");
        assertThat(popular.indexOf("좋아요 많음")).isLessThan(popular.indexOf("좋아요 적음"));
        assertThat(popular).contains("aria-current=\"page\">인기순</a>");
        mockMvc.perform(get("/api/members/tabpop/posts").param("sort", "popular"))
                .andExpect(jsonPath("$.items[0].title").value("좋아요 많음"));
        mockMvc.perform(get("/api/members/tabpop/posts").param("sort", "popular").param("cursor", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profilePagesUseLineIconsNotEmoji() throws Exception {
        long me = member("tabicon");
        long post = posts.published(me, "아이콘 글", "본문", 1, T);
        addToSeries(me, post, "new");
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        java.util.regex.Pattern emoji = java.util.regex.Pattern.compile("[\\x{1F300}-\\x{1FAFF}]");
        for (String url : new String[] {"/tabicon", "/tabicon/posts", "/tabicon/series", "/tabicon/series/" + sid, "/tabicon/guestbook"}) {
            String html = page(url);
            assertThat(emoji.matcher(html).find()).as(url).isFalse();
        }
        assertThat(page("/tabicon")).contains("class=\"blog-links\"").contains("/tabicon/guestbook").contains("/tabicon/rss");
    }
}
