package com.team.blog.account.web;

import com.team.blog.account.application.AgreementRequiredException;
import com.team.blog.account.application.DuplicateEmailException;
import com.team.blog.account.application.EmailSignupService;
import com.team.blog.account.application.EmailRules;
import com.team.blog.account.application.EmailVerificationService;
import com.team.blog.account.application.InstaAccountProperties;
import com.team.blog.account.application.InvalidEmailException;
import com.team.blog.account.application.InvalidPasswordException;
import com.team.blog.account.application.PasswordMismatchException;
import com.team.blog.account.application.WithdrawnAccountExistsException;
import com.team.blog.account.domain.PasswordPolicyViolation;
import com.team.blog.shared.error.HandleTakenException;
import com.team.blog.shared.error.HandleViolationException;
import com.team.blog.shared.error.LoginRequiredException;
import com.team.blog.shared.error.NicknameViolationException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.security.LoginSessionEstablisher;
import com.team.blog.shared.security.RedirectTargetValidator;
import com.team.blog.shared.web.PendingInvite;
import com.team.blog.shared.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * 이메일 가입·인증 화면(contracts/web-routes.md §1). 로그인 화면은 US2에서 이 컨트롤러에 더한다.
 */
@Controller
public class AuthController {

    private final EmailSignupService emailSignupService;
    private final EmailVerificationService emailVerificationService;
    private final LoginSessionEstablisher loginSessionEstablisher;
    private final CurrentUserProvider currentUserProvider;
    private final MessageSource messageSource;
    private final InstaAccountProperties insta;
    private final com.team.blog.account.application.SignupProperties signupProperties;

    public AuthController(EmailSignupService emailSignupService, EmailVerificationService emailVerificationService,
                          LoginSessionEstablisher loginSessionEstablisher, CurrentUserProvider currentUserProvider,
                          MessageSource messageSource, InstaAccountProperties insta,
                          com.team.blog.account.application.SignupProperties signupProperties) {
        this.signupProperties = signupProperties;
        this.insta = insta;
        this.emailSignupService = emailSignupService;
        this.emailVerificationService = emailVerificationService;
        this.loginSessionEstablisher = loginSessionEstablisher;
        this.currentUserProvider = currentUserProvider;
        this.messageSource = messageSource;
    }

    // ----- 가입 -----

    @GetMapping("/signup")
    public ModelAndView signupForm(HttpServletRequest request) {
        ModelAndView view = new ModelAndView("auth/signup");
        addCodeState(view, request, null);
        view.addObject("form", SignupForm.empty());
        view.addObject("errors", Map.of());
        view.addObject("passwordErrors", List.of());
        return view;
    }

    @PostMapping("/signup")
    public Object signup(@ModelAttribute SignupForm form, HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<String> passwordErrors = List.of();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String handleSuggestion = null;
        boolean emailDuplicate = false;
        try {
            // 074 강성찬 개인 확장: 메일 코드로 확인한 이메일이면 인증된 채로 가입하고 바로 시작
            HttpSession session = request.getSession(false);
            boolean codeVerified = session != null
                    && EmailRules.normalize(form.email()).equals(session.getAttribute(SignupCodeApiController.VERIFIED_EMAIL));
            long memberId = emailSignupService.signUp(form.toCommand(), codeVerified);
            if (form.under14()) {
                // 10-21 chan: 14세 미만은 보호자 동의 전이라 로그인하지 않는다
                if (session != null) {
                    session.removeAttribute(SignupCodeApiController.VERIFIED_EMAIL);
                }
                return Redirects.seeOther("/signup/guardian-sent");
            }
            loginSessionEstablisher.establish(memberId, "USER", request, response);
            if (codeVerified) {
                HttpSession current = request.getSession(false);
                if (current != null) {
                    current.removeAttribute(SignupCodeApiController.VERIFIED_EMAIL);
                }
                // 10-25 chan: 초대 링크로 와서 가입했으면 초대한 사람·방으로
                return Redirects.seeOther(PendingInvite.target(request, "/"));
            }
            return Redirects.seeOther("/signup/verify-sent");
        } catch (AgreementRequiredException e) {
            errors.put("agreements", message(AgreementRequiredException.CODE));
        } catch (com.team.blog.account.application.GuardianInfoException e) {
            errors.put("guardian", e.getMessage());
        } catch (InvalidEmailException e) {
            errors.put("email", message(InvalidEmailException.CODE));
        } catch (DuplicateEmailException e) {
            errors.put("email", message(DuplicateEmailException.CODE));
            emailDuplicate = true;
        } catch (WithdrawnAccountExistsException e) {
            errors.put("email", message(WithdrawnAccountExistsException.CODE));
        } catch (InvalidPasswordException e) {
            passwordErrors = passwordMessages(e.getViolations());
            errors.put("password", String.join(" ", passwordErrors));
        } catch (PasswordMismatchException e) {
            errors.put("passwordConfirm", message(PasswordMismatchException.CODE));
        } catch (HandleViolationException e) {
            errors.put("handle", message(e.getCode().name()));
            handleSuggestion = e.getSuggestion();
        } catch (HandleTakenException e) {
            status = HttpStatus.CONFLICT;
            errors.put("handle", message(HandleTakenException.CODE, e.getSuggestion()));
            handleSuggestion = e.getSuggestion();
        } catch (NicknameViolationException e) {
            String code = e.getCode().name();
            if (e.isConcurrent()) {
                status = HttpStatus.CONFLICT;
                errors.put("nickname", message(code + "_CONCURRENT"));
            } else {
                errors.put("nickname", message(code));
            }
        }
        ModelAndView view = new ModelAndView("auth/signup");
        addCodeState(view, request, form.email());
        view.setStatus(status);
        view.addObject("form", form.withoutPasswords());
        view.addObject("errors", errors);
        view.addObject("passwordErrors", passwordErrors);
        view.addObject("handleSuggestion", handleSuggestion);
        view.addObject("emailDuplicate", emailDuplicate);
        return view;
    }

    @GetMapping("/signup/verify-sent")
    public ModelAndView verifySent() {
        requireUser();
        return new ModelAndView("auth/verify-sent");
    }

    // ----- 로그인 (US2) -----

    static final String SUSPENDED_NOTICE = "LOGIN_SUSPENDED_NOTICE";

    @GetMapping("/login")
    public ModelAndView loginForm(@RequestParam(name = "redirect", required = false) String redirect,
                                  HttpServletRequest request) {
        ModelAndView view = new ModelAndView("auth/login");
        view.addObject("redirect", redirect != null && RedirectTargetValidator.isSafe(redirect) ? redirect : null);
        // 10-27 chan: 함께 쓰기 화면은 로그인해야 열려서 링크 미리보기가 이 화면을 읽는다. 방 이름은 보이지 않게 함께 쓰기 그림만
        if (redirect != null && redirect.startsWith("/missions/clubs")) {
            // og:url이 그냥 로그인과 같으면 미리보기 캐시가 섞인다. 초대 코드는 빼고 함께 쓰기 주소로
            view.addObject("ogUrlPath", "/missions/clubs");
            view.addObject("listMeta", new com.team.blog.shared.web.ListMeta("함께 쓰기 · 글그린",
                    "친구·반이 같이 글 쓰는 방이에요. 같은 글감으로 함께 쓰고 서로의 글을 읽어요.", "/images/og-clubs.png"));
        }
        String message = null;
        if (hasFlag(request, "error")) {
            message = "social".equals(request.getParameter("error"))
                    ? "소셜 로그인에 실패했어요. 다시 시도해 주세요" : "이메일 또는 비밀번호가 올바르지 않아요";
        } else if (hasFlag(request, "locked")) {
            message = "잠시 후 다시 시도해 주세요(약 15분)";
        } else if (hasFlag(request, "suspended")) {
            HttpSession session = request.getSession(false);
            Object notice = session == null ? null : session.getAttribute(SUSPENDED_NOTICE);
            if (session != null) {
                session.removeAttribute(SUSPENDED_NOTICE);
            }
            message = notice instanceof String text ? text : "정지된 계정이에요";
        } else if (hasFlag(request, "guardian")) {
            message = "보호자 동의를 기다리고 있어요. 보호자가 메일에서 동의하면 로그인할 수 있어요";
        } else if (hasFlag(request, "social")) {
            message = "다시 소셜 로그인해 주세요";
        }
        view.addObject("message", message);
        view.addObject("instaLogin", insta.enabled());
        view.addObject("notice", hasFlag(request, "reset") ? "비밀번호를 바꿨어요. 다시 로그인해 주세요" : null);
        return view;
    }

    /** {@code ?error}처럼 값 없는 표시 파라미터도 인식한다. */
    private static boolean hasFlag(HttpServletRequest request, String name) {
        if (request.getParameter(name) != null) {
            return true;
        }
        String query = request.getQueryString();
        if (query == null) {
            return false;
        }
        for (String part : query.split("&")) {
            if (part.equals(name) || part.startsWith(name + "=")) {
                return true;
            }
        }
        return false;
    }

    // ----- 인증 -----

    @GetMapping("/auth/verify")
    public ModelAndView verify(@RequestParam(name = "token", required = false) String token, HttpServletRequest request) {
        EmailVerificationService.VerifyResult result = emailVerificationService.verify(token);
        boolean verified = result == EmailVerificationService.VerifyResult.VERIFIED;
        // 10-25 chan: 초대 링크로 와서 메일 인증까지 마쳤으면 결과 화면 대신 그 초대로(방 참여는 인증 뒤에만 되므로 여기서 이어 준다)
        String invite = verified ? PendingInvite.path(request.getSession(false)) : null;
        if (invite != null) {
            return new ModelAndView("redirect:" + invite);
        }
        ModelAndView view = new ModelAndView("auth/verify-result");
        view.addObject("verified", verified);
        view.addObject("loggedIn", currentUserProvider.current().isPresent());
        return view;
    }

    @PostMapping("/auth/verify/resend")
    public ModelAndView resend() {
        CurrentUser user = requireUser();
        EmailVerificationService.ResendResult result = emailVerificationService.resend(user.memberId());
        ModelAndView view = new ModelAndView("auth/verify-sent");
        view.addObject("notice", switch (result) {
            case SENT -> "인증 메일을 다시 보냈어요";
            case RATE_LIMITED -> "잠시 후 다시 시도해 주세요";
            case ALREADY_VERIFIED -> "이미 인증된 계정이에요";
        });
        view.addObject("noticeKind", result == EmailVerificationService.ResendResult.RATE_LIMITED ? "error" : "success"); // 결과 상자 색(toast.js)
        view.addObject("resendResult", result.name());
        return view;
    }

    // ----- 도우미 -----

    /** 074: 가입 화면이 코드 단계를 보일지, 이 이메일을 이미 코드로 확인했는지. */
    private void addCodeState(ModelAndView view, HttpServletRequest request, String email) {
        view.addObject("instaSignup", insta.enabled());
        view.addObject("simpleSignup", signupProperties.simple());
        HttpSession session = request.getSession(false);
        Object verified = session == null ? null : session.getAttribute(SignupCodeApiController.VERIFIED_EMAIL);
        view.addObject("codeVerifiedEmail", verified != null && (email == null || verified.equals(EmailRules.normalize(email)))
                ? verified : null);
    }

    private CurrentUser requireUser() {
        return currentUserProvider.current().orElseThrow(LoginRequiredException::new);
    }

    private List<String> passwordMessages(List<PasswordPolicyViolation> violations) {
        return violations.stream()
                .map(v -> messageSource.getMessage("password." + v.name(), null, v.name(), Locale.KOREAN))
                .toList();
    }

    private String message(String code, Object... args) {
        return messageSource.getMessage("error." + code, args, code, Locale.KOREAN);
    }
}
