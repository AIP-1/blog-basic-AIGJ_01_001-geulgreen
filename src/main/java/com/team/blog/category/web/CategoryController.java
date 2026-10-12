package com.team.blog.category.web;

import com.team.blog.category.application.CategoryService;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.security.AccountGuard;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import static com.team.blog.shared.web.Redirects.seeOther;

/** 037 카테고리 관리(설정)와 글에 달기(강성찬 개인 확장). 스크립트 없이 폼. */
@Controller
@ConditionalOnProperty(name = "blog.categories.enabled", havingValue = "true", matchIfMissing = true)
public class CategoryController {

    private final CategoryService categories;
    private final CurrentUserProvider currentUserProvider;
    private final AccountGuard accountGuard;

    public CategoryController(CategoryService categories, CurrentUserProvider currentUserProvider, AccountGuard accountGuard) {
        this.categories = categories;
        this.currentUserProvider = currentUserProvider;
        this.accountGuard = accountGuard;
    }

    @GetMapping("/settings/categories")
    public String page(Model model) {
        CurrentUser me = accountGuard.requireLoggedIn(currentUserProvider.current());
        model.addAttribute("categories", categories.list(me.memberId()));
        return "settings/categories";
    }

    @PostMapping("/settings/categories")
    public RedirectView create(@RequestParam("name") String name) {
        categories.create(currentUserProvider.current(), name);
        return back("/settings/categories");
    }

    @PostMapping("/settings/categories/{id}/rename")
    public RedirectView rename(@PathVariable("id") long id, @RequestParam("name") String name) {
        categories.rename(currentUserProvider.current(), id, name);
        return back("/settings/categories");
    }

    @PostMapping("/settings/categories/{id}/delete")
    public RedirectView delete(@PathVariable("id") long id) {
        categories.delete(currentUserProvider.current(), id);
        return back("/settings/categories");
    }

    @PostMapping("/settings/categories/{id}/move")
    public RedirectView move(@PathVariable("id") long id, @RequestParam("dir") String dir) {
        categories.move(currentUserProvider.current(), id, "up".equals(dir));
        return back("/settings/categories");
    }

    @PostMapping("/posts/{postId}/category")
    public RedirectView assign(@PathVariable("postId") long postId,
                               @RequestParam(value = "categoryId", required = false) String categoryId,
                               @RequestParam(value = "back", required = false) String back) {
        Long id = categoryId == null || categoryId.isBlank() ? null : Long.valueOf(categoryId);
        categories.assign(currentUserProvider.current(), postId, id);
        return back(back != null && back.startsWith("/") && !back.startsWith("//") && !back.contains("\\") ? back : "/");
    }

    private static RedirectView back(String path) {
        return seeOther(path);
    }
}
