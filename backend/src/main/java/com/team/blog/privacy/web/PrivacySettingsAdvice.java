package com.team.blog.privacy.web;

import com.team.blog.account.web.SettingsController;
import com.team.blog.privacy.application.PrivateAccountService;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 087: 설정 > 계정의 비공개 계정 스위치 상태(설정 화면에서만 읽는다). */
@ControllerAdvice(assignableTypes = SettingsController.class)
public class PrivacySettingsAdvice {

    private final PrivateAccountService privacy;
    private final CurrentUserProvider currentUserProvider;

    public PrivacySettingsAdvice(PrivateAccountService privacy, CurrentUserProvider currentUserProvider) {
        this.privacy = privacy;
        this.currentUserProvider = currentUserProvider;
    }

    @ModelAttribute("isPrivateAccount")
    public boolean isPrivateAccount() {
        return currentUserProvider.current().map(u -> privacy.isPrivate(u.memberId())).orElse(false);
    }
}
