package com.team.blog.mission.web;

import com.team.blog.account.web.SettingsController;
import com.team.blog.mission.application.PromptMailService;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/** 10-21 설정 › 알림의 [저녁 글감 메일] 켜기·끄기(기본 꺼짐). */
@Controller
public class PromptMailController {

    private final PromptMailService prompts;
    private final CurrentUserProvider currentUserProvider;

    public PromptMailController(PromptMailService prompts, CurrentUserProvider currentUserProvider) {
        this.prompts = prompts;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/settings/prompt-mail")
    public RedirectView set(@RequestParam(value = "on", required = false) String on) {
        prompts.set(currentUserProvider.current(), on != null);
        RedirectView view = new RedirectView("/settings#prompt-mail-heading");
        view.setStatusCode(HttpStatus.SEE_OTHER);
        view.setExposeModelAttributes(false);
        return view;
    }

    /** 설정 화면에 켜져 있는지. 로그인 안 했거나 미션이 꺼져 있으면 null(칸을 숨긴다). */
    @ControllerAdvice(assignableTypes = SettingsController.class)
    public static class Advice {

        private final PromptMailService prompts;
        private final CurrentUserProvider currentUserProvider;

        public Advice(PromptMailService prompts, CurrentUserProvider currentUserProvider) {
            this.prompts = prompts;
            this.currentUserProvider = currentUserProvider;
        }

        @ModelAttribute("promptMailOn")
        public Boolean promptMailOn() {
            return currentUserProvider.current().filter(u -> prompts.todayFor(u.memberId()).isPresent())
                    .map(u -> prompts.subscribed(u.memberId())).orElse(null);
        }
    }
}
