package com.team.blog.admin.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.admin.application.AdminToolsService;
import com.team.blog.admin.application.SamplePostsService;
import com.team.blog.moderation.application.ReportReason;
import com.team.blog.shared.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/** 관리자 화면 확장(045~050, 강성찬 개인 확장): 대시보드·글/댓글·방명록·공지·관리 기록. 관리자가 아니면 404(스크립트 없는 폼). */
@Controller
public class AdminToolsController {

    private final AdminToolsService tools;
    private final CurrentUserProvider currentUserProvider;
    private final SamplePostsService samplePosts;

    public AdminToolsController(AdminToolsService tools, CurrentUserProvider currentUserProvider, SamplePostsService samplePosts) {
        this.tools = tools;
        this.currentUserProvider = currentUserProvider;
        this.samplePosts = samplePosts;
    }

    @GetMapping("/admin")
    public String dashboard(Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("dash", tools.dashboard(currentUserProvider.current()));
        model.addAttribute("adminTab", "dashboard");
        model.addAttribute("samplePostsEnabled", samplePosts.enabled());
        model.addAttribute("samplePostCount", samplePosts.count());
        return "admin/dashboard";
    }

    /** 083 테스트용 긴 글 넣기·모두 지우기(강성찬 개인 확장). */
    @PostMapping("/admin/sample-posts")
    public RedirectView seedSamplePosts() {
        samplePosts.seed(currentUserProvider.current());
        return seeOther("/admin");
    }

    @PostMapping("/admin/sample-posts/remove")
    public RedirectView removeSamplePosts() {
        samplePosts.remove(currentUserProvider.current());
        return seeOther("/admin");
    }

    @GetMapping("/admin/content")
    public String content(@RequestParam(value = "q", required = false) String q,
                          @RequestParam(value = "type", defaultValue = "posts") String type, Model model,
                          HttpServletResponse response) {
        noStore(response, model);
        boolean comments = "comments".equals(type);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("type", comments ? "comments" : "posts");
        model.addAttribute("rows", comments ? tools.comments(currentUserProvider.current(), q) : tools.posts(currentUserProvider.current(), q));
        model.addAttribute("reasons", ReportReason.values());
        model.addAttribute("adminTab", "content");
        return "admin/content";
    }

    @PostMapping("/admin/posts/{id}/hide")
    public RedirectView hidePost(@PathVariable long id, @RequestParam("reason") String reason) {
        tools.hidePost(currentUserProvider.current(), id, reason);
        return seeOther("/admin/content");
    }

    @PostMapping("/admin/posts/{id}/unhide")
    public RedirectView unhidePost(@PathVariable long id) {
        tools.unhidePost(currentUserProvider.current(), id);
        return seeOther("/admin/content");
    }

    @PostMapping("/admin/comments/{id}/hide")
    public RedirectView hideComment(@PathVariable long id, @RequestParam("reason") String reason) {
        tools.hideComment(currentUserProvider.current(), id, reason);
        return seeOther("/admin/content?type=comments");
    }

    @PostMapping("/admin/comments/{id}/unhide")
    public RedirectView unhideComment(@PathVariable long id) {
        tools.unhideComment(currentUserProvider.current(), id);
        return seeOther("/admin/content?type=comments");
    }

    @GetMapping("/admin/guestbook")
    public String guestbook(Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("entries", tools.guestbook(currentUserProvider.current()));
        model.addAttribute("adminTab", "guestbook");
        return "admin/guestbook";
    }

    @PostMapping("/admin/guestbook/{id}/delete")
    public RedirectView deleteGuestbook(@PathVariable long id) {
        tools.deleteGuestbook(currentUserProvider.current(), id);
        return seeOther("/admin/guestbook");
    }

    @GetMapping("/admin/notices")
    public String notices(Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("notices", tools.notices(currentUserProvider.current()));
        model.addAttribute("adminTab", "notices");
        return "admin/notices";
    }

    @PostMapping("/admin/notices")
    public RedirectView postNotice(@RequestParam("message") String message,
                                   @RequestParam(value = "linkUrl", required = false) String linkUrl) {
        tools.postNotice(currentUserProvider.current(), message, linkUrl);
        return seeOther("/admin/notices");
    }

    @PostMapping("/admin/notices/{id}/off")
    public RedirectView noticeOff(@PathVariable long id) {
        tools.turnOffNotice(currentUserProvider.current(), id);
        return seeOther("/admin/notices");
    }

    @GetMapping("/admin/logs")
    public String logs(Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("logs", tools.logs(currentUserProvider.current()));
        model.addAttribute("adminTab", "logs");
        return "admin/logs";
    }

    private static void noStore(HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "private, no-store");
        model.addAttribute("pageNoindex", true);
    }
}
