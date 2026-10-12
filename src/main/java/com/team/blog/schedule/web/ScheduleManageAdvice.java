package com.team.blog.schedule.web;

import com.team.blog.post.web.ManagePostsController;
import com.team.blog.schedule.application.ScheduleService;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 내 글 관리(011)의 임시글에 "⏰ 예약 · 시각"(042 강성찬 개인 확장). */
@ControllerAdvice(assignableTypes = ManagePostsController.class)
public class ScheduleManageAdvice {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final ScheduleService schedules;
    private final CurrentUserProvider currentUserProvider;
    private final JdbcTemplate jdbc;

    public ScheduleManageAdvice(ScheduleService schedules, CurrentUserProvider currentUserProvider, JdbcTemplate jdbc) {
        this.schedules = schedules;
        this.currentUserProvider = currentUserProvider;
        this.jdbc = jdbc;
    }

    @ModelAttribute("scheduleLabels")
    public Map<Long, String> scheduleLabels() {
        Map<Long, String> labels = new HashMap<>();
        if (!schedules.enabled()) {
            return labels;
        }
        currentUserProvider.current().map(CurrentUser::memberId).ifPresent(me -> jdbc.query(
                "SELECT s.post_id, s.publish_at FROM post_schedule s JOIN post p ON p.id = s.post_id WHERE p.author_id = ?",
                rs -> {
                    labels.put(rs.getLong("post_id"), LABEL.format(rs.getTimestamp("publish_at").toInstant()));
                }, me));
        return labels;
    }
}
