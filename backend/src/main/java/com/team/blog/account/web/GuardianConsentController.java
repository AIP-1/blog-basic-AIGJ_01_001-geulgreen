package com.team.blog.account.web;

import com.team.blog.account.application.GuardianConsentService;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 10-21 chan: 14세 미만 가입의 보호자 동의. 아이에게는 "보호자에게 메일을 보냈어요" 화면, 보호자에게는 메일 링크로 여는
 * 동의 화면([동의해요]·[동의하지 않아요])과 결과 화면. 로그인 없이 연다(링크 토큰으로만 확인).
 */
@Controller
public class GuardianConsentController {

    private final GuardianConsentService guardianConsent;

    public GuardianConsentController(GuardianConsentService guardianConsent) {
        this.guardianConsent = guardianConsent;
    }

    @GetMapping("/signup/guardian-sent")
    public String sent(Model model) {
        model.addAttribute("days", GuardianConsentService.TTL.toDays());
        return "auth/guardian-sent";
    }

    @GetMapping("/auth/guardian/consent")
    public String page(@RequestParam(value = "token", required = false) String token, Model model) {
        Optional<GuardianConsentService.Pending> pending = guardianConsent.find(token);
        model.addAttribute("pending", pending.orElse(null));
        model.addAttribute("token", token);
        model.addAttribute("days", GuardianConsentService.TTL.toDays());
        return "auth/guardian-consent";
    }

    @PostMapping("/auth/guardian/consent")
    public String answer(@RequestParam("token") String token, @RequestParam("answer") String answer, Model model) {
        boolean agree = "agree".equals(answer);
        boolean done = agree ? guardianConsent.consent(token) : guardianConsent.decline(token);
        model.addAttribute("result", !done ? "expired" : agree ? "agreed" : "declined");
        return "auth/guardian-done";
    }
}
