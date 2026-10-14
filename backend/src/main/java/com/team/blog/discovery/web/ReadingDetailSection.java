package com.team.blog.discovery.web;

import com.team.blog.discovery.application.PinService;
import com.team.blog.discovery.application.PostNavigationQuery;
import com.team.blog.discovery.application.ReadingProperties;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** 글 화면(강성찬 개인 확장): 039 목차·052 읽기 진행 막대 켜기, 054 고정 버튼, 038 공개 글이면 이전·다음 글. 095 글 아래 관련 글 4개는 화면이 /api/posts/{id}/related로 불러온다(글 화면 쿼리를 늘리지 않게). */
@Component
public class ReadingDetailSection implements PostDetailSection {

    private final ReadingProperties properties;
    private final PostNavigationQuery navigation;
    private final PinService pins;

    public ReadingDetailSection(ReadingProperties properties, PostNavigationQuery navigation, PinService pins) {
        this.properties = properties;
        this.navigation = navigation;
        this.pins = pins;
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        model.addAttribute("tocEnabled", properties.tocEnabled());
        // 052 읽기 진행 막대, 054 글쓴이에게 고정·해제
        model.addAttribute("readProgressEnabled", properties.progressEnabled());
        model.addAttribute("shareEnabled", properties.shareEnabled() && post.isPublic() && !post.hidden());
        if (properties.kakaoEnabled()) {
            model.addAttribute("kakaoJsKey", properties.kakaoJsKey().strip());
        }
        if (viewer.map(v -> v.memberId() == post.authorId()).orElse(false) && post.status().name().equals("PUBLISHED")) {
            model.addAttribute("pinnedHere", pins.pinnedPostId(post.authorId()).map(id -> id == post.id()).orElse(false));
        }
        if (!properties.navigationEnabled() || !post.isPublic() || post.hidden() || post.firstPublicAt() == null) {
            return;
        }
        model.addAttribute("prevPost", navigation.neighbor(post.authorId(), post.id(), post.firstPublicAt(), true).orElse(null));
        model.addAttribute("nextPost", navigation.neighbor(post.authorId(), post.id(), post.firstPublicAt(), false).orElse(null));
    }
}
