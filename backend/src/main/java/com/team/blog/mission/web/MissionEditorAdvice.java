package com.team.blog.mission.web;

import com.team.blog.mission.application.MissionService;
import com.team.blog.mission.application.PromptMailService;
import com.team.blog.post.web.EditorController;
import com.team.blog.shared.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerMapping;

/** 글쓰기 화면(057): 미션 주제로 연 글이면 위에 "오늘의 미션" 안내. (글쓰기 화면 자체가 본인 글만 연다) */
@ControllerAdvice(assignableTypes = EditorController.class)
public class MissionEditorAdvice {

    private final MissionService missions;
    private final PromptMailService prompts;
    private final CurrentUserProvider currentUserProvider;

    public MissionEditorAdvice(MissionService missions, PromptMailService prompts, CurrentUserProvider currentUserProvider) {
        this.missions = missions;
        this.prompts = prompts;
        this.currentUserProvider = currentUserProvider;
    }

    /** 10-21 오늘의 글감: 빈 새 글에 보여 줄 글감 한 줄(화면이 빈 글인지 본다). */
    @ModelAttribute("todayTopic")
    public MissionService.Topic todayTopic() {
        return currentUserProvider.current().flatMap(u -> prompts.todayFor(u.memberId())).orElse(null);
    }

    @ModelAttribute("missionTopic")
    public MissionService.PostMission missionTopic(HttpServletRequest request) {
        if (!missions.enabled() || !(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> vars)
                || !(vars.get("postId") instanceof String id) || !id.matches("\\d{1,18}")) {
            return null;
        }
        return missions.ofPost(Long.parseLong(id)).orElse(null);
    }
}
