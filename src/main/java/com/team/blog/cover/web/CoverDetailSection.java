package com.team.blog.cover.web;

import com.team.blog.cover.application.CoverService;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.markdown.MarkdownProperties;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.web.PageMeta;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** 글 화면(055): 대표 사진이 있으면 링크 미리보기(OG) 사진도 그 사진으로. */
@Component
public class CoverDetailSection implements PostDetailSection {

    private final CoverService covers;
    private final String origin;

    public CoverDetailSection(CoverService covers, MarkdownProperties markdownProperties) {
        this.covers = covers;
        this.origin = markdownProperties.siteOrigin().replaceAll("/+$", "");
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        if (!covers.enabled() || !(model.getAttribute("pageMeta") instanceof PageMeta meta)) {
            return;
        }
        covers.of(post.id()).ifPresent(c -> {
            String image = c.imageUrl().startsWith("/") ? origin + c.imageUrl() : c.imageUrl();
            model.addAttribute("ogImage", image);
            model.addAttribute("pageMeta", new PageMeta(meta.title(), meta.description(), meta.canonicalUrl(), image,
                    meta.publishedTime(), meta.modifiedTime()));
        });
    }
}
