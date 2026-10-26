package com.team.blog.shared.security;

import com.team.blog.account.application.AccountStatusChecker;
import com.team.blog.account.application.EmailRules;
import com.team.blog.account.application.LoginAttemptService;
import com.team.blog.account.application.LoginRecorder;
import com.team.blog.account.application.LoginStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * 폼 로그인 성공(비밀번호 일치·정지 아님): 실패 카운터 삭제, {@code last_login_at} 기록, 이동.
 * 탈퇴 유예면 복구 전용 세션으로 표시하고 {@code 303 /account/restore}(42 P-12).
 */
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final AccountStatusChecker accountStatusChecker;
    private final LoginRecorder loginRecorder;
    private final RequestCache requestCache = SecurityConfig.requestCache();

    public FormLoginSuccessHandler(LoginAttemptService loginAttemptService, AccountStatusChecker accountStatusChecker,
                                   LoginRecorder loginRecorder) {
        this.loginAttemptService = loginAttemptService;
        this.accountStatusChecker = accountStatusChecker;
        this.loginRecorder = loginRecorder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        long memberId = Long.parseLong(authentication.getName());
        loginAttemptService.recordSuccess(EmailRules.normalize(request.getParameter("email")));
        loginRecorder.recordLogin(memberId);
        String target = afterLoginTarget(request, response, requestCache, request.getParameter("redirect"));
        if (accountStatusChecker.checkOnLogin(memberId) instanceof LoginStatus.WithdrawnPending) {
            RestoreOnlySessionFilter.markRestoreOnly(request);
            target = RestoreOnlySessionFilter.RESTORE_PATH;
        }
        AuthResponses.seeOther(request, response, target);
    }

    /** 검증된 {@code redirect} → 세션의 초대 경로 → 저장된 요청(상대 경로로) → {@code /}. */
    static String afterLoginTarget(HttpServletRequest request, HttpServletResponse response, RequestCache cache,
                                   String redirect) {
        SavedRequest saved = cache.getRequest(request, response);
        cache.removeRequest(request, response);
        if (redirect != null && !redirect.isBlank()) {
            return RedirectTargetValidator.sanitize(redirect);
        }
        // 10-25 chan: 초대 링크를 열어 두고 로그인했으면(소셜 포함) 그 초대로 돌아간다
        String invite = com.team.blog.shared.web.PendingInvite.path(request.getSession(false));
        if (invite != null) {
            return invite;
        }
        if (saved != null) {
            try {
                URI uri = URI.create(saved.getRedirectUrl());
                String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                String relative = path.startsWith(request.getContextPath())
                        ? path.substring(request.getContextPath().length()) : path;
                return RedirectTargetValidator.sanitize(relative);
            } catch (IllegalArgumentException e) {
                return RedirectTargetValidator.FALLBACK;
            }
        }
        return RedirectTargetValidator.FALLBACK;
    }
}
