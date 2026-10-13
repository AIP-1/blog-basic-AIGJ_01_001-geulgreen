package com.team.blog.account.web;

import com.team.blog.account.application.InstaAccountProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 074 강성찬 개인 확장: 화면이 인스타식 모양(블로그 머리말 등)을 쓸지. */
@ControllerAdvice
public class InstaAccountModelAdvice {

    private final InstaAccountProperties properties;

    public InstaAccountModelAdvice(InstaAccountProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute("instaAccount")
    public boolean instaAccount() {
        return properties.enabled();
    }
}
