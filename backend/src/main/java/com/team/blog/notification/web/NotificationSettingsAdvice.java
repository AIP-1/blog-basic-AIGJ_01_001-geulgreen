package com.team.blog.notification.web;

import com.team.blog.account.web.SettingsController;
import com.team.blog.notification.application.NotificationService;
import com.team.blog.notification.application.TelegramService;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 설정 화면의 "알림" 칸(25 §7)에 지금 켜짐 여부를 넣는다. account 모듈은 바꾸지 않는다. */
@ControllerAdvice(assignableTypes = SettingsController.class)
public class NotificationSettingsAdvice {

    private final NotificationService service;
    private final CurrentUserProvider currentUserProvider;
    private final TelegramService telegram;

    public NotificationSettingsAdvice(NotificationService service, CurrentUserProvider currentUserProvider,
                                      TelegramService telegram) {
        this.telegram = telegram;
        this.service = service;
        this.currentUserProvider = currentUserProvider;
    }

    private static final Map<String, String> LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("COMMENT", "내 글에 달린 댓글");
        labels.put("REPLY", "내 댓글에 달린 답글");
        labels.put("LIKE", "좋아요");
        labels.put("FOLLOW", "새 팔로워");
        labels.put("NEW_POST", "팔로우한 사람의 새 글");
        LABELS = java.util.Collections.unmodifiableMap(labels);
    }

    @ModelAttribute("notificationLabels")
    public Map<String, String> notificationLabels() {
        return LABELS;
    }

    @ModelAttribute("notificationSettings")
    public Map<String, Boolean> notificationSettings() {
        return currentUserProvider.current().isPresent() ? service.settings(currentUserProvider.current()) : Map.of();
    }

    /** 096 강성찬 개인 확장: 텔레그램 칸. 지금은 관리자에게만 보내므로 관리자 화면에만 보인다. null이면 칸을 숨긴다. */
    @ModelAttribute("telegramLinked")
    public Boolean telegramLinked() {
        return currentUserProvider.current().filter(u -> telegram.active() && "ADMIN".equals(u.role()))
                .map(CurrentUser::memberId).map(telegram::linked).orElse(null);
    }
}
