package com.team.blog.friend.web;

import static com.team.blog.shared.web.Redirects.seeOther;

import com.team.blog.friend.application.GroupService;
import com.team.blog.shared.security.AccountGuard;
import com.team.blog.shared.security.CurrentUserProvider;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

/** 친구 그룹(030, 강성찬 개인 확장): 설정 친구 화면(087에서 합침)의 그룹 폼과 그룹 공개 글의 대상 고르기. 스크립트 없이 폼으로 동작한다. */
@Controller
public class GroupPageController {

    private static final String BACK = "/settings/friends#groups";

    private final GroupService groups;
    private final AccountGuard accountGuard;
    private final CurrentUserProvider currentUserProvider;
    private final JdbcTemplate jdbc;

    public GroupPageController(GroupService groups, AccountGuard accountGuard,
                               CurrentUserProvider currentUserProvider, JdbcTemplate jdbc) {
        this.groups = groups;
        this.accountGuard = accountGuard;
        this.currentUserProvider = currentUserProvider;
        this.jdbc = jdbc;
    }

    private long me() {
        return accountGuard.requireLoggedIn(currentUserProvider.current()).memberId();
    }

    /** 087: 그룹은 친구 화면 아래로 합쳤다. 예전 주소는 그 자리로. */
    @GetMapping("/settings/groups")
    public RedirectView page() {
        return seeOther(BACK);
    }

    @PostMapping("/settings/groups")
    public RedirectView create(@RequestParam("name") String name) {
        groups.create(me(), name);
        return seeOther(BACK);
    }

    @PostMapping("/settings/groups/{id}/delete")
    public RedirectView delete(@PathVariable("id") long id) {
        groups.delete(me(), id);
        return seeOther(BACK);
    }

    @PostMapping("/settings/groups/{id}/members")
    public RedirectView add(@PathVariable("id") long id, @RequestParam("memberId") long memberId) {
        groups.add(me(), id, memberId);
        return seeOther(BACK);
    }

    @PostMapping("/settings/groups/{id}/members/{memberId}/remove")
    public RedirectView remove(@PathVariable("id") long id, @PathVariable("memberId") long memberId) {
        groups.remove(me(), id, memberId);
        return seeOther(BACK);
    }

    /** 그룹 공개 글이 보일 그룹 고르기(글쓴이만). */
    @PostMapping("/posts/{id}/groups")
    public RedirectView postGroups(@PathVariable("id") long id,
                                   @RequestParam(value = "groupId", required = false) List<Long> groupIds) {
        long me = me();
        groups.setPostGroups(me, id, groupIds);
        String handle = jdbc.queryForObject("SELECT handle FROM member WHERE id = ?", String.class, me);
        return seeOther(com.team.blog.shared.web.BlogPaths.of(handle) + "/posts/" + id + "#post-groups");
    }
}
