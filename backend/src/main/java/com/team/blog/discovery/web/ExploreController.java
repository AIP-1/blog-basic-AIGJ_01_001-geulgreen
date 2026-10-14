package com.team.blog.discovery.web;

import com.team.blog.discovery.application.RecommendedBlogQuery;
import com.team.blog.discovery.application.RecommendedPostQuery;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.FeedCursor;
import com.team.blog.post.application.PostCard;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.post.application.TagListingQuery;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.tag.application.icon.TagIconService;
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
 */
@Controller
public class ExploreController {

    private final TagListingQuery tagListingQuery;
    private final TagIconService tagIcons;
    private final RecommendedPostQuery recommendedPostQuery;
    private final RecommendedBlogQuery recommendedBlogQuery;
    private final PostListQuery listQuery;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ExploreController(TagListingQuery tagListingQuery, TagIconService tagIcons, RecommendedPostQuery recommendedPostQuery,
                             RecommendedBlogQuery recommendedBlogQuery, PostListQuery listQuery,
                             CurrentUserProvider currentUserProvider, Clock clock) {
        this.tagListingQuery = tagListingQuery;
        this.tagIcons = tagIcons;
        this.recommendedPostQuery = recommendedPostQuery;
        this.recommendedBlogQuery = recommendedBlogQuery;
        this.listQuery = listQuery;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @GetMapping("/explore")
    public String explore(@RequestParam(value = "cursor", required = false) String cursor, Model model) {
        long viewer = viewer();
        CardPage page;
        try {
            page = page(viewer, cursor);
        } catch (PostContentException e) {
            page = page(viewer, null);
            cursor = null;
        }
        boolean continued = cursor != null && !cursor.isEmpty();
        CardModel.fill(model, page, clock.instant());
        model.addAttribute("continued", continued);
        model.addAttribute("listApi", "/api/explore");
        List<TagListingQuery.TagCount> popular = continued ? List.of() : tagListingQuery.top(12);
        model.addAttribute("popularTags", popular);
        model.addAttribute("tagStyles", tagIcons.styles(popular.stream().map(TagListingQuery.TagCount::name).toList()));
        model.addAttribute("recommended", continued ? List.of() : recommendedBlogQuery.forMember(viewer));
        return "explore";
    }

    @GetMapping("/api/explore")
    @ResponseBody
    public CardPage api(@RequestParam(value = "cursor", required = false) String cursor) {
        return page(viewer(), cursor);
    }

    private long viewer() {
        return currentUserProvider.current().map(CurrentUser::memberId).orElse(-1L);
    }

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
        List<Long> ids = recommendedPostQuery.pool(viewer);
        int end = Math.min(offset + PostListQuery.PAGE_SIZE, ids.size());
        List<Long> slice = offset >= end ? List.of() : ids.subList(offset, end);
        Map<Long, PostCard> cards = listQuery.cardsByIds(slice);
        return new CardPage(slice.stream().map(cards::get).filter(Objects::nonNull).toList(),
                end < ids.size() ? String.valueOf(end) : null);
    }
}
