package com.team.blog.series.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.series.application.SeriesService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/** 041 시리즈 화면(강성찬 개인 확장): 블로그의 시리즈 목록·시리즈 글 목록, 글 화면에서 넣기·빼기(폼). */
@Controller
@ConditionalOnProperty(name = "blog.series.enabled", havingValue = "true", matchIfMissing = true)
public class SeriesController {

    private final SeriesService series;
    private final BlogOwnerResolver owners;
    private final CurrentUserProvider currentUserProvider;

    public SeriesController(SeriesService series, BlogOwnerResolver owners, CurrentUserProvider currentUserProvider) {
        this.series = series;
        this.owners = owners;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/@{handle}/series")
    public String list(@PathVariable("handle") String handle, Model model) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        boolean isOwner = currentUserProvider.current().map(CurrentUser::memberId).map(id -> id == owner.memberId()).orElse(false);
        model.addAttribute("owner", owner);
        model.addAttribute("seriesList", series.list(owner.memberId(), isOwner));
        return "series/list";
    }

    @GetMapping("/@{handle}/series/{id}")
    public String detail(@PathVariable("handle") String handle, @PathVariable("id") long id, Model model) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        SeriesService.Series s = series.find(owner.memberId(), id).orElseThrow(NotFoundException::new);
        model.addAttribute("owner", owner);
        model.addAttribute("series", s);
        model.addAttribute("items", series.items(id));
        return "series/detail";
    }

    @PostMapping("/posts/{postId}/series")
    public RedirectView add(@PathVariable("postId") long postId,
                            @RequestParam(value = "seriesId", required = false) String seriesId,
                            @RequestParam(value = "newTitle", required = false) String newTitle,
                            @RequestParam(value = "back", required = false) String back) {
        Long id = seriesId == null || seriesId.isBlank() || "new".equals(seriesId) ? null : Long.valueOf(seriesId);
        series.add(currentUserProvider.current(), postId, id, newTitle);
        return seeOther(safe(back));
    }

    @PostMapping("/posts/{postId}/series/remove")
    public RedirectView remove(@PathVariable("postId") long postId, @RequestParam(value = "back", required = false) String back) {
        series.remove(currentUserProvider.current(), postId);
        return seeOther(safe(back));
    }

    private static String safe(String back) {
        return back != null && back.startsWith("/") && !back.startsWith("//") && !back.contains("\\") ? back : "/";
    }
}
