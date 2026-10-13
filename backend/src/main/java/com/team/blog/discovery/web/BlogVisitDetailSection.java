package com.team.blog.discovery.web;

import com.team.blog.discovery.application.BlogVisitService;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.web.ClientIpResolver;
import com.team.blog.shared.web.VisitorCookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 051: 글 화면을 본 것도 그 블로그 방문으로 센다(강성찬 개인 확장). */
@Component
public class BlogVisitDetailSection implements PostDetailSection {

    private final BlogVisitService visits;
    private final ClientIpResolver ipResolver;

    public BlogVisitDetailSection(BlogVisitService visits, ClientIpResolver ipResolver) {
        this.visits = visits;
        this.ipResolver = ipResolver;
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        if (!visits.enabled() || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        visits.recordVisit(post.authorId(), viewer, VisitorCookie.read(request), ipResolver.resolve(request),
                request.getHeader("User-Agent"));
    }
}
