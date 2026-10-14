package com.team.blog.tistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.guestbook.application.GuestbookPurgeStep;
import com.team.blog.schedule.application.ScheduleService;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** 037~044 티스토리 참고 기능(강성찬 개인 확장). */
class TistoryFeaturesIT extends IntegrationTestBase {

    private static final Instant T = Instant.parse("2026-10-12T00:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ScheduleService schedules;

    @Autowired
    GuestbookPurgeStep guestbookPurge;

    private long member(String handle) {
        return members.localMember(handle, handle.substring(0, Math.min(10, handle.length())), handle + "@example.com",
                "Blog#2026ok", true);
    }

    private String page(String url, long viewer) throws Exception {
        return mockMvc.perform(get(url).with(TestAuth.member(viewer))).andReturn().getResponse().getContentAsString();
    }

    private String page(String url) throws Exception {
        return mockMvc.perform(get(url)).andReturn().getResponse().getContentAsString();
    }

    private void tag(long postId, String name, int position) {
        jdbc.update("INSERT INTO tag (name) VALUES (?) ON CONFLICT (name) DO NOTHING", name);
        jdbc.update("INSERT INTO post_tag (post_id, tag_id, position) SELECT ?, id, ? FROM tag WHERE name = ?", postId, position, name);
    }

    @Test
    void categoriesFilterBlogAndOnlyOwnerAssigns() throws Exception {
        long me = member("catme");
        long other = member("catother");
        long a = posts.published(me, "자바 공부", "본문", 1, T.minusSeconds(300));
        long b = posts.published(me, "산책 일기", "본문", 1, T.minusSeconds(200));
        mockMvc.perform(post("/settings/categories").param("name", "개발").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/settings/categories").param("name", "개발").with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().isBadRequest());
        long dev = jdbc.queryForObject("SELECT id FROM category WHERE member_id = ?", Long.class, me);
        mockMvc.perform(post("/posts/{id}/category", a).param("categoryId", String.valueOf(dev)).param("back", "/")
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        // 남의 글에는 못 단다
        mockMvc.perform(post("/posts/{id}/category", a).param("categoryId", String.valueOf(dev))
                .with(csrf()).with(TestAuth.member(other))).andExpect(status().isNotFound());
        String blog = page("/catme");
        assertThat(blog).contains("개발 1").contains("자바 공부").contains("산책 일기");
        String filtered = page("/catme?category=" + dev);
        assertThat(filtered.substring(filtered.indexOf("id=\"card-grid\""))).contains("자바 공부").doesNotContain("산책 일기");
        assertThat(page("/catme/posts/" + a)).contains("📁 개발");
        // 지우면 글은 분류 없음
        mockMvc.perform(post("/settings/categories/{id}/delete", dev).with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post_category", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ?", Integer.class, b)).isEqualTo(1);
    }

    @Test
    void prevNextRelatedAndToc() throws Exception {
        long me = member("navme");
        long first = posts.published(me, "첫 번째 글", "본문", 1, T.minusSeconds(300));
        long second = posts.published(me, "두 번째 글", "본문", 1, T.minusSeconds(200));
        long third = posts.published(me, "세 번째 글", "본문", 1, T.minusSeconds(100));
        long hidden = posts.published(me, "나만 보는 글", "본문", 1, T.minusSeconds(150));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", hidden);
        tag(second, "spring", 0);
        tag(third, "spring", 0);
        tag(hidden, "spring", 0);
        String html = page("/navme/posts/" + second);
        assertThat(html).contains("이전 글").contains("첫 번째 글").contains("다음 글").contains("세 번째 글")
                .contains("관련 글").doesNotContain("나만 보는 글").contains("id=\"post-toc\"").contains("/js/post/toc-");
        assertThat(page("/navme/posts/" + first)).doesNotContain("이전 글</span>");
    }

    @Test
    void relatedPostsAreFourByTagOverlapThenAuthorThenPopular() throws Exception {
        long a = member("rela");
        long b = member("relb");
        long v = member("relv");
        long src = posts.published(a, "지금 글", "본문", 1, T.minusSeconds(500));
        tag(src, "spring", 0);
        tag(src, "java", 1);
        long twoTags = posts.published(b, "태그 둘 겹침", "본문", 1, T.minusSeconds(900));
        tag(twoTags, "spring", 0);
        tag(twoTags, "java", 1);
        long oneTag = posts.published(b, "태그 하나 겹침", "본문", 1, T.minusSeconds(800));
        tag(oneTag, "spring", 0);
        posts.published(a, "같은 글쓴이 글", "본문", 1, T.minusSeconds(700));
        long popular = posts.published(b, "인기 글", "본문", 1, T.minusSeconds(600));
        jdbc.update("UPDATE post SET like_count = 9 WHERE id = ?", popular);
        posts.published(b, "그냥 최신 글", "본문", 1, T.minusSeconds(10));
        long hidden = posts.published(b, "비공개 같은 태그", "본문", 1, T.minusSeconds(100));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", hidden);
        tag(hidden, "spring", 0);
        long friends = posts.published(b, "친구 공개 같은 태그", "본문", 1, T.minusSeconds(100));
        jdbc.update("UPDATE post SET visibility = 'FRIENDS' WHERE id = ?", friends);
        tag(friends, "java", 0);
        String section = mockMvc.perform(get("/api/posts/{id}/related", src).with(TestAuth.member(v)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        // 겹치는 태그 수 → 같은 글쓴이 → 최근 인기 순, 4개까지(그냥 최신 글은 다섯째라 빠짐)
        assertThat(section.indexOf("태그 둘 겹침")).isPositive().isLessThan(section.indexOf("태그 하나 겹침"));
        assertThat(section.indexOf("태그 하나 겹침")).isLessThan(section.indexOf("같은 글쓴이 글"));
        assertThat(section.indexOf("같은 글쓴이 글")).isLessThan(section.indexOf("인기 글"));
        assertThat(section).doesNotContain("그냥 최신 글").doesNotContain("지금 글").doesNotContain("비공개 같은 태그")
                .doesNotContain("친구 공개 같은 태그");
        assertThat(section).contains("\"nextCursor\":null");
        // 글 화면은 관련 글 칸만 두고 스크립트가 한 번 불러온다(무한 스크롤 API는 없어짐)
        assertThat(page("/rela/posts/" + src, v)).contains("관련 글").doesNotContain("관심 있을 만한 글")
                .contains("data-list-api=\"/api/posts/" + src + "/related\"");
        mockMvc.perform(get("/api/posts/{id}/more", src)).andExpect(status().isNotFound());
        // 비공개 글 번호로 불러도 그 글의 태그·글쓴이는 쓰지 않는다
        assertThat(mockMvc.perform(get("/api/posts/{id}/related", hidden)).andReturn().getResponse().getContentAsString())
                .doesNotContain("비공개 같은 태그");
    }

    @Test
    void guestbookSecretAndDelete() throws Exception {
        long owner = member("gbowner");
        long guest = member("gbguest");
        long stranger = member("gbstranger");
        mockMvc.perform(post("/gbowner/guestbook").param("content", "안녕하세요 <b>반가워요</b>").with(csrf())
                .with(TestAuth.member(guest))).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/gbowner/guestbook").param("content", "비밀 이야기").param("secret", "on").with(csrf())
                .with(TestAuth.member(guest))).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/gbowner/guestbook").param("content", "가".repeat(301)).with(csrf())
                .with(TestAuth.member(guest))).andExpect(status().isBadRequest());
        assertThat(page("/gbowner/guestbook")).contains("안녕하세요 &lt;b&gt;반가워요&lt;/b&gt;").doesNotContain("비밀 이야기")
                .contains("비밀글이에요");
        assertThat(page("/gbowner/guestbook", stranger)).doesNotContain("비밀 이야기");
        assertThat(page("/gbowner/guestbook", owner)).contains("비밀 이야기");
        assertThat(page("/gbowner/guestbook", guest)).contains("비밀 이야기");
        long id = jdbc.queryForObject("SELECT min(id) FROM guestbook_entry", Long.class);
        mockMvc.perform(post("/gbowner/guestbook/{id}/delete", id).with(csrf()).with(TestAuth.member(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/gbowner/guestbook/{id}/delete", id).with(csrf()).with(TestAuth.member(owner)))
                .andExpect(status().is3xxRedirection());
        guestbookPurge.purge(guest);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM guestbook_entry", Integer.class)).isZero();
    }

    @Test
    void seriesOrderAndNavigation() throws Exception {
        long me = member("serme");
        long p1 = posts.published(me, "입문 1편", "본문", 1, T.minusSeconds(300));
        long p2 = posts.published(me, "입문 2편", "본문", 1, T.minusSeconds(200));
        long p3 = posts.published(me, "입문 3편", "본문", 1, T.minusSeconds(100));
        mockMvc.perform(post("/posts/{id}/series", p1).param("seriesId", "new").param("newTitle", "스프링 입문")
                .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        long series = jdbc.queryForObject("SELECT id FROM series WHERE member_id = ?", Long.class, me);
        for (long p : new long[] {p2, p3}) {
            mockMvc.perform(post("/posts/{id}/series", p).param("seriesId", String.valueOf(series))
                    .with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        }
        String html = page("/serme/posts/" + p2);
        assertThat(html).contains("📚 스프링 입문").contains("(2/3)").contains("이전편: 입문 1편").contains("다음편: 입문 3편");
        String list = page("/serme/series/" + series);
        assertThat(list.indexOf("입문 1편")).isLessThan(list.indexOf("입문 2편"));
        assertThat(list.indexOf("입문 2편")).isLessThan(list.indexOf("입문 3편"));
        // 다 빼면 시리즈도 사라진다
        for (long p : new long[] {p1, p2, p3}) {
            mockMvc.perform(post("/posts/{id}/series/remove", p).with(csrf()).with(TestAuth.member(me)))
                    .andExpect(status().is3xxRedirection());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM series", Integer.class)).isZero();
    }

    @Test
    void scheduledDraftPublishesAtTime() throws Exception {
        long me = member("schme");
        long draft = posts.draft(me, "예약한 글", "내일 아침 공개", 1);
        clock.set(T);
        mockMvc.perform(post("/api/posts/{id}/schedule", draft).with(csrf()).with(TestAuth.member(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publishAt\":\"" + T.plus(1, ChronoUnit.MINUTES) + "\",\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/posts/{id}/schedule", draft).with(csrf()).with(TestAuth.member(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publishAt\":\"" + T.plus(1, ChronoUnit.HOURS) + "\",\"visibility\":\"PUBLIC\",\"tags\":[\"아침\"]}"))
                .andExpect(status().isOk());
        assertThat(page("/manage/posts?tab=drafts", me)).contains("⏰ 예약");
        assertThat(schedules.runDue()).isZero();
        assertThat(page("/schme")).doesNotContain("예약한 글");
        clock.set(T.plus(61, ChronoUnit.MINUTES));
        assertThat(schedules.runDue()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM post WHERE id = ?", String.class, draft)).isEqualTo("PUBLISHED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post_schedule", Integer.class)).isZero();
        assertThat(page("/schme")).contains("예약한 글");
        // 취소는 본인만
        long other = posts.draft(me, "다른 글", "본문", 1);
        mockMvc.perform(post("/api/posts/{id}/schedule", other).with(csrf()).with(TestAuth.member(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"publishAt\":\"" + T.plus(3, ChronoUnit.HOURS) + "\"}")).andExpect(status().isOk());
        long stranger = member("schstranger");
        mockMvc.perform(delete("/api/posts/{id}/schedule", other).with(csrf()).with(TestAuth.member(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/posts/{id}/schedule", other).with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().isNoContent());
    }

    @Test
    void secretCommentsVisibleToWriterAndPostAuthorOnly() throws Exception {
        long author = member("scauthor");
        long writer = member("scwriter");
        long stranger = member("scstranger");
        long postId = posts.published(author, "비밀 댓글 시험", "본문", 1, T.minusSeconds(60));
        mockMvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).with(TestAuth.member(writer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"글쓴이만 보세요\",\"secret\":true}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).with(TestAuth.member(writer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"모두 보는 댓글\"}"))
                .andExpect(status().isCreated());
        assertThat(page("/scauthor/posts/" + postId, stranger)).contains("🔒 비밀 댓글이에요").doesNotContain("글쓴이만 보세요")
                .contains("모두 보는 댓글");
        assertThat(page("/scauthor/posts/" + postId)).doesNotContain("글쓴이만 보세요");
        assertThat(page("/scauthor/posts/" + postId, author)).contains("글쓴이만 보세요");
        assertThat(page("/scauthor/posts/" + postId, writer)).contains("글쓴이만 보세요");
        String api = mockMvc.perform(get("/api/posts/{id}/comments", postId).with(TestAuth.member(stranger))).andReturn()
                .getResponse().getContentAsString();
        assertThat(api).doesNotContain("글쓴이만 보세요").contains("SECRET");
        // 비밀 댓글에 단 답글도 비밀
        long secretId = jdbc.queryForObject("SELECT id FROM comment WHERE secret", Long.class);
        mockMvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).with(TestAuth.member(author))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"답글이에요\",\"replyToCommentId\":" + secretId + "}")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM comment WHERE secret", Integer.class)).isEqualTo(2);
        assertThat(page("/scauthor/posts/" + postId, stranger)).doesNotContain("답글이에요");
        assertThat(jdbc.queryForObject("SELECT comment_count FROM post WHERE id = ?", Integer.class, postId)).isEqualTo(3);
        // 043: 보통 댓글에도 비밀 답글을 달 수 있다(글쓴이·원댓글 쓴 사람·답글 쓴 사람만 본다)
        long openId = jdbc.queryForObject("SELECT id FROM comment WHERE content = '모두 보는 댓글'", Long.class);
        mockMvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).with(TestAuth.member(stranger))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"몰래 단 답글\",\"replyToCommentId\":" + openId + ",\"secret\":true}"))
                .andExpect(status().isCreated());
        long outsider = member("scoutsider");
        assertThat(page("/scauthor/posts/" + postId, outsider)).doesNotContain("몰래 단 답글").contains("모두 보는 댓글");
        assertThat(page("/scauthor/posts/" + postId, writer)).contains("몰래 단 답글");
        assertThat(page("/scauthor/posts/" + postId, author)).contains("몰래 단 답글");
        assertThat(page("/scauthor/posts/" + postId, stranger)).contains("몰래 단 답글");
    }

    @Test
    void rssListsPublicPostsOnly() throws Exception {
        long me = member("rssme");
        posts.published(me, "공개 글 <제목>", "본문", 1, T.minusSeconds(100));
        long priv = posts.published(me, "비공개 글", "본문", 1, T.minusSeconds(50));
        jdbc.update("UPDATE post SET visibility = 'PRIVATE' WHERE id = ?", priv);
        var res = mockMvc.perform(get("/rssme/rss")).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(res.getContentType()).startsWith("application/rss+xml");
        String xml = res.getContentAsString();
        assertThat(xml).contains("<rss version=\"2.0\">").contains("공개 글 &lt;제목&gt;").doesNotContain("비공개 글")
                .contains("/rssme/posts/");
        assertThat(mockMvc.perform(get("/rss")).andReturn().getResponse().getContentAsString()).contains("공개 글 &lt;제목&gt;");
        mockMvc.perform(get("/nobodyhere/rss")).andExpect(status().isNotFound());
        assertThat(page("/rssme")).contains("/rssme/rss").contains("/rssme/guestbook").contains("/rssme/series");
    }
}
