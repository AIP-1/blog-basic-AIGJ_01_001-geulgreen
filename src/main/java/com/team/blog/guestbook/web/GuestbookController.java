package com.team.blog.guestbook.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.guestbook.application.GuestbookService;
import com.team.blog.post.application.CardDates;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/** 040 방명록 화면(강성찬 개인 확장): 스크립트 없이 폼. */
@Controller
@ConditionalOnProperty(name = "blog.guestbook.enabled", havingValue = "true", matchIfMissing = true)
public class GuestbookController {

    private final GuestbookService guestbook;
    private final BlogOwnerResolver owners;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public GuestbookController(GuestbookService guestbook, BlogOwnerResolver owners, CurrentUserProvider currentUserProvider,
                               Clock clock) {
        this.guestbook = guestbook;
        this.owners = owners;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @GetMapping("/@{handle}/guestbook")
    public String list(@PathVariable("handle") String handle, @RequestParam(value = "before", required = false) Long before,
                       Model model) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        Optional<CurrentUser> viewer = currentUserProvider.current();
        int size = guestbook.pageSize();
        List<GuestbookService.Entry> page = guestbook.entries(owner.memberId(), viewer, before, size + 1);
        List<GuestbookService.Entry> items = page.subList(0, Math.min(size, page.size()));
        Map<Long, String> dates = new HashMap<>();
        items.forEach(e -> dates.put(e.id(), CardDates.label(e.createdAt(), clock.instant())));
        model.addAttribute("owner", owner);
        model.addAttribute("entries", items);
        model.addAttribute("entryDates", dates);
        model.addAttribute("nextBefore", page.size() > size ? items.get(items.size() - 1).id() : null);
        model.addAttribute("loginRedirect", "/login?redirect=/@" + owner.handle() + "/guestbook");
        return "guestbook/list";
    }

    @PostMapping("/@{handle}/guestbook")
    public RedirectView write(@PathVariable("handle") String handle, @RequestParam("content") String content,
                              @RequestParam(value = "secret", required = false) String secret) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        guestbook.write(currentUserProvider.current(), owner.memberId(), content, secret != null);
        return seeOther("/@" + owner.handle() + "/guestbook");
    }

    @PostMapping("/@{handle}/guestbook/{id}/delete")
    public RedirectView delete(@PathVariable("handle") String handle, @PathVariable("id") long id) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        guestbook.delete(currentUserProvider.current(), id);
        return seeOther("/@" + owner.handle() + "/guestbook");
    }
}
