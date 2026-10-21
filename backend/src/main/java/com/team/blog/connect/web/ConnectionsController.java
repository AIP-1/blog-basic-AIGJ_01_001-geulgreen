package com.team.blog.connect.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.connect.application.AiKeyService;
import com.team.blog.post.markdown.MarkdownProperties;
import com.team.blog.shared.error.NotFoundException;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUserProvider;
import com.team.blog.shared.web.KoreanDateFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 097 설정 > 연결. 지금은 AI 연결(MCP) 카드 하나 — 다른 연결(웹훅 등)도 이 화면에 카드로 더한다. 본인만, 스크립트 없이 폼으로.
 * 새 키 원문은 발급 응답 화면에만 한 번 보여 준다(되돌아가기 주소·세션에 남기지 않음).
 */
@Controller
public class ConnectionsController {

    private final AiKeyService keys;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;
    private final MarkdownProperties markdownProperties;

    public ConnectionsController(AiKeyService keys, AccountGuard accountGuard, CurrentUserProvider currentUserProvider,
                                 MarkdownProperties markdownProperties) {
        this.keys = keys;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
        this.markdownProperties = markdownProperties;
    }

    @GetMapping("/settings/connections")
    public String page(Model model) {
        return render(model, null, false);
    }

    @PostMapping("/settings/connections/ai")
    public String issue(@RequestParam(value = "name", required = false) String name, Model model) {
        enabled();
        String raw = keys.issue(currentUserProvider.current(), name).orElse(null);
        return render(model, raw, raw == null);
    }

    @PostMapping("/settings/connections/ai/{id}/revoke")
    public RedirectView revoke(@PathVariable("id") long id) {
        enabled();
        keys.revoke(currentUserProvider.current(), id);
        return seeOther("/settings/connections");
    }

    private String render(Model model, String newKey, boolean tooMany) {
        enabled();
        long me = accountGuard.requireLoggedIn(currentUserProvider.current()).memberId();
        model.addAttribute("keys", keys.list(me).stream().map(k -> new KeyRow(k.id(), k.name(),
                KoreanDateFormatter.yearMonthDay(k.createdAt()),
                k.lastUsedAt() == null ? null : KoreanDateFormatter.dateTime(k.lastUsedAt()))).toList());
        model.addAttribute("maxKeys", keys.maxKeys());
        model.addAttribute("newKey", newKey);
        model.addAttribute("tooMany", tooMany);
        String url = markdownProperties.siteOrigin().replaceAll("/+$", "") + "/mcp";
        String key = newKey != null ? newKey : "<연결 키>";
        model.addAttribute("mcpUrl", url);
        model.addAttribute("claudeCode", "claude mcp add --transport http --scope user geulgreen " + url
                + " --header \"Authorization: Bearer " + key + "\"");
        model.addAttribute("cursor", """
                {
                  "mcpServers": {
                    "geulgreen": {
                      "url": "%s",
                      "headers": { "Authorization": "Bearer %s" }
                    }
                  }
                }""".formatted(url, key));
        model.addAttribute("claudeDesktop", """
                {
                  "mcpServers": {
                    "geulgreen": {
                      "command": "npx",
                      "args": ["-y", "mcp-remote", "%s", "--header", "Authorization:${AUTH}"],
                      "env": { "AUTH": "Bearer %s" }
                    }
                  }
                }""".formatted(url, key));
        return "settings/connections";
    }

    public record KeyRow(long id, String name, String createdAt, String lastUsedAt) {
    }

    private void enabled() {
        if (!keys.enabled()) {
            throw new NotFoundException();
        }
    }
}
