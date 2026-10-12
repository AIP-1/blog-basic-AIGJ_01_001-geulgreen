package com.team.blog.category.web;

import com.team.blog.category.application.CategoryService;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** 글 화면(037): 글의 카테고리(누구나), 글쓴이에게는 카테고리 고르기. */
@Component
public class CategoryDetailSection implements PostDetailSection {

    private final CategoryService categories;

    public CategoryDetailSection(CategoryService categories) {
        this.categories = categories;
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        if (!categories.enabled()) {
            return;
        }
        categories.ofPost(post.id()).ifPresent(c -> model.addAttribute("postCategory", c));
        if (viewer.map(v -> v.memberId() == post.authorId()).orElse(false)) {
            model.addAttribute("myCategories", categories.list(post.authorId()));
        }
    }
}
