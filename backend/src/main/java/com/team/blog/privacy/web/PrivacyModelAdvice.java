package com.team.blog.privacy.web;

import com.team.blog.privacy.application.PrivateAccountService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 설정 화면의 비공개 계정 링크를 끄기 스위치에 따라 보이게(073). */
@ControllerAdvice
public class PrivacyModelAdvice {

    private final PrivateAccountService privacy;

    public PrivacyModelAdvice(PrivateAccountService privacy) {
        this.privacy = privacy;
    }

    @ModelAttribute("privateAccountsEnabled")
    public boolean privateAccountsEnabled() {
        return privacy.enabled();
    }
}
