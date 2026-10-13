package com.team.blog.discovery.web;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.CurrentUser;
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
 */
@Controller
public class BlogPageController {

    private final BlogOwnerResolver blogOwnerResolver;
    private final PostListQuery listQuery;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final com.team.blog.post.application.TagListingQuery tagListingQuery;
    private final com.team.blog.interaction.application.FollowQuery followQuery;
    private final SearchController searchController;
    private final com.team.blog.friend.application.FriendQuery friendQuery;
    private final com.team.blog.discovery.application.ActivityQuery activityQuery;
    private final com.team.blog.note.application.NoteService noteService;
    private final com.team.blog.category.application.CategoryService categoryService;
    private final com.team.blog.discovery.application.BlogVisitService blogVisits;
    private final com.team.blog.discovery.application.PinService pins;
    private final com.team.blog.shared.web.ClientIpResolver ipResolver;
    private final com.team.blog.privacy.application.PrivateAccountService privacy;

    public BlogPageController(BlogOwnerResolver blogOwnerResolver, PostListQuery listQuery,
                              CurrentUserProvider currentUserProvider, Clock clock,
                              com.team.blog.post.application.TagListingQuery tagListingQuery,
                              com.team.blog.interaction.application.FollowQuery followQuery,
                              SearchController searchController,
                              com.team.blog.friend.application.FriendQuery friendQuery,
                              com.team.blog.discovery.application.ActivityQuery activityQuery,
                              com.team.blog.note.application.NoteService noteService,
                              com.team.blog.category.application.CategoryService categoryService,
                              com.team.blog.discovery.application.BlogVisitService blogVisits,
                              com.team.blog.discovery.application.PinService pins,
                              com.team.blog.shared.web.ClientIpResolver ipResolver,
                              com.team.blog.privacy.application.PrivateAccountService privacy) {
        this.privacy = privacy;
        this.blogVisits = blogVisits;
        this.pins = pins;
        this.ipResolver = ipResolver;
        this.noteService = noteService;
        this.categoryService = categoryService;
        this.activityQuery = activityQuery;
        this.friendQuery = friendQuery;
        this.followQuery = followQuery;
        this.searchController = searchController;
        this.blogOwnerResolver = blogOwnerResolver;
        this.listQuery = listQuery;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.tagListingQuery = tagListingQuery;
    }

    @GetMapping("/@{handle}")
    public Object blog(@PathVariable("handle") String handle,
                       @RequestParam(value = "cursor", required = false) String cursor,
                       @RequestParam(value = "tag", required = false) String tagRaw,
                       @RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "category", required = false) String categoryRaw,
                       jakarta.servlet.http.HttpServletRequest request, Model model) {
        BlogOwner owner = blogOwnerResolver.resolve(handle).orElseThrow(NotFoundException::new);
        // 073 강성찬 개인 확장: 수락받지 않은 사람에게 비공개 계정은 프로필 윗부분만
        if (!privacy.canSee(currentUserProvider.current(), owner.memberId())) {
            model.addAttribute("owner", owner);
            model.addAttribute("author", owner.toAuthorDisplay());
            model.addAttribute("followCounts", followQuery.counts(owner.memberId()));
            model.addAttribute("followRequested", currentUserProvider.current()
                    .map(v -> privacy.requested(v.memberId(), owner.memberId())).orElse(false));
            model.addAttribute("loginRedirect", "/login?redirect=" + com.team.blog.shared.web.BlogPaths.of(owner.handle()));
            return "blog/private";
        }
        // 020 FR-017: 블로그 안 검색
        if (q != null) {
            return searchController.blog(owner, q, sort, cursor, request, model);
        }
        // 013 FR-026: 블로그 안 태그 필터. 정리되지 않은 값은 정리된 주소로 301, 형식 밖이면 필터 없이
        String tag = null;
        if (tagRaw != null && !tagRaw.isEmpty()) {
            java.util.Optional<String> normalized = com.team.blog.tag.domain.TagNormalizer.lookupName(tagRaw);
            if (normalized.isPresent() && !normalized.get().equals(tagRaw)) {
                org.springframework.web.servlet.view.RedirectView redirect = new org.springframework.web.servlet.view.RedirectView(
                        com.team.blog.shared.web.BlogPaths.of(owner.handle()) + "?tag=" + org.springframework.web.util.UriUtils.encodeQueryParam(
                                normalized.get(), java.nio.charset.StandardCharsets.UTF_8));
                redirect.setStatusCode(org.springframework.http.HttpStatus.MOVED_PERMANENTLY);
                return redirect;
            }
            tag = normalized.orElse(null);
        }
        java.util.Optional<Long> tagId = tag == null ? java.util.Optional.empty() : tagListingQuery.tagId(tag);
        CardPage page;
        String requestedCursor = cursor;
        // 025·069 강성찬 개인 확장: 친구 버튼은 보는 사람이 이 주인을 친구로 넣을 수 있는지, 목록은 주인이 보는 사람을 골랐으면 친구 공개 글도
        com.team.blog.friend.application.FriendQuery.Status friendStatus = currentUserProvider.current()
                .map(v -> friendQuery.status(v.memberId(), owner.memberId()))
                .orElse(com.team.blog.friend.application.FriendQuery.Status.NONE);
        // 030: 그러면 그 사람이 든 그룹의 그룹 공개 글도
        Long friendViewer = currentUserProvider.current().map(CurrentUser::memberId)
                .filter(v -> friendQuery.hasFriend(owner.memberId(), v)).orElse(null);
        try {
            page = blogPage(owner.memberId(), friendViewer, tag, tagId, requestedCursor);
        } catch (PostContentException e) {
            page = blogPage(owner.memberId(), friendViewer, tag, tagId, null);
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
            }
        }
        model.addAttribute("filterCategory", filterCategory);
        // 051 강성찬 개인 확장: 방문자 수(세고 나서 보여 준다)
        if (blogVisits.enabled()) {
            blogVisits.recordVisit(owner.memberId(), currentUserProvider.current(),
                    com.team.blog.shared.web.VisitorCookie.read(request), ipResolver.resolve(request), request.getHeader("User-Agent"));
            model.addAttribute("blogVisits", blogVisits.counts(owner.memberId()));
        }
        // 054 강성찬 개인 확장: 고정 글(첫 화면, 필터 없을 때, 공개 글일 때만)
        if ((cursor == null || cursor.isEmpty()) && tag == null && filterCategory == null) {
            pins.pinnedPostId(owner.memberId()).map(id -> listQuery.cardsByIds(java.util.List.of(id), true).get(id)).ifPresent(card -> {
                model.addAttribute("pinnedCard", card);
                model.addAttribute("pinnedDate", com.team.blog.post.application.CardDates.label(card.firstPublicAt(), clock.instant()));
            });
        }
        model.addAttribute("filterTag", tag);
        model.addAttribute("blogTags", tagListingQuery.blogTags(owner.memberId(), 50));
        model.addAttribute("owner", owner);
        model.addAttribute("author", owner.toAuthorDisplay());
        model.addAttribute("publicCount", listQuery.publicCount(owner.memberId()));
        model.addAttribute("isOwner", currentUserProvider.current().map(CurrentUser::memberId)
                .map(id -> id == owner.memberId()).orElse(false));
        // 018: 팔로워·팔로잉 수와 보는 사람 기준 팔로우 상태
        model.addAttribute("followCounts", followQuery.counts(owner.memberId()));
        model.addAttribute("followingOwner", currentUserProvider.current().map(CurrentUser::memberId)
                .filter(id -> id != owner.memberId()).map(id -> followQuery.isFollowing(id, owner.memberId())).orElse(false));
        model.addAttribute("friendStatus", friendStatus.name());
        // 031 강성찬 개인 확장: 잔디·스트릭(첫 화면에서만)
        if (activityQuery.enabled() && (cursor == null || cursor.isEmpty()) && tag == null && filterCategory == null) {
            model.addAttribute("grass", activityQuery.grass(owner.memberId()));
        }
        // 033 강성찬 개인 확장: 짧은 기록 최근 3개(첫 화면에서만)
        if (noteService.enabled() && (cursor == null || cursor.isEmpty()) && tag == null && filterCategory == null) {
            java.util.List<com.team.blog.note.application.NoteService.Note> recent =
                    noteService.notes(owner.memberId(), currentUserProvider.current(), null, 3);
            java.util.Map<Long, String> noteDates = new java.util.HashMap<>();
            recent.forEach(n -> noteDates.put(n.id(), com.team.blog.post.application.CardDates.label(n.createdAt(), clock.instant())));
            model.addAttribute("recentNotes", recent);
            model.addAttribute("noteDates", noteDates);
        }
        model.addAttribute("loginRedirect", "/login?redirect=" + com.team.blog.shared.web.BlogPaths.of(owner.handle()));
        model.addAttribute("continued", cursor != null && !cursor.isEmpty());
        model.addAttribute("listApi", "/api/members/" + owner.handle() + "/posts"
                + (tag == null ? "" : "?tag=" + org.springframework.web.util.UriUtils.encodeQueryParam(tag, java.nio.charset.StandardCharsets.UTF_8)));
        CardModel.fill(model, page, clock.instant());
        return "blog/home";
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
