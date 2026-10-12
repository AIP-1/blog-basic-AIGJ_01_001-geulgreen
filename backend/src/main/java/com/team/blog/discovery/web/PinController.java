package com.team.blog.discovery.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.discovery.application.PinService;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/** 054 글 고정·해제(강성찬 개인 확장, 폼). */
@Controller
public class PinController {

    private final PinService pins;
    private final CurrentUserProvider currentUserProvider;

    public PinController(PinService pins, CurrentUserProvider currentUserProvider) {
        this.pins = pins;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/posts/{postId}/pin")
    public RedirectView pin(@PathVariable("postId") long postId, @RequestParam(value = "back", required = false) String back) {
        pins.pin(currentUserProvider.current(), postId);
        return seeOther(safe(back));
    }

    @PostMapping("/posts/{postId}/unpin")
    public RedirectView unpin(@PathVariable("postId") long postId, @RequestParam(value = "back", required = false) String back) {
        pins.unpin(currentUserProvider.current());
        return seeOther(safe(back));
    }

    private static String safe(String back) {
        return back != null && back.startsWith("/") && !back.startsWith("//") && !back.contains("\\") ? back : "/";
    }
}
