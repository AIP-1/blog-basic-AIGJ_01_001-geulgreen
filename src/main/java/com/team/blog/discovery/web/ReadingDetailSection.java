package com.team.blog.discovery.web;

import com.team.blog.discovery.application.PostNavigationQuery;
import com.team.blog.discovery.application.ReadingProperties;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** 글 화면(강성찬 개인 확장): 039 목차 켜기, 038 공개 글이면 이전·다음 글과 관련 글. */
@Component
public class ReadingDetailSection implements PostDetailSection {

    private final ReadingProperties properties;
    private final PostNavigationQuery navigation;

    public ReadingDetailSection(ReadingProperties properties, PostNavigationQuery navigation) {
        this.properties = properties;
        this.navigation = navigation;
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        model.addAttribute("tocEnabled", properties.tocEnabled());
        if (!properties.navigationEnabled() || !post.isPublic() || post.hidden() || post.firstPublicAt() == null) {
            return;
        }
        model.addAttribute("prevPost", navigation.neighbor(post.authorId(), post.id(), post.firstPublicAt(), true).orElse(null));
        model.addAttribute("nextPost", navigation.neighbor(post.authorId(), post.id(), post.firstPublicAt(), false).orElse(null));
        model.addAttribute("relatedPosts", navigation.related(post.id(), properties.relatedCount()));
    }
}
