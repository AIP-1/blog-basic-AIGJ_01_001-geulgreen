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
        assertThat(page("/tabperm/series")).contains("글 1편</span> · <span class=\"meta-part\">").contains(" 업데이트");
        assertThat(page("/tabperm")).contains("id=\"shelf-series\"").contains("탭 시리즈");
        // 글 화면 시리즈 위치·이전/다음도 보이는 글만
        assertThat(page("/tabperm/posts/" + open)).contains(">1/1<").doesNotContain("series-box-nav");
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
        String move = "/manage/series/" + sid + "/move"; // 106: 순서 바꾸기는 내 글 관리 [시리즈] 탭
        // 셋을 앞으로 → 하나, 셋, 둘
        mockMvc.perform(post(move).param("postId", String.valueOf(p3)).param("dir", "up").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        // 검색 문구(head)에도 제목이 들어가서 순서는 본문에서만 본다
        String list = page("/tabord/series/" + sid, me);
        list = list.substring(list.indexOf("<body"));
        assertThat(list.indexOf("순서 하나")).isLessThan(list.indexOf("순서 셋"));
        assertThat(list.indexOf("순서 셋")).isLessThan(list.indexOf("순서 둘"));
        // 106: 읽는 화면은 1편부터·최신 글부터만, 주인에게는 관리 탭 링크. 앞으로·뒤로는 관리 탭에
        assertThat(list).contains(">시리즈 관리</a>").contains("1편부터").doesNotContain("앞으로</button>");
        assertThat(page("/tabord/series/" + sid)).doesNotContain(">시리즈 관리</a>");
        assertThat(page("/manage/series", me)).contains("순서 셋 앞으로").contains("순서 둘 시리즈에서 빼기");
        String latest = page("/tabord/series/" + sid + "?order=latest");
        latest = latest.substring(latest.indexOf("<body"));
        assertThat(latest.indexOf("순서 둘")).isLessThan(latest.indexOf("순서 하나"));
        assertThat(latest).contains(">3편<");
        assertThat(page("/tabord/posts/" + p3)).contains(">2/3<").contains("이전 편").contains("순서 하나").contains("다음 편").contains("순서 둘");
        // 맨 앞에서 더 앞으로는 그대로, 남은 못 바꾼다
        mockMvc.perform(post(move).param("postId", String.valueOf(p1)).param("dir", "up").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post(move).param("postId", String.valueOf(p1)).param("dir", "down").with(csrf()).with(TestAuth.member(other)))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForList("SELECT post_id FROM series_post WHERE series_id = ? ORDER BY position", Long.class, sid))
                .containsExactly(p1, p3, p2);
    }

    /** 106 강성찬 개인 확장: 관리 탭에서 새 시리즈를 글 여러 개와 함께 만들고, 더 넣고, 비공개 글도 보인다. 글쓰기에 시리즈 칸. */
    @Test
    void manageTabCreatesSeriesWithManyPosts() throws Exception {
        long me = member("tabmng");
        long other = member("tabmng2");
        long a = posts.published(me, "관리 가", "본문", 1, T);
        long b = posts.published(me, "관리 나", "본문", 1, T.plusSeconds(1));
        long c = posts.published(me, "관리 다", "본문", 1, T.plusSeconds(2));
        long theirs = posts.published(other, "남의 글", "본문", 1, T);
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", b);
        assertThat(page("/manage/posts", me)).contains("href=\"/manage/series\"");
        mockMvc.perform(post("/manage/series").param("newTitle", "관리 시리즈").param("postIds", String.valueOf(a), String.valueOf(b))
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        mockMvc.perform(post("/manage/series/{id}/add", sid).param("postIds", String.valueOf(c))
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForList("SELECT post_id FROM series_post WHERE series_id = ? ORDER BY position", Long.class, sid))
                .containsExactly(a, b, c);
        String manage = page("/manage/series", me);
        assertThat(manage).contains("관리 시리즈").contains("관리 나").contains("공개 안 됨").contains("3편");
        // 남의 글은 넣지 못하고, 글 없이·이름 없이 만들면 안내
        mockMvc.perform(post("/manage/series/{id}/add", sid).param("postIds", String.valueOf(theirs))
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().isNotFound());
        // 107: 글 없이도 만든다(빈 시리즈는 공개 화면에 안 보임). 있는 시리즈에 아무것도 안 고르면 안내
        mockMvc.perform(post("/manage/series").param("newTitle", "빈 시리즈").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        assertThat(page("/manage/series", me)).contains("빈 시리즈");
        assertThat(page("/tabmng/series")).doesNotContain("빈 시리즈");
        mockMvc.perform(post("/manage/series/{id}/add", sid).with(csrf()).with(TestAuth.member(me)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/manage/series?error=SERIES_POSTS_REQUIRED"));
        assertThat(page("/manage/series?error=SERIES_POSTS_REQUIRED", me)).contains("넣을 글을 하나 이상 골라 주세요");
        // 이름 바꾸기(같은 이름은 안내), 삭제는 묶음만 풀고 글은 남는다
        // 107: 이름·설명·대표 사진. 사진은 이 시리즈 글의 사진만(다른 주소는 자동으로), 없으면 1편부터 첫 사진
        jdbc.update("UPDATE post SET thumbnail_url = '/media/c.webp' WHERE id = ?", c);
        assertThat(page("/tabmng/series")).contains("src=\"/media/c.webp\"");
        mockMvc.perform(post("/manage/series/{id}/edit", sid).param("title", "새 이름 시리즈").param("description", "한 줄 설명")
                .param("coverUrl", "https://evil.example/x.png").with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForMap("SELECT title, description, cover_url FROM series WHERE id = ?", sid))
                .containsEntry("title", "새 이름 시리즈").containsEntry("description", "한 줄 설명").containsEntry("cover_url", null);
        mockMvc.perform(post("/manage/series/{id}/edit", sid).param("title", "새 이름 시리즈").param("description", "한 줄 설명").param("coverUrl", "/media/c.webp")
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT cover_url FROM series WHERE id = ?", String.class, sid)).isEqualTo("/media/c.webp");
        mockMvc.perform(post("/manage/series/{id}/edit", sid).param("title", "x").with(csrf()).with(TestAuth.member(other)))
                .andExpect(status().isNotFound());
        assertThat(page("/tabmng/series/" + sid)).contains("한 줄 설명");
        assertThat(mockMvc.perform(get("/explore?tab=series")).andReturn().getResponse().getContentAsString()).contains("한 줄 설명");
        // 글쓰기 발행 설정에 시리즈 칸(이 글이 든 시리즈가 골라져 있다)
        assertThat(page("/write/" + c, me)).contains("id=\"series-pick\"").contains("+ 새 시리즈 만들기")
                .containsPattern("value=\"" + sid + "\" selected=\"selected\">새 이름 시리즈");
        mockMvc.perform(post("/manage/series/{id}/delete", sid).with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM series WHERE id = ?", Integer.class, sid)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE id IN (?, ?, ?) AND deleted_at IS NULL", Integer.class, a, b, c)).isEqualTo(3);
    }

    /** 10-16: 글 넣기는 고른 순서(화면은 최신 글이 위)가 아니라 공개한 순서대로 뒤에 붙는다. 관리 탭 버튼은 늘 같은 자리. */
    @Test
    void addingManyPostsKeepsPublishOrder() throws Exception {
        long me = member("taborder2");
        long day1 = posts.published(me, "1일차", "본문", 1, T);
        long day2 = posts.published(me, "2일차", "본문", 1, T.plusSeconds(60));
        long day3 = posts.published(me, "3일차", "본문", 1, T.plusSeconds(120));
        mockMvc.perform(post("/manage/series").param("newTitle", "제작기").param("postIds", String.valueOf(day3), String.valueOf(day1))
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        mockMvc.perform(post("/manage/series/{id}/add", sid).param("postIds", String.valueOf(day2))
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForList("SELECT post_id FROM series_post WHERE series_id = ? ORDER BY position", Long.class, sid))
                .containsExactly(day1, day3, day2);
        String manage = page("/manage/series", me);
        // 첫 줄의 앞으로·끝 줄의 뒤로는 숨기지 않고 비활성
        assertThat(manage).contains("aria-label=\"1일차 앞으로\" disabled=\"disabled\"").contains("aria-label=\"2일차 뒤로\" disabled=\"disabled\"")
                .contains("aria-label=\"3일차 앞으로\">");
    }

    /** 112 강성찬 개인 확장: '0편부터 시작'을 켜면 시리즈 화면·글 화면 상자·관리 탭 번호가 0부터. 기본은 1편부터. */
    @Test
    void seriesCanStartAtZero() throws Exception {
        long me = member("tabzero");
        long p0 = posts.published(me, "강의 영", "본문", 1, T);
        long p1 = posts.published(me, "강의 일", "본문", 1, T.plusSeconds(60));
        long p2 = posts.published(me, "강의 이", "본문", 1, T.plusSeconds(120));
        mockMvc.perform(post("/manage/series").param("newTitle", "강의").param("postIds", String.valueOf(p0), String.valueOf(p1), String.valueOf(p2))
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        long sid = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        assertThat(page("/tabzero/series/" + sid)).contains(">1편<").doesNotContain(">0편<");
        mockMvc.perform(post("/manage/series/{id}/edit", sid).param("title", "강의").param("startZero", "on")
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(page("/tabzero/series/" + sid)).contains(">0편<").contains(">2편<").doesNotContain(">3편<");
        assertThat(page("/tabzero/series/" + sid + "?order=latest").indexOf(">2편<"))
                .isLessThan(page("/tabzero/series/" + sid + "?order=latest").indexOf(">0편<"));
        assertThat(page("/tabzero/posts/" + p1)).contains(">1/2<").contains("series-box-no\">0<");
        assertThat(page("/manage/series", me)).contains(">0편<").contains("name=\"startZero\" value=\"on\" checked=\"checked\"");
        // 끄면(체크 칸이 안 오면) 다시 1편부터
        mockMvc.perform(post("/manage/series/{id}/edit", sid).param("title", "강의").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        assertThat(page("/tabzero/posts/" + p1)).contains(">2/3<");
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
