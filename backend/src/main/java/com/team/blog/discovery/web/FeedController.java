package com.team.blog.discovery.web;

import com.team.blog.discovery.application.RecommendedBlogQuery;
import com.team.blog.discovery.application.RecommendedPostQuery;
import com.team.blog.interaction.application.FollowQuery;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.FeedCursor;
import com.team.blog.post.application.PostCard;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 팔로잉 피드(018 FR-011~FR-016, 24 §2-3): 로그인 필요, 홈과 같은 카드·정렬·9개·커서. 홈은 바꾸지 않는다.
 * 068(강성찬 개인 확장): 첫 화면 위에 "추천 블로그"(아직 팔로우하지 않은 작가)를 따로 보여 준다.
 * 081(강성찬 개인 확장): 팔로우한 글 {@value #EVERY}개마다 추천 글(077 규칙) 1개를 끼우고, 팔로우한 글이 끝나면
 * "팔로우한 글을 모두 봤어요" 뒤로 추천 글이 이어진다. 팔로우가 없으면 처음부터 추천 글. 커서는 {@code 추천위치.팔로우커서}(끝나면 {@code -}).
 */
@Controller
public class FeedController {

    private final PostListQuery listQuery;
    private final FollowQuery followQuery;
    private final RecommendedBlogQuery recommendedBlogQuery;
    private final RecommendedPostQuery recommendedPostQuery;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public FeedController(PostListQuery listQuery, FollowQuery followQuery,
                          RecommendedBlogQuery recommendedBlogQuery, RecommendedPostQuery recommendedPostQuery, AccountGuard accountGuard,
                          CurrentUserProvider currentUserProvider, Clock clock) {
        this.listQuery = listQuery;
        this.followQuery = followQuery;
        this.recommendedBlogQuery = recommendedBlogQuery;
        this.recommendedPostQuery = recommendedPostQuery;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(value = "cursor", required = false) String cursor, Model model) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        FeedPage page;
        try {
            page = mixed(user.memberId(), cursor);
        } catch (PostContentException e) {
            page = mixed(user.memberId(), null);
            cursor = null;
        }
        CardModel.fill(model, new CardPage(page.items(), page.nextCursor()), clock.instant());
        model.addAttribute("reasons", page.reasons());
        model.addAttribute("dividerBefore", page.dividerBefore());
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
    public FeedPage api(@RequestParam(value = "cursor", required = false) String cursor) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        return mixed(user.memberId(), cursor);
    }

    /** {@code reasons}: 추천 글 번호 → 이유. {@code dividerBefore}: 이 글 앞에 "팔로우한 글을 모두 봤어요"(없으면 null). */
    public record FeedPage(List<PostCard> items, String nextCursor, Map<Long, String> reasons, Long dividerBefore) {
    }

    static final int EVERY = 4;

    private FeedPage mixed(long me, String cursor) {
        int offset = 0;
        String follow = null;
        boolean followDone = false;
        if (cursor != null && !cursor.isEmpty()) {
            int dot = cursor.indexOf('.');
            try {
                offset = Integer.parseInt(cursor.substring(0, dot));
            } catch (RuntimeException e) {
                throw new PostContentException(FeedCursor.INVALID);
            }
            if (offset < 0) {
                throw new PostContentException(FeedCursor.INVALID);
            }
            follow = cursor.substring(dot + 1);
            followDone = follow.equals("-");
        }
        List<RecommendedPostQuery.Pick> picks = recommendedPostQuery.forFeed(me);
        List<RecommendedPostQuery.Pick> window = picks.subList(Math.min(offset, picks.size()),
                Math.min(offset + 2 * com.team.blog.post.application.PostListQuery.PAGE_SIZE, picks.size()));
        Map<Long, PostCard> recCards = listQuery.cardsByIds(window.stream().map(RecommendedPostQuery.Pick::postId).toList());
        List<PostCard> items = new ArrayList<>();
        Map<Long, String> reasons = new HashMap<>();
        Long dividerBefore = null;
        String nextFollow = null;
        int[] next = {offset};
        boolean endedHere = false;
        if (!followDone) {
            CardPage page = listQuery.followingFeed(me, follow);
            for (int i = 0; i < page.items().size(); i++) {
                items.add(page.items().get(i));
                if ((i + 1) % EVERY == 0) {
                    addPick(picks, next, recCards, items, reasons);
                }
            }
            nextFollow = page.nextCursor();
            followDone = nextFollow == null;
            endedHere = followDone && (!page.items().isEmpty() || (follow != null && !follow.isEmpty()));
        }
        if (followDone) {
            int before = items.size();
            while (items.size() < com.team.blog.post.application.PostListQuery.PAGE_SIZE && addPick(picks, next, recCards, items, reasons)) {
                // 채울 때까지
            }
            if (endedHere && items.size() > before) {
                dividerBefore = items.get(before).id();
            }
        }
        String nextCursor = !followDone ? next[0] + "." + nextFollow : next[0] < picks.size() ? next[0] + ".-" : null;
        return new FeedPage(items, nextCursor, reasons, dividerBefore);
    }

    /** 다음 추천 1개(카드를 못 만든 글은 건너뜀). 남은 추천이 없으면 false. */
    private boolean addPick(List<RecommendedPostQuery.Pick> picks, int[] next, Map<Long, PostCard> recCards,
                            List<PostCard> items, Map<Long, String> reasons) {
        while (next[0] < picks.size()) {
            RecommendedPostQuery.Pick pick = picks.get(next[0]++);
            PostCard card = recCards.get(pick.postId());
            if (card != null) {
                items.add(card);
                reasons.put(card.id(), pick.sameTag() ? "관심 태그와 같은 글" : "많이 읽는 글");
                return true;
            }
        }
        return false;
    }
}
