package com.team.blog.discovery.web;

import com.team.blog.post.application.CardPage;
import com.team.blog.post.application.PostAccessPolicy;
import com.team.blog.post.application.PostListQuery;
import com.team.blog.post.application.PostListQuery.Collected;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 10-23 chan 모아보기: 내가 좋아요한 글·저장한 글(탭 두 개). 로그인 필요, 나만 본다. 카드·[더 보기]는 피드와 같다.
 * 들어가는 길은 오른쪽 위 프로필 메뉴(위쪽 메뉴는 그대로). 주소는 /me/…(예약어 me, 맨 앞 경로를 새로 늘리지 않는다).
 */
@Controller
public class CollectionController {

    private final PostListQuery listQuery;
    private final PostAccessPolicy accessPolicy;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public CollectionController(PostListQuery listQuery, PostAccessPolicy accessPolicy, AccountGuard accountGuard,
                                CurrentUserProvider currentUserProvider, Clock clock) {
        this.listQuery = listQuery;
        this.accessPolicy = accessPolicy;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @GetMapping("/me/{tab:liked|saved}")
    public String page(@PathVariable("tab") String tab, @RequestParam(value = "cursor", required = false) String cursor,
                       Model model) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        Collected kind = kind(tab);
        CardPage page;
        try {
            page = list(kind, user, cursor);
        } catch (PostContentException e) {
            page = list(kind, user, null);
            cursor = null;
        }
        CardModel.fill(model, page, clock.instant());
        model.addAttribute("tab", tab);
        model.addAttribute("continued", cursor != null && !cursor.isEmpty());
        model.addAttribute("listApi", "/api/me/" + tab);
        return "collection";
    }

    @GetMapping("/api/me/{tab:liked|saved}")
    @ResponseBody
    public CardPage api(@PathVariable("tab") String tab, @RequestParam(value = "cursor", required = false) String cursor) {
        CurrentUser user = accountGuard.requireLoggedIn(currentUserProvider.current());
        return list(kind(tab), user, cursor);
    }

    private CardPage list(Collected kind, CurrentUser user, String cursor) {
        Optional<CurrentUser> viewer = Optional.of(user);
        return listQuery.collected(kind, user.memberId(), cursor, authorId -> accessPolicy.canSeeAuthor(viewer, authorId));
    }

    private static Collected kind(String tab) {
        return "saved".equals(tab) ? Collected.SAVED : Collected.LIKED;
    }
}
