package com.team.blog.discovery.web;

import com.team.blog.discovery.application.TrendingService;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.error.SnapshotExpiredException;
import java.time.Clock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 홈({@code GET /}): 전체 공개 글 목록 첫 9개를 서버가 완성해서 보낸다(009 FR-008). 스크립트가 없으면 [더 보기]가
 * {@code /?cursor=…} 링크로 다음 9개를 보여 준다. 잘못된 커서면 처음부터 보여 준다(API는 400).
 * 019: {@code ?tab=trending}이면 트렌딩(스냅샷 커서). 보던 순위가 사라졌으면 "순위가 새로 바뀌었어요"와 함께 처음부터.
 * 로그아웃 직후에는 1회용 플래시 {@code logoutCleanupMemberId}가 들어와 브라우저 임시 데이터 정리를 한 번 더 실행한다(001).
 */
@Controller
public class HomeController {

    /** 시안(10-08) 소개 사진: static/images/hero.webp가 있으면 배경으로, 없으면 그린 고양이 그림. */
    private static final boolean HERO_PHOTO = new org.springframework.core.io.ClassPathResource("static/images/hero.webp").exists();

    private final PostListQuery listQuery;
    private final TrendingService trendingService;
    private final Clock clock;
    private final com.team.blog.discovery.application.NewcomerQuery newcomerQuery;
    private final com.team.blog.post.application.TagListingQuery tagListingQuery;
    private final com.team.blog.tag.application.icon.TagIconService tagIcons;
    private final com.team.blog.discovery.application.RecommendedBlogQuery recommendedBlogQuery;
    private final com.team.blog.shared.security.CurrentUserProvider currentUserProvider;

    public HomeController(PostListQuery listQuery, TrendingService trendingService, Clock clock,
                          com.team.blog.discovery.application.NewcomerQuery newcomerQuery,
                          com.team.blog.post.application.TagListingQuery tagListingQuery,
                          com.team.blog.tag.application.icon.TagIconService tagIcons,
                          com.team.blog.discovery.application.RecommendedBlogQuery recommendedBlogQuery,
                          com.team.blog.shared.security.CurrentUserProvider currentUserProvider) {
        this.tagIcons = tagIcons;
        this.recommendedBlogQuery = recommendedBlogQuery;
        this.currentUserProvider = currentUserProvider;
        this.tagListingQuery = tagListingQuery;
        this.newcomerQuery = newcomerQuery;
        this.listQuery = listQuery;
        this.trendingService = trendingService;
        this.clock = clock;
    }

    @GetMapping("/")
    public String home(@RequestParam(value = "cursor", required = false) String cursor,
                       @RequestParam(value = "tab", required = false) String tab,
                       @RequestParam(value = "expired", required = false) String expired, Model model) {
        boolean trending = "trending".equals(tab);
        boolean snapshotExpired = expired != null;
        CardPage page;
        try {
            page = trending ? trendingThenLatest(cursor) : listQuery.feed(cursor);
        } catch (PostContentException | SnapshotExpiredException e) {
            snapshotExpired = snapshotExpired || e instanceof SnapshotExpiredException;
            page = trending ? trendingThenLatest(null) : listQuery.feed(null);
            cursor = null;
        }
        CardModel.fill(model, page, clock.instant());
        model.addAttribute("tab", trending ? "trending" : "latest");
        model.addAttribute("snapshotExpired", trending && snapshotExpired);
        model.addAttribute("continued", cursor != null && !cursor.isEmpty());
        model.addAttribute("listApi", trending ? "/api/posts/trending" : "/api/posts");
        model.addAttribute("moreLinkBase", trending ? "?tab=trending&cursor=" : "?cursor=");
        model.addAttribute("heroPhoto", HERO_PHOTO);
        // 홈 리뉴얼(강성찬 개인 확장): 오른쪽 인기 태그와 목록 위 태그 탭
        java.util.List<com.team.blog.post.application.TagListingQuery.TagCount> popular = tagListingQuery.top(8);
        model.addAttribute("popularTags", popular);
        model.addAttribute("tagStyles", tagIcons.styles(popular.stream().map(com.team.blog.post.application.TagListingQuery.TagCount::name).toList()));
        // 036 강성찬 개인 확장: 새로 온 작가(최신 탭 첫 화면)
        if (newcomerQuery.enabled() && !trending && (cursor == null || cursor.isEmpty())) {
            model.addAttribute("newcomers", newcomerQuery.recent());
        }
        // 067 후속(강성찬 개인 확장): 오른쪽 칸 추천 블로그(피드 068과 같은 고르기, 비회원은 팔로우 대신 로그인으로)
        model.addAttribute("recommended", recommendedBlogQuery.forMember(
                currentUserProvider.current().map(com.team.blog.shared.security.CurrentUser::memberId).orElse(-1L)));
        return "home";
    }

    @GetMapping("/api/posts/trending")
    @ResponseBody
    public CardPage trending(@RequestParam(value = "cursor", required = false) String cursor) {
        return trendingThenLatest(cursor);
    }

    /** 067 후속(강성찬 개인 확장): 트렌딩 다음 커서 중 이 머리로 시작하면 최신 글 목록으로 이어진다. */
    static final String THEN_LATEST = "latest:";

    /**
     * 트렌딩 순위(최근 7일·반응 있는 글)가 끝나면 최신 글로 이어 붙여 무한 스크롤이 멈추지 않게 한다.
     * 이미 본 글은 화면(list-more.js)이 건너뛴다. 트렌딩 글이 하나도 없으면 빈 화면 안내를 그대로 둔다.
     */
    private CardPage trendingThenLatest(String cursor) {
        if (cursor != null && cursor.startsWith(THEN_LATEST)) {
            String rest = cursor.substring(THEN_LATEST.length());
            CardPage feed = listQuery.feed(rest.isEmpty() ? null : rest);
            return new CardPage(feed.items(), feed.nextCursor() == null ? null : THEN_LATEST + feed.nextCursor());
        }
        CardPage page = trendingService.page(cursor);
        return page.nextCursor() != null || page.items().isEmpty() ? page : new CardPage(page.items(), THEN_LATEST);
    }
}
