package com.team.blog.mission.web;

import com.team.blog.mission.application.MissionService;
import com.team.blog.post.web.EditorController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerMapping;

/** 글쓰기 화면(057): 미션 주제로 연 글이면 위에 "오늘의 미션" 안내. (글쓰기 화면 자체가 본인 글만 연다) */
@ControllerAdvice(assignableTypes = EditorController.class)
public class MissionEditorAdvice {

    private final MissionService missions;

    public MissionEditorAdvice(MissionService missions) {
        this.missions = missions;
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
