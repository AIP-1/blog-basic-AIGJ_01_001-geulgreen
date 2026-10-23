package com.team.blog.discovery.web;

import com.team.blog.interaction.application.FollowQuery;
import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.Clock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 팔로잉 피드(018 FR-011~FR-016, 24 §2-3): 로그인 필요, 홈과 같은 카드·정렬·커서. 홈은 바꾸지 않는다.
 * 086(강성찬 개인 확장): 팔로우한 사람 글만. 추천 블로그(068)·섞어 넣던 추천 글(081)은 탐색으로 옮겼다.
 * 10-23(강성찬 개인 확장, chan "홈·탐색·피드가 너무 똑같아"): 카드 칸 대신 "친구 소식" 한 줄 목록. 위에 팔로우한 사람 얼굴 줄
 * (24시간 안에 새 글이 있으면 테두리), 글은 오늘·어제·이번 주·지난 글로 나눈다.
 */
@Controller
public class FeedController {

    private final PostListQuery listQuery;
    private final FollowQuery followQuery;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public FeedController(PostListQuery listQuery, FollowQuery followQuery, AccountGuard accountGuard,
                          CurrentUserProvider currentUserProvider, Clock clock) {
        this.listQuery = listQuery;
        this.followQuery = followQuery;
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
        java.time.Instant now = clock.instant();
        CardModel.fill(model, page, now);
        java.util.Map<Long, String> days = new java.util.HashMap<>();
        java.util.Set<String> fresh = new java.util.HashSet<>();
        for (com.team.blog.post.application.PostCard card : page.items()) {
            days.put(card.id(), com.team.blog.post.application.CardDates.day(card.firstPublicAt(), now));
            if (card.firstPublicAt() != null && card.firstPublicAt().isAfter(now.minus(java.time.Duration.ofHours(24)))) {
                fresh.add(card.author().handle());
            }
        }
        model.addAttribute("cardDays", days);
        boolean continued = cursor != null && !cursor.isEmpty();
        model.addAttribute("feedPeople", continued ? java.util.List.of() : people(user.memberId(), fresh));
        model.addAttribute("continued", continued);
        long following = followQuery.counts(user.memberId()).following();
        model.addAttribute("followingCount", following);
        model.addAttribute("followsAnyone", !page.items().isEmpty() || continued || following > 0);
        model.addAttribute("listApi", "/api/feed");
        return "feed";
    }

    /** 얼굴 줄 한 사람. {@code fresh}면 24시간 안에 새 글이 있다. */
    public record FeedPerson(String handle, com.team.blog.account.application.ProfileAvatar avatar, boolean fresh) {
    }

    /** 팔로우한 사람(최근 팔로우 순 첫 묶음) 중 새 글이 있는 사람을 앞으로. */
    private java.util.List<FeedPerson> people(long me, java.util.Set<String> fresh) {
        return followQuery.page(me, false, me, null).items().stream()
                .map(m -> new FeedPerson(m.handle(), m.avatar(), fresh.contains(m.handle())))
                .sorted(java.util.Comparator.comparing(p -> !p.fresh()))
                .toList();
    }

    @GetMapping("/api/feed")
    @ResponseBody
    public CardPage api(@RequestParam(value = "cursor", required = false) String cursor) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        return listQuery.followingFeed(user.memberId(), cursor);
    }
}
