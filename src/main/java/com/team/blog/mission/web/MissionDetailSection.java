package com.team.blog.mission.web;

import com.team.blog.mission.application.MissionService;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** 글 화면(057): 미션 글이면 "🗓 오늘의 미션 · 주제"(누구나), 글쓴이에게는 완료·연속 일수. */
@Component
public class MissionDetailSection implements PostDetailSection {

    private final MissionService missions;

    public MissionDetailSection(MissionService missions) {
        this.missions = missions;
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        if (!missions.enabled()) {
            return;
        }
        missions.ofPost(post.id()).filter(MissionService.PostMission::completed).ifPresent(m -> {
            model.addAttribute("postMission", m);
            if (viewer.map(v -> v.memberId() == post.authorId()).orElse(false)) {
                model.addAttribute("missionStats", missions.stats(post.authorId()));
            }
        });
    }
}
