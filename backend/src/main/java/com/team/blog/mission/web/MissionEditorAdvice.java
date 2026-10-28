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
    private final com.team.blog.mission.application.ClubService clubs;

    public MissionEditorAdvice(MissionService missions, PromptMailService prompts, CurrentUserProvider currentUserProvider,
                               com.team.blog.mission.application.ClubService clubs) {
        this.clubs = clubs;
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

    /** 10-28 발행 설정 "올릴 방": 내가 든 방과 이 글이 올라가 있는지. 방이 없으면 칸을 안 보인다(null). */
    @ModelAttribute("editorRooms")
    public java.util.List<com.team.blog.mission.application.ClubService.PostRoom> editorRooms(HttpServletRequest request) {
        if (!missions.enabled() || !(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> vars)
                || !(vars.get("postId") instanceof String id) || !id.matches("\\d{1,18}")) {
            return null;
        }
        java.util.List<com.team.blog.mission.application.ClubService.PostRoom> rooms = currentUserProvider.current()
                .map(u -> clubs.postRooms(u.memberId(), Long.parseLong(id))).orElse(java.util.List.of());
        return rooms.isEmpty() ? null : rooms;
    }

    /** 10-25 함께 쓰기 방 글감으로 연 글이면 위에 "방 글감" 안내. */
    @ModelAttribute("clubTopic")
    public com.team.blog.mission.application.ClubService.PostTopic clubTopic(HttpServletRequest request) {
        if (!missions.enabled() || !(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> vars)
                || !(vars.get("postId") instanceof String id) || !id.matches("\\d{1,18}")) {
            return null;
        }
        return clubs.topicOfPost(Long.parseLong(id)).orElse(null);
    }
}
