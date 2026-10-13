package com.team.blog.discovery.web;

import com.team.blog.discovery.application.RecommendedBlogQuery;
import com.team.blog.interaction.application.FollowQuery;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 팔로잉 피드(018 FR-011~FR-016, 24 §2-3): 로그인 필요, 홈과 같은 카드·정렬·9개·커서. 홈은 바꾸지 않는다.
 * 068(강성찬 개인 확장): 첫 화면 위에 "추천 블로그"(아직 팔로우하지 않은 작가)를 따로 보여 준다.
 */
@Controller
public class FeedController {

    private final PostListQuery listQuery;
    private final FollowQuery followQuery;
    private final RecommendedBlogQuery recommendedBlogQuery;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public FeedController(PostListQuery listQuery, FollowQuery followQuery,
                          RecommendedBlogQuery recommendedBlogQuery, AccountGuard accountGuard,
                          CurrentUserProvider currentUserProvider, Clock clock) {
        this.listQuery = listQuery;
        this.followQuery = followQuery;
        this.recommendedBlogQuery = recommendedBlogQuery;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(value = "cursor", required = false) String cursor, Model model) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        CardPage page;
        try {
            page = listQuery.followingFeed(user.memberId(), cursor);
        } catch (PostContentException e) {
            page = listQuery.followingFeed(user.memberId(), null);
            cursor = null;
        }
        CardModel.fill(model, page, clock.instant());
        boolean continued = cursor != null && !cursor.isEmpty();
        model.addAttribute("continued", continued);
        model.addAttribute("followsAnyone", !page.items().isEmpty() || continued
                || followQuery.counts(user.memberId()).following() > 0);
        model.addAttribute("listApi", "/api/feed");
        model.addAttribute("recommended", continued ? List.of() : recommendedBlogQuery.forMember(user.memberId()));
        return "feed";
    }

    @GetMapping("/api/feed")
    @ResponseBody
    public CardPage api(@RequestParam(value = "cursor", required = false) String cursor) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        return listQuery.followingFeed(user.memberId(), cursor);
    }
}
