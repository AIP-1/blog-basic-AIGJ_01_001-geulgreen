package com.team.blog.series.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.series.application.SeriesService;
import com.team.blog.shared.error.PostContentException;
import com.team.blog.shared.security.CurrentUserProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 106 강성찬 개인 확장: 내 글 관리의 [시리즈] 탭. 새 시리즈 만들기, 글 여러 개 한 번에 넣기·빼기, 편 순서 바꾸기.
 * 공개 시리즈 화면에는 읽기 정렬만 남고 순서 바꾸기는 여기로 옮겼다.
 */
@Controller
@ConditionalOnProperty(name = "blog.series.enabled", havingValue = "true", matchIfMissing = true)
public class SeriesManageController {

    private static final String PAGE = "/manage/series";
    private static final Set<String> ERRORS = Set.of("SERIES_TITLE_INVALID", "SERIES_POSTS_REQUIRED");

    private final SeriesService series;
    private final CurrentUserProvider currentUserProvider;

    public SeriesManageController(SeriesService series, CurrentUserProvider currentUserProvider) {
        this.series = series;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping(PAGE)
    public String page(@RequestParam(value = "error", required = false) String error, Model model) {
        List<SeriesService.MyPost> posts = series.myPosts(currentUserProvider.current()); // 로그인 확인도 여기서
        long me = currentUserProvider.current().orElseThrow().memberId();
        Map<SeriesService.Series, List<SeriesService.MyPost>> items = new LinkedHashMap<>();
        series.list(me, true).forEach(s -> items.put(s, series.ownerItems(s.id())));
        model.addAttribute("seriesItems", items);
        model.addAttribute("myBlog", com.team.blog.shared.web.BlogPaths.of(series.handle(me)));
        model.addAttribute("freePosts", posts.stream().filter(p -> p.seriesId() == null).toList());
        model.addAttribute("myPosts", posts);
        model.addAttribute("error", error != null && ERRORS.contains(error) ? error : null);
        return "series/manage";
    }

    @PostMapping(PAGE)
    public RedirectView create(@RequestParam("newTitle") String newTitle,
                               @RequestParam(value = "postIds", required = false) List<Long> postIds) {
        return addAll(postIds, null, newTitle);
    }

    @PostMapping(PAGE + "/{id}/add")
    public RedirectView add(@PathVariable("id") long id, @RequestParam(value = "postIds", required = false) List<Long> postIds) {
        return addAll(postIds, id, null);
    }

    @PostMapping(PAGE + "/{id}/move")
    public RedirectView move(@PathVariable("id") long id, @RequestParam("postId") long postId, @RequestParam("dir") String dir) {
        series.move(currentUserProvider.current(), id, postId, "up".equals(dir));
        return seeOther(PAGE + "#series-" + id);
    }

    private RedirectView addAll(List<Long> postIds, Long seriesId, String newTitle) {
        try {
            long id = series.addAll(currentUserProvider.current(), postIds, seriesId, newTitle);
            return seeOther(PAGE + "#series-" + id);
        } catch (PostContentException e) {
            return seeOther(PAGE + "?error=" + e.getCode());
        }
    }
}
