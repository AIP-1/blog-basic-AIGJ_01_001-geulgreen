package com.team.blog.contact.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.contact.application.InquiryService;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.team.blog.shared.web.AdminList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/** 090 문의하기(강성찬 개인 확장): 누구나 보내는 문의 양식과 관리자 문의함(스크립트 없는 폼). */
@Controller
@ConditionalOnProperty(name = "blog.contact.enabled", havingValue = "true", matchIfMissing = true)
public class ContactController {

    private final InquiryService inquiries;
    private final CurrentUserProvider currentUserProvider;
    private final ClientIpResolver ipResolver;

    public ContactController(InquiryService inquiries, CurrentUserProvider currentUserProvider, ClientIpResolver ipResolver) {
        this.inquiries = inquiries;
        this.currentUserProvider = currentUserProvider;
        this.ipResolver = ipResolver;
    }

    @GetMapping("/contact")
    public String form(Model model) {
        return form(model, null, null, null, null, null);
    }

    private String form(Model model, String kind, String title, String content, String email, String error) {
        String memberEmail = inquiries.memberEmail(currentUserProvider.current());
        model.addAttribute("kinds", InquiryService.KIND_ORDER.stream()
                .map(k -> new String[] {k, InquiryService.KINDS.get(k)}).toList());
        model.addAttribute("kind", kind);
        model.addAttribute("formTitle", title);
        model.addAttribute("formContent", content);
        model.addAttribute("memberEmail", memberEmail);
        model.addAttribute("email", memberEmail != null ? memberEmail : email);
        model.addAttribute("error", error);
        model.addAttribute("titleMax", InquiryService.TITLE_MAX);
        model.addAttribute("contentMax", InquiryService.CONTENT_MAX);
        return "contact/form";
    }

    @PostMapping("/contact")
    public ModelAndView send(@RequestParam(value = "kind", required = false) String kind,
                       @RequestParam(value = "title", required = false) String title,
                       @RequestParam(value = "content", required = false) String content,
                       @RequestParam(value = "email", required = false) String email,
                       HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            inquiries.submit(currentUserProvider.current(), ipResolver.resolve(request), kind, title, content, email);
            return new ModelAndView(seeOther("/contact/done"));
        } catch (InquiryService.Rejected e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new ModelAndView(form(model, kind, title, content, email, e.getMessage()));
        }
    }

    @GetMapping("/contact/done")
    public String done() {
        return "contact/done";
    }

    @GetMapping("/admin/inquiries")
    public String inbox(@RequestParam(value = "status", defaultValue = "all") String status,
                        @RequestParam(value = "q", required = false) String q,
                        @RequestParam(value = "page", defaultValue = "1") int page, Model model, HttpServletResponse response) {
        noStore(response, model);
        model.addAttribute("status", status);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("list", AdminList.of(inquiries.list(currentUserProvider.current()), i -> switch (status) {
            case "new" -> !i.done();
            case "done" -> i.done();
            default -> true;
        } && AdminList.matches(q, i.title(), i.content(), i.email(), i.memberHandle()), page));
        return "admin/inquiries";
    }

    @GetMapping("/admin/inquiries/{id}")
    public String inquiry(@PathVariable long id, Model model, HttpServletResponse response) {
        return inquiry(id, model, response, null, null);
    }

    private String inquiry(long id, Model model, HttpServletResponse response, String reply, String error) {
        noStore(response, model);
        model.addAttribute("inquiry", inquiries.get(currentUserProvider.current(), id));
        model.addAttribute("replyDraft", reply);
        model.addAttribute("error", error);
        return "admin/inquiry";
    }

    @PostMapping("/admin/inquiries/{id}/reply")
    public ModelAndView reply(@PathVariable long id, @RequestParam(value = "reply", required = false) String reply, Model model,
                        HttpServletResponse response) {
        try {
            inquiries.reply(currentUserProvider.current(), id, reply);
            return new ModelAndView(seeOther("/admin/inquiries/" + id));
        } catch (InquiryService.Rejected e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new ModelAndView(inquiry(id, model, response, reply, e.getMessage()));
        }
    }

    private static void noStore(HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "private, no-store");
        model.addAttribute("pageNoindex", true);
        model.addAttribute("adminTab", "inquiries");
    }
}
