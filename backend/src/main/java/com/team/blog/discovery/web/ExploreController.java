package com.team.blog.discovery.web;

import com.team.blog.discovery.application.NewcomerQuery;
import com.team.blog.discovery.application.RecommendedBlogQuery;
import com.team.blog.discovery.application.RecommendedPostQuery;
import com.team.blog.post.application.TagListingQuery;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.tag.application.icon.TagIconService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 079 강성찬 개인 확장: 탐색({@code GET /explore}). 홈 오른쪽 칸(휴대폰에서는 끝없는 목록 아래라 닿지 못함)의
 * 인기 태그·추천 글·추천 블로그와 새로 온 작가를 한 화면에 모은다. 비회원도 본다(팔로우 자리는 로그인 링크).
 */
@Controller
public class ExploreController {

    private final TagListingQuery tagListingQuery;
    private final TagIconService tagIcons;
    private final RecommendedPostQuery recommendedPostQuery;
    private final RecommendedBlogQuery recommendedBlogQuery;
    private final NewcomerQuery newcomerQuery;
    private final CurrentUserProvider currentUserProvider;

    public ExploreController(TagListingQuery tagListingQuery, TagIconService tagIcons, RecommendedPostQuery recommendedPostQuery,
                             RecommendedBlogQuery recommendedBlogQuery, NewcomerQuery newcomerQuery,
                             CurrentUserProvider currentUserProvider) {
        this.tagListingQuery = tagListingQuery;
        this.tagIcons = tagIcons;
        this.recommendedPostQuery = recommendedPostQuery;
        this.recommendedBlogQuery = recommendedBlogQuery;
        this.newcomerQuery = newcomerQuery;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/explore")
    public String explore(Model model) {
        List<TagListingQuery.TagCount> popular = tagListingQuery.top(12);
        model.addAttribute("popularTags", popular);
        model.addAttribute("tagStyles", tagIcons.styles(popular.stream().map(TagListingQuery.TagCount::name).toList()));
        long viewer = currentUserProvider.current().map(CurrentUser::memberId).orElse(-1L);
        model.addAttribute("recommendedPosts", recommendedPostQuery.forMember(viewer));
        model.addAttribute("recommended", recommendedBlogQuery.forMember(viewer));
        model.addAttribute("newcomers", newcomerQuery.enabled() ? newcomerQuery.recent() : List.of());
        return "explore";
    }
}
