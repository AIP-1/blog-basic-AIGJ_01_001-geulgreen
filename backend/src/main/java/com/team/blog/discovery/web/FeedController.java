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
        for (com.team.blog.post.application.PostCard card : page.items()) {
            days.put(card.id(), com.team.blog.post.application.CardDates.day(card.firstPublicAt(), now));
        }
        model.addAttribute("cardDays", days);
        boolean continued = cursor != null && !cursor.isEmpty();
        model.addAttribute("feedPeople", continued ? java.util.List.of() : people(user.memberId(), now));
        model.addAttribute("continued", continued);
        long following = followQuery.counts(user.memberId()).following();
        model.addAttribute("followingCount", following);
        model.addAttribute("myHandle", following > 0 ? followQuery.handleOf(user.memberId()) : null);
        model.addAttribute("followsAnyone", !page.items().isEmpty() || continued || following > 0);
        model.addAttribute("listApi", "/api/feed");
        return "feed";
    }

    /** 얼굴 줄 한 사람. {@code fresh}면 24시간 안에 새 글이 있다. */
    public record FeedPerson(String handle, com.team.blog.account.application.ProfileAvatar avatar, boolean fresh) {
    }

    /** 얼굴 줄에 보이는 최대 인원. 나머지는 [전체 N명]으로 팔로잉 목록에. */
    static final int PEOPLE_LIMIT = 20;

    /**
     * 10-26(chan "팔로우가 1000명이 넘어가면"): 최근 팔로우 순 20명이 아니라 마지막 글이 최근인 순 20명.
     * 팔로우가 많아도 오늘 글 쓴 사람이 줄에서 빠지지 않는다. 24시간 안에 글이 있으면 테두리.
     */
    private java.util.List<FeedPerson> people(long me, java.time.Instant now) {
        java.time.Instant since = now.minus(java.time.Duration.ofHours(24));
        return followQuery.recentWriters(me, PEOPLE_LIMIT).stream()
                .map(w -> new FeedPerson(w.handle(), w.avatar(), w.lastPostAt() != null && w.lastPostAt().isAfter(since)))
                .toList();
    }

    @GetMapping("/api/feed")
    @ResponseBody
    public CardPage api(@RequestParam(value = "cursor", required = false) String cursor) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        return listQuery.followingFeed(user.memberId(), cursor);
    }
}
