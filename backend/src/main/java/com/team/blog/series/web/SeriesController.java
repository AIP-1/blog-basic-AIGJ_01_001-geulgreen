package com.team.blog.series.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.account.application.BlogOwner;
import com.team.blog.account.application.BlogOwnerResolver;
import com.team.blog.discovery.web.BlogHeaderModel;
import com.team.blog.post.application.CardDates;
import com.team.blog.post.application.PostCard;
import com.team.blog.series.application.SeriesService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Clock clock;
    private final BlogHeaderModel header;

    public SeriesController(SeriesService series, BlogOwnerResolver owners, CurrentUserProvider currentUserProvider, Clock clock,
                            BlogHeaderModel header) {
        this.header = header;
        this.series = series;
        this.owners = owners;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    /** 100 강성찬 개인 확장: 블로그의 시리즈 탭(머리말·탭은 홈과 같이, 비공개 계정은 프로필 윗부분만). */
    @GetMapping("/@{handle}/series")
    public String list(@PathVariable("handle") String handle, HttpServletRequest request, Model model) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        if (!header.fill(owner, "series", request, model)) {
            return "blog/private";
        }
        model.addAttribute("seriesList", series.list(owner.memberId(), Boolean.TRUE.equals(model.getAttribute("isOwner"))));
        return "series/list";
    }

    @GetMapping("/@{handle}/series/{id}")
    public String detail(@PathVariable("handle") String handle, @PathVariable("id") long id,
                         @RequestParam(value = "order", required = false) String order, HttpServletRequest request,
                         Model model) {
        BlogOwner owner = owners.resolve(handle).orElseThrow(NotFoundException::new);
        if (!header.fill(owner, "series", request, model)) {
            return "blog/private";
        }
        SeriesService.Series s = series.find(owner.memberId(), id).orElseThrow(NotFoundException::new);
        model.addAttribute("series", s);
        // 067 후속: 홈과 같은 카드
        List<PostCard> cards = series.cards(id);
        // 105 강성찬 개인 확장: 1편부터(기본)·최신 글부터. 편 번호는 그대로(화면이 total로 거꾸로 센다).
        // 106: 순서 바꾸기는 내 글 관리 [시리즈] 탭으로 옮겼다
        boolean latest = "latest".equals(order);
        model.addAttribute("order", latest ? "latest" : "first");
        model.addAttribute("total", cards.size());
        if (latest) {
            cards = cards.reversed();
        }
        Instant now = clock.instant();
        Map<Long, String> dates = new HashMap<>();
        cards.forEach(c -> dates.put(c.id(), CardDates.label(c.firstPublicAt(), now)));
        model.addAttribute("cards", cards);
        model.addAttribute("cardDates", dates);
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
