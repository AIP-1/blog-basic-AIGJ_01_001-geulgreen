package com.team.blog.admin.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.admin.application.AdminToolsService;
import com.team.blog.admin.application.SamplePostsService;
import com.team.blog.admin.application.SystemStatusService;
import com.team.blog.moderation.application.ReportReason;
import com.team.blog.shared.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/** 관리자 화면 확장(045~050, 강성찬 개인 확장): 대시보드·글/댓글·방명록·공지·관리 기록. 관리자가 아니면 404(스크립트 없는 폼). */
@Controller
public class AdminToolsController {

    private final AdminToolsService tools;
    private final CurrentUserProvider currentUserProvider;
    private final SamplePostsService samplePosts;
    private final boolean insightsEnabled;
    private final SystemStatusService systemStatus;

    public AdminToolsController(AdminToolsService tools, CurrentUserProvider currentUserProvider, SamplePostsService samplePosts,
                                @Value("${blog.admin.insights.enabled:true}") boolean insightsEnabled, SystemStatusService systemStatus) {
        this.systemStatus = systemStatus;
        this.tools = tools;
        this.currentUserProvider = currentUserProvider;
        this.samplePosts = samplePosts;
        this.insightsEnabled = insightsEnabled;
    }

    @GetMapping("/admin")
    public String dashboard(Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("dash", tools.dashboard(currentUserProvider.current()));
        // 098 30일 추이·인기 글·최근 활동·서버 상태(강성찬 개인 확장). 끄면 예전(045)처럼 최근 7일 추이만
        model.addAttribute("insights", tools.insights(currentUserProvider.current(), insightsEnabled));
        model.addAttribute("insightsEnabled", insightsEnabled);
        if (insightsEnabled) {
            model.addAttribute("serverHost", SERVER_HOST);
            model.addAttribute("serverVersion", systemStatus.version());
            model.addAttribute("serverStartedAt", Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()));
        }
        model.addAttribute("adminTab", "dashboard");
        model.addAttribute("samplePostsEnabled", samplePosts.enabled());
        model.addAttribute("samplePostCount", samplePosts.count());
        return "admin/dashboard";
    }

    /** 083 테스트용 긴 글 넣기·모두 지우기(강성찬 개인 확장). */
    @PostMapping("/admin/sample-posts")
    public RedirectView seedSamplePosts(RedirectAttributes flash) {
        samplePosts.seed(currentUserProvider.current());
        flash.addFlashAttribute("toast", "테스트 글을 넣었어요");
        return seeOther("/admin");
    }

    @PostMapping("/admin/sample-posts/remove")
    public RedirectView removeSamplePosts(RedirectAttributes flash) {
        samplePosts.remove(currentUserProvider.current());
        flash.addFlashAttribute("toast", "테스트 글을 모두 지웠어요");
        return seeOther("/admin");
    }

    @GetMapping("/admin/content")
    public String content(@RequestParam(value = "q", required = false) String q,
                          @RequestParam(value = "type", defaultValue = "posts") String type,
                          @RequestParam(value = "status", defaultValue = "all") String status,
                          @RequestParam(value = "page", defaultValue = "1") int page, Model model,
                          HttpServletResponse response) {
        noStore(response, model);
        boolean comments = "comments".equals(type);
        boolean hiddenOnly = "hidden".equals(status);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("type", comments ? "comments" : "posts");
        model.addAttribute("status", hiddenOnly ? "hidden" : "all");
        model.addAttribute("list", comments ? tools.comments(currentUserProvider.current(), q, hiddenOnly, page)
                : tools.posts(currentUserProvider.current(), q, hiddenOnly, page));
        model.addAttribute("reasons", ReportReason.values());
        model.addAttribute("adminTab", "content");
        return "admin/content";
    }

    @PostMapping("/admin/posts/{id}/hide")
    public RedirectView hidePost(@PathVariable long id, @RequestParam("reason") String reason, RedirectAttributes flash) {
        tools.hidePost(currentUserProvider.current(), id, reason);
        flash.addFlashAttribute("toast", "글을 숨겼어요");
        return seeOther("/admin/content");
    }

    @PostMapping("/admin/posts/{id}/unhide")
    public RedirectView unhidePost(@PathVariable long id, RedirectAttributes flash) {
        tools.unhidePost(currentUserProvider.current(), id);
        flash.addFlashAttribute("toast", "글 숨김을 풀었어요");
        return seeOther("/admin/content");
    }

    @PostMapping("/admin/comments/{id}/hide")
    public RedirectView hideComment(@PathVariable long id, @RequestParam("reason") String reason, RedirectAttributes flash) {
        tools.hideComment(currentUserProvider.current(), id, reason);
        flash.addFlashAttribute("toast", "댓글을 숨겼어요");
        return seeOther("/admin/content?type=comments");
    }

    @PostMapping("/admin/comments/{id}/unhide")
    public RedirectView unhideComment(@PathVariable long id, RedirectAttributes flash) {
        tools.unhideComment(currentUserProvider.current(), id);
        flash.addFlashAttribute("toast", "댓글 숨김을 풀었어요");
        return seeOther("/admin/content?type=comments");
    }

    @GetMapping("/admin/guestbook")
    public String guestbook(@RequestParam(value = "q", required = false) String q,
                            @RequestParam(value = "status", defaultValue = "all") String status,
                            @RequestParam(value = "page", defaultValue = "1") int page, Model model, HttpServletResponse response) {
        noStore(response, model);
        boolean secretOnly = "secret".equals(status);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", secretOnly ? "secret" : "all");
        model.addAttribute("list", tools.guestbook(currentUserProvider.current(), q, secretOnly, page));
        model.addAttribute("adminTab", "guestbook");
        return "admin/guestbook";
    }

    @PostMapping("/admin/guestbook/{id}/delete")
    public RedirectView deleteGuestbook(@PathVariable long id, RedirectAttributes flash) {
        tools.deleteGuestbook(currentUserProvider.current(), id);
        flash.addFlashAttribute("toast", "방명록 글을 지웠어요");
        return seeOther("/admin/guestbook");
    }

    @GetMapping("/admin/notices")
    public String notices(@RequestParam(value = "q", required = false) String q,
                          @RequestParam(value = "page", defaultValue = "1") int page, Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("list", tools.notices(currentUserProvider.current(), q, page));
        model.addAttribute("adminTab", "notices");
        return "admin/notices";
    }

    @PostMapping("/admin/notices")
    public RedirectView postNotice(@RequestParam("message") String message,
                                   @RequestParam(value = "linkUrl", required = false) String linkUrl, RedirectAttributes flash) {
        tools.postNotice(currentUserProvider.current(), message, linkUrl);
        flash.addFlashAttribute("toast", "공지를 올렸어요");
        return seeOther("/admin/notices");
    }

    @PostMapping("/admin/notices/{id}/off")
    public RedirectView noticeOff(@PathVariable long id, RedirectAttributes flash) {
        tools.turnOffNotice(currentUserProvider.current(), id);
        flash.addFlashAttribute("toast", "공지를 내렸어요");
        return seeOther("/admin/notices");
    }

    @GetMapping("/admin/logs")
    public String logs(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "cat", defaultValue = "all") String cat,
                       @RequestParam(value = "page", defaultValue = "1") int page, Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("cat", cat);
        model.addAttribute("list", tools.logs(currentUserProvider.current(), q, cat, page));
        model.addAttribute("adminTab", "logs");
        return "admin/logs";
    }

    private static final String SERVER_HOST = hostName();

    private static String hostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.io.IOException e) {
            return "알 수 없음";
        }
    }

    private static void noStore(HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "private, no-store");
        model.addAttribute("pageNoindex", true);
    }
}
