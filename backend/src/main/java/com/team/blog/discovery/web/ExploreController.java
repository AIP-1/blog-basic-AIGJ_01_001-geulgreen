package com.team.blog.discovery.web;

import com.team.blog.discovery.application.RecommendedBlogQuery;
import com.team.blog.discovery.application.NewcomerQuery;
import com.team.blog.discovery.application.RecommendedPostQuery;
import com.team.blog.discovery.application.TrendingService;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.FeedCursor;
import com.team.blog.post.application.PostCard;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.series.application.SeriesService;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.error.SnapshotExpiredException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 079 강성찬 개인 확장: 탐색({@code GET /explore}). 홈 오른쪽 칸(휴대폰에서는 끝없는 목록 아래라 닿지 못함)의
 * 인기 태그·추천 글·추천 블로그와 새로 온 작가를 한 화면에 모은다. 비회원도 본다(팔로우 자리는 로그인 링크).
 * 086(강성찬 개인 확장): 피드 형식으로. 위에 추천 블로그 줄(팔로우 버튼), 아래 추천 글 카드 목록(무한 스크롤, 커서는 추천 위치).
 * 추천 글은 내 글·팔로우한 사람 글(피드에 나옴)을 뺀 077 규칙 순서.
 * 086: 홈에 있던 [트렌딩] 탭(019, 스냅샷 커서)과 새로 온 작가(036)도 여기로. 인기 태그는 검색 창·/tags에만(여기는 링크).
 */
@Controller // NOSONAR /explore 화면(뷰 이름)과 JSON API를 함께 내므로 @RestController가 아니다
public class ExploreController {

    private final RecommendedPostQuery recommendedPostQuery;
    private final RecommendedBlogQuery recommendedBlogQuery;
    private final NewcomerQuery newcomerQuery;
    private final TrendingService trendingService;
    private final PostListQuery listQuery;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final SeriesService seriesService;

    public ExploreController(RecommendedPostQuery recommendedPostQuery, RecommendedBlogQuery recommendedBlogQuery,
                             NewcomerQuery newcomerQuery, TrendingService trendingService, PostListQuery listQuery,
                             CurrentUserProvider currentUserProvider, Clock clock, SeriesService seriesService) {
        this.seriesService = seriesService;
        this.recommendedPostQuery = recommendedPostQuery;
        this.recommendedBlogQuery = recommendedBlogQuery;
        this.newcomerQuery = newcomerQuery;
        this.trendingService = trendingService;
        this.listQuery = listQuery;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @GetMapping("/explore")
    public String explore(@RequestParam(value = "cursor", required = false) String cursor,
                          @RequestParam(value = "tab", required = false) String tab,
                          @RequestParam(value = "expired", required = false) String expired,
                          @RequestParam(value = "sort", required = false) String sort,
                          @RequestParam(value = "offset", required = false) Integer offset, Model model) {
        model.addAttribute("seriesTab", seriesService.enabled());
        if ("series".equals(tab) && seriesService.enabled()) {
            // 104 강성찬 개인 확장: 모든 블로그의 시리즈를 골라 보는 탭(정렬은 태그 모아 보기와 같은 인기순·최근순)
            String s = "recent".equals(sort) ? sort : "popular";
            model.addAttribute("tab", "series");
            model.addAttribute("sort", s);
            // 108: 더 보기(60개씩, 하나 더 읽어 다음이 있는지 안다)
            int from = offset == null || offset < 0 ? 0 : offset;
            List<SeriesService.Browse> rows = seriesService.browse(s, from, SeriesService.BROWSE_PAGE + 1);
            model.addAttribute("seriesList", rows.stream().limit(SeriesService.BROWSE_PAGE).toList());
            model.addAttribute("seriesNext", rows.size() > SeriesService.BROWSE_PAGE ? from + SeriesService.BROWSE_PAGE : null);
            model.addAttribute("seriesFrom", from);
            model.addAttribute("people", List.of());
            return "explore";
        }
        long viewer = viewer();
        boolean trending = "trending".equals(tab);
        boolean snapshotExpired = expired != null;
        CardPage page;
        try {
            page = trending ? trendingThenLatest(cursor) : page(viewer, cursor);
        } catch (PostContentException | SnapshotExpiredException e) {
            snapshotExpired = snapshotExpired || e instanceof SnapshotExpiredException;
            page = trending ? trendingThenLatest(null) : page(viewer, null);
            cursor = null;
        }
        boolean continued = cursor != null && !cursor.isEmpty();
        CardModel.fill(model, page, clock.instant());
        model.addAttribute("tab", trending ? "trending" : "recommended");
        model.addAttribute("snapshotExpired", trending && snapshotExpired);
        model.addAttribute("continued", continued);
        model.addAttribute("listApi", trending ? "/api/posts/trending" : "/api/explore");
        model.addAttribute("moreLinkBase", trending ? "?tab=trending&cursor=" : "?cursor=");
        boolean firstRecommended = !trending && !continued;
        model.addAttribute("people", firstRecommended ? people(viewer) : List.of());
        // 강성찬 개인 확장(10-16): [추천] 맨 위 "첫 댓글을 기다리는 글"(없으면 칸을 숨긴다)
        List<Long> waitingIds = firstRecommended ? recommendedPostQuery.waitingFirstComment(viewer, WAITING) : List.of();
        Map<Long, PostCard> waitingCards = listQuery.cardsByIds(waitingIds);
        List<PostCard> waiting = waitingIds.stream().map(waitingCards::get).filter(Objects::nonNull).toList();
        java.util.Map<Long, String> dates = new java.util.HashMap<>(java.util.Objects.requireNonNullElse(
                (java.util.Map<Long, String>) model.getAttribute("cardDates"), java.util.Map.of()));
        waiting.forEach(c -> dates.put(c.id(), com.team.blog.post.application.CardDates.label(c.firstPublicAt(), clock.instant())));
        model.addAttribute("cardDates", dates);
        model.addAttribute("waiting", waiting);
        return "explore";
    }

    /** 사람 추천 한 줄(10-15 탐색 정리): 추천 블로그와 새로 온 작가를 합친 것. {@code newcomer}면 "새 작가" 표시. */
    public record Person(String handle, com.team.blog.account.application.ProfileAvatar avatar, boolean newcomer) {
    }

    static final int PEOPLE = 8;

    /** 추천 블로그 먼저, 그다음 새로 온 작가 중 아직 없는 사람. 같은 사람은 한 번만, 나와 이미 팔로우한 사람은 빼고, 최대 {@value #PEOPLE}명. */
    private List<Person> people(long viewer) {
        java.util.Map<String, Person> out = new java.util.LinkedHashMap<>();
        java.util.Set<String> fresh = new java.util.HashSet<>();
        List<com.team.blog.discovery.application.NewcomerQuery.Newcomer> newcomers = newcomerQuery.enabled() ? newcomerQuery.recent(viewer) : List.of();
        newcomers.forEach(n -> fresh.add(n.handle()));
        recommendedBlogQuery.forMember(viewer).forEach(b -> out.putIfAbsent(b.handle(), new Person(b.handle(), b.avatar(), fresh.contains(b.handle()))));
        newcomers.forEach(n -> out.putIfAbsent(n.handle(), new Person(n.handle(), n.avatar(), true)));
        return out.values().stream().limit(PEOPLE).toList();
    }

    @GetMapping("/api/explore")
    @ResponseBody
    public CardPage api(@RequestParam(value = "cursor", required = false) String cursor) {
        return page(viewer(), cursor);
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

    private long viewer() {
        return currentUserProvider.current().map(CurrentUser::memberId).orElse(-1L);
    }

    static final int WAITING = 4;

    /** 10-19(chan "크기는 작게 해서 많이 추천"): 탐색 카드는 PC 한 줄 5장이라 한 번에 15개(세 줄). */
    static final int PAGE = 15;

    private CardPage page(long viewer, String cursor) {
        int offset = 0;
        if (cursor != null && !cursor.isEmpty()) {
            try {
                offset = Integer.parseInt(cursor);
            } catch (NumberFormatException e) {
                throw new PostContentException(FeedCursor.INVALID);
            }
            if (offset < 0) {
                throw new PostContentException(FeedCursor.INVALID);
            }
        }
        // 위 "첫 댓글을 기다리는 글"에 나온 글은 아래 추천 글에서 뺀다(같은 글이 두 번 보이지 않게)
        java.util.Set<Long> waiting = new java.util.LinkedHashSet<>(recommendedPostQuery.waitingFirstComment(viewer, WAITING));
        List<Long> ids = recommendedPostQuery.pool(viewer).stream().filter(id -> !waiting.contains(id)).toList();
        if (ids.isEmpty()) {
            // 10-19: 남은 글이 위 칸의 글뿐이면 목록이 비지 않게 그 글들을 아래에도 둔다
            ids = List.copyOf(waiting);
        }
        int end = Math.min(offset + PAGE, ids.size());
        List<Long> slice = offset >= end ? List.of() : ids.subList(offset, end);
        Map<Long, PostCard> cards = listQuery.cardsByIds(slice);
        return new CardPage(slice.stream().map(cards::get).filter(Objects::nonNull).toList(),
                end < ids.size() ? String.valueOf(end) : null);
    }
}
