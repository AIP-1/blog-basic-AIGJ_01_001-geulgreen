package com.team.blog.admin.web;

import com.team.blog.admin.application.SystemStatusService;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 098 관리자 시스템 화면(강성찬 개인 확장): 기능 스위치·외부 연동·앱 서버 상태. 관리자가 아니거나 꺼져 있으면 404. */
@Controller
public class AdminSystemController {

    private final SystemStatusService status;
    private final CurrentUserProvider currentUserProvider;
    private final boolean enabled;

    public AdminSystemController(SystemStatusService status, CurrentUserProvider currentUserProvider,
                                 @Value("${blog.admin.system.enabled:true}") boolean enabled) {
        this.status = status;
        this.currentUserProvider = currentUserProvider;
        this.enabled = enabled;
    }

    @GetMapping("/admin/system")
    public String system(Model model, HttpServletResponse response) {
        if (!enabled) {
            throw new NotFoundException();
        }
        response.setHeader("Cache-Control", "private, no-store");
        model.addAttribute("pageNoindex", true);
        model.addAttribute("instances", status.instances(currentUserProvider.current()));
        model.addAttribute("integrations", status.integrations(currentUserProvider.current()));
        model.addAttribute("features", status.features(currentUserProvider.current()));
        model.addAttribute("adminTab", "system");
        return "admin/system";
    }
}
