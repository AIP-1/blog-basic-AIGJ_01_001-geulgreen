package com.team.blog.privacy.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.privacy.application.PrivateAccountService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/** 비공개 계정(073, 강성찬 개인 확장): 설정의 켜기·끄기와 받은 팔로우 요청(수락·거절). 본인만, 스크립트 없이 폼으로. */
@Controller
public class PrivacyPageController {

    private final PrivateAccountService privacy;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;
    private final BlogOwnerResolver ownerResolver;

    public PrivacyPageController(PrivateAccountService privacy, AccountGuard accountGuard, CurrentUserProvider currentUserProvider,
                                 BlogOwnerResolver ownerResolver) {
        this.privacy = privacy;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
        this.ownerResolver = ownerResolver;
    }

    @GetMapping("/settings/privacy")
    public String page(Model model) {
        long me = me();
        model.addAttribute("isPrivate", privacy.isPrivate(me));
        model.addAttribute("requests", privacy.requests(me));
        return "settings/privacy";
    }

    @PostMapping("/settings/privacy")
    public RedirectView set(@RequestParam("private") boolean on) {
        privacy.setPrivate(me(), on);
        return seeOther("/settings/privacy");
    }

    /** {@code action}: accept / decline. */
    @PostMapping("/settings/privacy/requests/{handle}")
    public RedirectView answer(@PathVariable("handle") String handle, @RequestParam("action") String action) {
        long me = me();
        long requester = ownerResolver.resolve(handle).orElseThrow(NotFoundException::new).memberId();
        switch (action) {
            case "accept" -> privacy.accept(me, requester);
            case "decline" -> privacy.cancel(requester, me);
            default -> throw new NotFoundException();
        }
        return seeOther("/settings/privacy");
    }

    private long me() {
        if (!privacy.enabled()) {
            throw new NotFoundException();
        }
        return accountGuard.requireLoggedIn(currentUserProvider.current()).memberId();
    }
}
