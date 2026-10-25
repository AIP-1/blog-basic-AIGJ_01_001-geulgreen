package com.team.blog.invite.web;

import com.team.blog.invite.application.InviteProperties;
import com.team.blog.invite.application.InviteService;
import com.team.blog.shared.error.LoginRequiredException;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.web.ClientIpResolver;
import com.team.blog.shared.web.PendingInvite;
import com.team.blog.shared.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 10-25 chan: 친구 초대 링크 {@code /i/{아이디}[/{코드}]}와 내 초대 링크 화면 {@code /me/invite}.
 * <ul>
 *   <li>비회원: "○○님이 초대했어요" 화면에 [Google로 시작] [이메일로 시작] 두 버튼. 경로를 세션에 적어 두고({@link PendingInvite}),
 *       가입·로그인이 끝나면 여기로 돌아와 바로 받아들인다.</li>
 *   <li>회원: 같은 화면에 [함께 시작하기] 버튼 하나. 누르면 초대한 사람을 팔로우하고 방에 들어간다.</li>
 * </ul>
 * {@code i}는 한 글자라 블로그 주소 형식이 아니어서 예약어 없이도 블로그 경로로 읽히지 않는다. 끄기: {@code blog.invite.enabled=false}({@link InviteProperties}).
 */
@Controller
@ConditionalOnProperty(name = "blog.invite.enabled", havingValue = "true", matchIfMissing = true)
public class InviteController {

    private final InviteService invites;
    private final CurrentUserProvider currentUserProvider;
    private final ClientIpResolver clientIpResolver;

    public InviteController(InviteService invites, CurrentUserProvider currentUserProvider, ClientIpResolver clientIpResolver) {
        this.invites = invites;
        this.currentUserProvider = currentUserProvider;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping({"/i/{handle}", "/i/{handle}/{code}"})
    public Object landing(@PathVariable("handle") String handle, @PathVariable(value = "code", required = false) String code,
                          HttpServletRequest request, Model model, RedirectAttributes flash) {
        InviteService.Invite invite = invites.find(handle, code).orElseThrow(NotFoundException::new);
        Optional<CurrentUser> me = currentUserProvider.current();
        if (me.isEmpty()) {
            PendingInvite.remember(request, invite.path());
            model.addAttribute("invite", invite);
            model.addAttribute("loginUrl", "/login?redirect=" + URLEncoder.encode(invite.path(), StandardCharsets.UTF_8));
            return "invite/landing";
        }
        // 가입·로그인을 마치고 돌아온 길이면 버튼을 다시 누르게 하지 않고 바로 받아들인다
        if (invite.path().equals(PendingInvite.path(request.getSession(false)))) {
            PendingInvite.clear(request);
            return accept(invite, request, flash);
        }
        model.addAttribute("invite", invite);
        model.addAttribute("self", invite.inviter().memberId() == me.get().memberId());
        return "invite/landing";
    }

    @PostMapping({"/i/{handle}", "/i/{handle}/{code}"})
    public RedirectView join(@PathVariable("handle") String handle, @PathVariable(value = "code", required = false) String code,
                             HttpServletRequest request, RedirectAttributes flash) {
        InviteService.Invite invite = invites.find(handle, code).orElseThrow(NotFoundException::new);
        PendingInvite.clear(request);
        return accept(invite, request, flash);
    }

    /** 내 초대 링크: 블로그 초대와 내 함께 쓰기 방마다 하나. 복사·공유 버튼은 js/invite/share.js. */
    @GetMapping("/me/invite")
    public String mine(Model model) {
        CurrentUser me = currentUserProvider.current().orElseThrow(LoginRequiredException::new);
        model.addAttribute("links", invites.myLinks(me.memberId()));
        return "invite/mine";
    }

    private RedirectView accept(InviteService.Invite invite, HttpServletRequest request, RedirectAttributes flash) {
        InviteService.Outcome outcome = invites.accept(currentUserProvider.current(), invite, clientIpResolver.resolve(request));
        flash.addFlashAttribute("clubNotice", outcome.notice());
        return Redirects.seeOther(outcome.target());
    }
}
