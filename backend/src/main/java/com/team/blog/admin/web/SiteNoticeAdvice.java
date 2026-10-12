package com.team.blog.admin.web;

import com.team.blog.admin.application.AdminToolsService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 049 사이트 공지(강성찬 개인 확장): 켜진 공지가 있으면 모든 화면 머리말 아래 띠로. */
@ControllerAdvice
public class SiteNoticeAdvice {

    private final AdminToolsService tools;

    public SiteNoticeAdvice(AdminToolsService tools) {
        this.tools = tools;
    }

    @ModelAttribute("siteNotice")
    public AdminToolsService.Notice siteNotice() {
        return tools.activeNotice().orElse(null);
    }
}
