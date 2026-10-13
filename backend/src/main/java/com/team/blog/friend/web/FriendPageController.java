package com.team.blog.friend.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.friend.application.FriendQuery;
import com.team.blog.friend.application.FriendService;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 친한 친구(025·069, 강성찬 개인 확장): 블로그 머리·설정 화면의 폼(스크립트 없이 동작)과 설정의 친구 화면.
 * 친구 목록과 내가 팔로우하는 사람 목록은 본인만 본다.
 */
@Controller
public class FriendPageController {

    private final FriendService friendService;
    private final FriendQuery friendQuery;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;

    public FriendPageController(FriendService friendService, FriendQuery friendQuery, AccountGuard accountGuard,
                                CurrentUserProvider currentUserProvider) {
        this.friendService = friendService;
        this.friendQuery = friendQuery;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
    }

    /** {@code action}: add(내가 팔로우하는 사람을 친구로 넣기) / remove(친구에서 빼기). */
    @PostMapping("/@{handle}/friend")
    public RedirectView form(@PathVariable("handle") String handle, @RequestParam("action") String action,
                             @RequestParam(value = "back", required = false) String back) {
        switch (action) {
            case "add" -> friendService.add(currentUserProvider.current(), handle);
            case "remove" -> friendService.remove(currentUserProvider.current(), handle);
            default -> throw new com.team.blog.shared.error.NotFoundException();
        }
        String target = back != null && back.startsWith("/") && !back.startsWith("//") && !back.contains("\\")
                ? back : "/" + handle;
        return seeOther(target);
    }

    @GetMapping("/settings/friends")
    public String friends(@RequestParam(value = "q", required = false) String q, Model model) {
        CurrentUser me = accountGuard.requireLoggedIn(currentUserProvider.current());
        model.addAttribute("friends", friendQuery.friends(me.memberId()));
        model.addAttribute("candidates", friendQuery.candidates(me.memberId(), q));
        model.addAttribute("q", q == null ? "" : q.strip());
        return "settings/friends";
    }
}
