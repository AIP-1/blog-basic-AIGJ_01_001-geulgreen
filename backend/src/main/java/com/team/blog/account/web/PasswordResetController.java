package com.team.blog.account.web;

import com.team.blog.account.application.EmailRules;
import com.team.blog.account.application.InstaAccountProperties;
import com.team.blog.account.application.InvalidPasswordException;
import com.team.blog.account.application.PasswordMismatchException;
import com.team.blog.account.application.PasswordResetService;
import com.team.blog.account.domain.PasswordPolicyViolation;
import com.team.blog.shared.web.ClientIpResolver;
import com.team.blog.shared.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/** 비밀번호 찾기·재설정 화면(contracts/web-routes.md §4). 로그인 전 기능. */
@Controller
public class PasswordResetController {

    static final String SENT_MESSAGE = "가입된 이메일이면 안내 메일을 보냈어요";

    private final PasswordResetService passwordResetService;
    private final ClientIpResolver clientIpResolver;
    private final MessageSource messageSource;
    private final InstaAccountProperties insta;

    public PasswordResetController(PasswordResetService passwordResetService, ClientIpResolver clientIpResolver,
                                   MessageSource messageSource, InstaAccountProperties insta) {
        this.insta = insta;
        this.passwordResetService = passwordResetService;
        this.clientIpResolver = clientIpResolver;
        this.messageSource = messageSource;
    }

    @GetMapping("/password/forgot")
    public String forgotForm() {
        return "auth/password-forgot";
    }

    /** 가입 여부·한도와 상관없이 항상 같은 화면·문구(SC-006). */
    @PostMapping("/password/forgot")
    public ModelAndView forgot(@RequestParam(name = "email", required = false) String email, HttpServletRequest request) {
        passwordResetService.request(email, clientIpResolver.resolve(request));
        ModelAndView view = new ModelAndView("auth/password-forgot");
        view.addObject("sent", SENT_MESSAGE);
        // 074: 형식이 맞는 이메일이면 가입 여부와 상관없이 코드 칸을 보인다(SC-006)
        if (insta.enabled() && email != null && !email.isBlank()) {
            view.addObject("codeEmail", EmailRules.normalize(email));
        }
        return view;
    }

    /** 074 강성찬 개인 확장: 메일 6자리 코드로 재설정. */
    @PostMapping("/password/reset-code")
    public Object resetWithCode(@RequestParam(name = "email", required = false) String email,
                                @RequestParam(name = "code", required = false) String code,
                                @RequestParam(name = "password", required = false) String password,
                                @RequestParam(name = "passwordConfirm", required = false) String passwordConfirm) {
        ModelAndView view = new ModelAndView("auth/password-forgot");
        view.setStatus(HttpStatus.BAD_REQUEST);
        view.addObject("codeEmail", EmailRules.normalize(email));
        try {
            if (passwordResetService.resetWithCode(email, code, password, passwordConfirm)
                    == PasswordResetService.ResetResult.RESET) {
                return Redirects.seeOther("/login?reset");
            }
            view.addObject("codeError", "코드가 맞지 않거나 시간이 지났어요. 여러 번 틀렸다면 코드를 다시 받아 주세요");
        } catch (InvalidPasswordException e) {
            view.addObject("passwordErrors", e.getViolations().stream().map(this::message).toList());
        } catch (PasswordMismatchException e) {
            view.addObject("passwordErrors",
                    List.of(messageSource.getMessage("error." + PasswordMismatchException.CODE, null, Locale.KOREAN)));
        }
        return view;
    }

    @GetMapping("/password/reset")
    public ModelAndView resetForm(@RequestParam(name = "token", required = false) String token) {
        ModelAndView view = new ModelAndView("auth/password-reset");
        boolean valid = passwordResetService.isValid(token);
        view.addObject("valid", valid);
        view.addObject("token", valid ? token : null);
        view.addObject("passwordErrors", List.of());
        return view;
    }

    @PostMapping("/password/reset")
    public Object reset(@RequestParam(name = "token", required = false) String token,
                        @RequestParam(name = "password", required = false) String password,
                        @RequestParam(name = "passwordConfirm", required = false) String passwordConfirm) {
        List<String> errors;
        try {
            PasswordResetService.ResetResult result = passwordResetService.reset(token, password, passwordConfirm);
            if (result == PasswordResetService.ResetResult.RESET) {
                return Redirects.seeOther("/login?reset");
            }
            ModelAndView expired = new ModelAndView("auth/password-reset");
            expired.addObject("valid", false);
            expired.addObject("passwordErrors", List.of());
            return expired;
        } catch (InvalidPasswordException e) {
            errors = e.getViolations().stream().map(this::message).toList();
        } catch (PasswordMismatchException e) {
            errors = List.of(messageSource.getMessage("error." + PasswordMismatchException.CODE, null, Locale.KOREAN));
        }
        ModelAndView view = new ModelAndView("auth/password-reset");
        view.setStatus(HttpStatus.BAD_REQUEST);
        view.addObject("valid", true);
        view.addObject("token", token);
        view.addObject("passwordErrors", errors);
        return view;
    }

    private String message(PasswordPolicyViolation v) {
        return messageSource.getMessage("password." + v.name(), null, v.name(), Locale.KOREAN);
    }
}
