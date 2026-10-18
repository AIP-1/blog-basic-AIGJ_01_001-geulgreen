package com.team.blog.discovery.web;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.Clock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 개인 블로그({@code GET /@{handle}}, 009 FR-016~FR-020): 프로필 머리말 + 공개 글 수 + 그 회원의 공개 글 카드.
 * 작성자 본인이 봐도 비공개 글은 나오지 않는다. 없는·형식 밖·탈퇴 주소는 404, 대문자 주소는 필터가 301.
 * 100 강성찬 개인 확장: 머리말 아래 탭 — 홈(선반: 고정 글·기록·짧은 기록·최근 글·시리즈), 글({@code /posts}), 시리즈.
 */
@Controller
public class BlogPageController {

    /** 홈 선반 하나에 보여 줄 개수(넓은 화면 한 줄). */
    static final int HOME_SHELF = 4;

    private final BlogOwnerResolver blogOwnerResolver;
    private final PostListQuery listQuery;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final com.team.blog.post.application.TagListingQuery tagListingQuery;
    private final SearchController searchController;
    private final com.team.blog.discovery.application.ActivityQuery activityQuery;
    private final com.team.blog.note.application.NoteService noteService;
    private final com.team.blog.category.application.CategoryService categoryService;
    private final com.team.blog.discovery.application.PinService pins;
    private final com.team.blog.series.application.SeriesService series;
    private final BlogHeaderModel header;

    public BlogPageController(BlogOwnerResolver blogOwnerResolver, PostListQuery listQuery,
                              CurrentUserProvider currentUserProvider, Clock clock,
                              com.team.blog.post.application.TagListingQuery tagListingQuery,
                              SearchController searchController,
                              com.team.blog.discovery.application.ActivityQuery activityQuery,
                              com.team.blog.note.application.NoteService noteService,
                              com.team.blog.category.application.CategoryService categoryService,
                              com.team.blog.discovery.application.PinService pins,
                              com.team.blog.series.application.SeriesService series,
                              BlogHeaderModel header) {
        this.blogOwnerResolver = blogOwnerResolver;
        this.listQuery = listQuery;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.tagListingQuery = tagListingQuery;
        this.searchController = searchController;
        this.activityQuery = activityQuery;
        this.noteService = noteService;
        this.categoryService = categoryService;
        this.pins = pins;
        this.series = series;
        this.header = header;
    }

    /** 100 강성찬 개인 확장: 홈 탭. 예전 목록 주소(?cursor·?tag·?category)는 글 탭과 같은 화면으로 연다. */
    @GetMapping("/@{handle}")
    public Object blog(@PathVariable("handle") String handle,
                       @RequestParam(value = "cursor", required = false) String cursor,
                       @RequestParam(value = "tag", required = false) String tagRaw,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "category", required = false) String categoryRaw,
                       jakarta.servlet.http.HttpServletRequest request, Model model) {
        if (q != null || notEmpty(cursor) || notEmpty(tagRaw) || notEmpty(categoryRaw)) {
            return posts(handle, cursor, tagRaw, q, sort, categoryRaw, request, model);
        }
        BlogOwner owner = blogOwnerResolver.resolve(handle).orElseThrow(NotFoundException::new);
        if (!header.fill(owner, "home", request, model)) {
            return "blog/private";
        }
        Long friendViewer = header.friendViewer(owner);
        // 054 강성찬 개인 확장: 고정 글(공개 글일 때만)
        pins.pinnedPostId(owner.memberId()).map(id -> listQuery.cardsByIds(java.util.List.of(id), true).get(id)).ifPresent(card -> {
            model.addAttribute("pinnedCard", card);
            model.addAttribute("pinnedDate", com.team.blog.post.application.CardDates.label(card.firstPublicAt(), clock.instant()));
        });
        // 031 강성찬 개인 확장: 잔디·스트릭
        if (activityQuery.enabled()) {
            model.addAttribute("grass", activityQuery.grass(owner.memberId()));
        }
        // 033 강성찬 개인 확장: 짧은 기록 최근 3개
        if (noteService.enabled()) {
            java.util.List<com.team.blog.note.application.NoteService.Note> recent =
                    noteService.notes(owner.memberId(), currentUserProvider.current(), null, 3);
            java.util.Map<Long, String> noteDates = new java.util.HashMap<>();
            recent.forEach(n -> noteDates.put(n.id(), com.team.blog.post.application.CardDates.label(n.createdAt(), clock.instant())));
            model.addAttribute("recentNotes", recent);
            model.addAttribute("noteDates", noteDates);
        }
        // 100: 선반 — 최근 글 몇 개, 시리즈 몇 개(보는 사람이 볼 수 있는 것만, 비면 화면이 숨긴다)
        CardPage recent = blogPage(owner.memberId(), friendViewer, null, java.util.Optional.empty(), null);
        CardModel.fill(model, new CardPage(recent.items().stream().limit(HOME_SHELF).toList(), null), clock.instant());
        // 검색·미리보기: 소개글, 없으면 공개 글 수와 최근 글 제목(목록은 공개 글만)
        String bio = owner.bio() == null ? "" : owner.bio().strip();
        String recentTitles = com.team.blog.shared.web.ListMeta.titles(recent.items().stream()
                .map(com.team.blog.post.application.PostCard::title).toList(), 3);
        model.addAttribute("listMeta", new com.team.blog.shared.web.ListMeta(owner.displayName() + "의 블로그 · 글그린",
                !bio.isEmpty() ? bio : recentTitles.isEmpty() ? owner.displayName() + "님의 글그린 블로그예요"
                        : owner.displayName() + "님의 글그린 블로그. 최근 글: " + recentTitles, null));
        if (series.enabled()) {
            model.addAttribute("homeSeries", series.list(owner.memberId(), false).stream().limit(HOME_SHELF).toList());
        }
        return "blog/home";
    }

    /** 100 강성찬 개인 확장: 글 탭 — 전체 글(최신순 커서·인기순), 카테고리·태그 필터, 블로그 안 검색. */
    @GetMapping("/@{handle}/posts")
    public Object posts(@PathVariable("handle") String handle,
                        @RequestParam(value = "cursor", required = false) String cursor,
                        @RequestParam(value = "tag", required = false) String tagRaw,
                        @RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "sort", required = false) String sort,
                        @RequestParam(value = "category", required = false) String categoryRaw,
                        jakarta.servlet.http.HttpServletRequest request, Model model) {
        BlogOwner owner = blogOwnerResolver.resolve(handle).orElseThrow(NotFoundException::new);
        if (!header.fill(owner, "posts", request, model)) {
            return "blog/private";
        }
        // 020 FR-017: 블로그 안 검색
        if (q != null) {
            return searchController.blog(owner, q, sort, cursor, request, model);
        }
        String postsPath = com.team.blog.shared.web.BlogPaths.of(owner.handle()) + "/posts";
        // 013 FR-026: 블로그 안 태그 필터. 정리되지 않은 값은 정리된 주소로 301, 형식 밖이면 필터 없이
        String tag = null;
        if (tagRaw != null && !tagRaw.isEmpty()) {
            java.util.Optional<String> normalized = com.team.blog.tag.domain.TagNormalizer.lookupName(tagRaw);
            if (normalized.isPresent() && !normalized.get().equals(tagRaw)) {
                org.springframework.web.servlet.view.RedirectView redirect = new org.springframework.web.servlet.view.RedirectView(
                        postsPath + "?tag=" + org.springframework.web.util.UriUtils.encodeQueryParam(
                                normalized.get(), java.nio.charset.StandardCharsets.UTF_8));
                redirect.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
                return redirect;
            }
            tag = normalized.orElse(null);
        }
        java.util.Optional<Long> tagId = tag == null ? java.util.Optional.empty() : tagListingQuery.tagId(tag);
        // 100: 인기순은 필터 없을 때만
        boolean popular = "popular".equals(sort) && tag == null;
        Long friendViewer = header.friendViewer(owner);
        CardPage page;
        try {
            page = popular ? listQuery.blogPopular(owner.memberId(), friendViewer, cursor)
                    : blogPage(owner.memberId(), friendViewer, tag, tagId, cursor);
        } catch (PostContentException e) {
            page = popular ? listQuery.blogPopular(owner.memberId(), friendViewer, null)
                    : blogPage(owner.memberId(), friendViewer, tag, tagId, null);
            cursor = null;
        }
        // 037 강성찬 개인 확장: 카테고리 목록과 필터(공개 글만, 최신 N개)
        com.team.blog.category.application.CategoryService.Category filterCategory = null;
        if (categoryService.enabled()) {
            model.addAttribute("blogCategories", categoryService.list(owner.memberId()));
            if (categoryRaw != null && categoryRaw.matches("\\d{1,18}")) {
                filterCategory = categoryService.find(owner.memberId(), Long.parseLong(categoryRaw)).orElse(null);
            }
            if (filterCategory != null) {
                java.util.List<Long> ids = categoryService.postIds(filterCategory.id());
                java.util.Map<Long, com.team.blog.post.application.PostCard> byId = listQuery.cardsByIds(ids, true);
                page = new CardPage(ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList(), null);
                tag = null;
                popular = false;
            }
        }
        model.addAttribute("filterCategory", filterCategory);
        model.addAttribute("filterTag", tag);
        model.addAttribute("sort", popular ? "popular" : "latest");
        model.addAttribute("postsPath", postsPath);
        model.addAttribute("blogTags", tagListingQuery.blogTags(owner.memberId(), 50));
        model.addAttribute("continued", cursor != null && !cursor.isEmpty());
        model.addAttribute("linkBase", popular ? "?sort=popular&cursor=" : tag == null ? "?cursor="
                : "?tag=" + org.springframework.web.util.UriUtils.encodeQueryParam(tag, java.nio.charset.StandardCharsets.UTF_8) + "&cursor=");
        model.addAttribute("listApi", "/api/members/" + owner.handle() + "/posts" + (popular ? "?sort=popular"
                : tag == null ? "" : "?tag=" + org.springframework.web.util.UriUtils.encodeQueryParam(tag, java.nio.charset.StandardCharsets.UTF_8)));
        CardModel.fill(model, page, clock.instant());
        return "blog/posts";
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    private CardPage blogPage(long ownerId, Long friendViewer, String tag, java.util.Optional<Long> tagId, String cursor) {
        if (tag != null && tagId.isEmpty()) {
            return new CardPage(java.util.List.of(), null);
        }
        Long tid = tagId.orElse(null);
        if (friendViewer != null) {
            return listQuery.blogForFriend(ownerId, friendViewer, tid, cursor);
        }
        return tid == null ? listQuery.blog(ownerId, cursor) : listQuery.blogByTag(ownerId, tid, cursor);
    }
}
