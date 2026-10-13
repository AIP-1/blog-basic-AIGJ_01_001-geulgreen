package com.team.blog.account.web;

import com.team.blog.account.application.EmailCodeService;
import com.team.blog.account.application.EmailRules;
import com.team.blog.account.application.InstaAccountProperties;
import com.team.blog.account.application.InvalidEmailException;
import com.team.blog.account.application.MailSender;
import com.team.blog.account.infra.AuthIdentityRepository;
import com.team.blog.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 074 강성찬 개인 확장: 가입 첫 단계(인스타처럼 이메일 → 6자리 코드). 맞히면 세션에 확인한 이메일을 두고,
 * {@code POST /signup}이 같은 이메일이면 인증된 계정으로 만든다.
 */
@RestController
public class SignupCodeApiController {

    static final String VERIFIED_EMAIL = "signup.verifiedEmail";

    private final EmailCodeService codes;
    private final AuthIdentityRepository identities;
    private final MailSender mailSender;
    private final ClientIpResolver ipResolver;
    private final InstaAccountProperties properties;

    public SignupCodeApiController(EmailCodeService codes, AuthIdentityRepository identities, MailSender mailSender,
                                   ClientIpResolver ipResolver, InstaAccountProperties properties) {
        this.codes = codes;
        this.identities = identities;
        this.mailSender = mailSender;
        this.ipResolver = ipResolver;
        this.properties = properties;
    }

    public record Result(String message) {
    }

    @PostMapping("/api/signup/code")
    public ResponseEntity<Result> send(@RequestParam(name = "email", defaultValue = "") String rawEmail,
                                       HttpServletRequest request) {
        if (!properties.enabled()) {
            return ResponseEntity.notFound().build();
        }
        String email;
        try {
            email = EmailRules.normalizeAndValidate(rawEmail);
        } catch (InvalidEmailException e) {
            return reply(HttpStatus.BAD_REQUEST, "이메일 형식을 확인해 주세요");
        }
        if (identities.findLocalByEmail(email).isPresent()) {
            return reply(HttpStatus.CONFLICT, "이미 가입된 이메일이에요. 로그인하거나 비밀번호를 찾아 주세요");
        }
        if (!codes.allowSend(EmailCodeService.Purpose.SIGNUP, email, ipResolver.resolve(request))) {
            return reply(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요");
        }
        String code = codes.issue(EmailCodeService.Purpose.SIGNUP, email, "ok");
        mailSender.send(email, "[블로그] 인증 코드 " + code, "mail/code", Map.of("code", code,
                "minutes", codes.ttl().toMinutes(), "purpose", "가입 화면에 아래 코드를 넣어 주세요."));
        return reply(HttpStatus.OK, "코드를 보냈어요. 메일함을 확인해 주세요");
    }

    @PostMapping("/api/signup/code/verify")
    public ResponseEntity<Result> verify(@RequestParam(name = "email", defaultValue = "") String rawEmail,
                                         @RequestParam(name = "code", defaultValue = "") String code,
                                         HttpServletRequest request) {
        if (!properties.enabled()) {
            return ResponseEntity.notFound().build();
        }
        String email = EmailRules.normalize(rawEmail);
        if (codes.consume(EmailCodeService.Purpose.SIGNUP, email, code).isEmpty()) {
            return reply(HttpStatus.BAD_REQUEST, "코드가 맞지 않거나 시간이 지났어요");
        }
        request.getSession().setAttribute(VERIFIED_EMAIL, email);
        return reply(HttpStatus.OK, "확인했어요");
    }

    private static ResponseEntity<Result> reply(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new Result(message));
    }
}
