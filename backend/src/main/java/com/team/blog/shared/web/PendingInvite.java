package com.team.blog.shared.web;

import com.team.blog.shared.security.RedirectTargetValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 10-25 chan: 친구 초대 링크({@code /i/{아이디}[/{코드}]})를 비회원이 열면 그 경로를 세션에 적어 두고, 가입·로그인이 끝나면
 * 처음 화면 대신 그 경로로 돌려보낸다. 초대 화면이 받아들인 뒤 지운다.
 */
public final class PendingInvite {

    public static final String ATTRIBUTE = "PENDING_INVITE_PATH";

    private PendingInvite() {
    }

    public static void remember(HttpServletRequest request, String path) {
        request.getSession(true).setAttribute(ATTRIBUTE, path);
    }

    /** 세션에 적힌 초대 경로(검증된 상대 경로), 없으면 null. */
    public static String path(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(ATTRIBUTE);
        return value instanceof String path && RedirectTargetValidator.isSafe(path) ? path : null;
    }

    /** 초대 경로가 있으면 그곳, 없으면 {@code fallback}. */
    public static String target(HttpServletRequest request, String fallback) {
        String path = path(request.getSession(false));
        return path != null ? path : fallback;
    }

    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(ATTRIBUTE);
        }
    }
}
