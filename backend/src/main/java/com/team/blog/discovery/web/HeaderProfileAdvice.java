package com.team.blog.discovery.web;

import com.team.blog.account.application.ProfileAvatar;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 홈 리뉴얼(강성찬 개인 확장): 머리말 내 프로필 메뉴의 사진·주소, 꼬리말 [문의하기] 메일·문의 양식(090). */
@ControllerAdvice(annotations = Controller.class)
public class HeaderProfileAdvice {

    private final CurrentUserProvider currentUserProvider;
    private final JdbcTemplate jdbc;
    private final String contactEmail;
    private final boolean contactPage;
    private final boolean inviteEnabled;
    private final com.team.blog.post.application.TagListingQuery tagListingQuery;

    public HeaderProfileAdvice(CurrentUserProvider currentUserProvider, JdbcTemplate jdbc,
                               com.team.blog.post.application.TagListingQuery tagListingQuery,
                               @org.springframework.beans.factory.annotation.Value("${blog.contact.enabled:true}") boolean contactPage,
                               @org.springframework.beans.factory.annotation.Value("${blog.contact.email:blogeulgreen@gmail.com}") String contactEmail,
                               @org.springframework.beans.factory.annotation.Value("${blog.invite.enabled:true}") boolean inviteEnabled) {
        this.inviteEnabled = inviteEnabled;
        this.contactPage = contactPage;
        this.currentUserProvider = currentUserProvider;
        this.tagListingQuery = tagListingQuery;
        this.jdbc = jdbc;
        // 090: 사이트 문의 주소만 쓴다. 관리자 지정 주소(blog.admin.emails)는 개인 주소라 화면에 내보내지 않는다.
        this.contactEmail = contactEmail.isBlank() ? null : contactEmail.strip();
    }

    /** 10-25 chan: 친구 초대 켜짐 여부(프로필 메뉴 [친구 초대], 함께 쓰기 방 초대 칸 링크). */
    @ModelAttribute("inviteEnabled")
    public boolean inviteEnabled() {
        return inviteEnabled;
    }

    @ModelAttribute("currentAvatar")
    public ProfileAvatar currentAvatar() {
        return currentUserProvider.current().map(CurrentUser::memberId).flatMap(id -> jdbc.query(
                "SELECT handle, nickname, profile_image_url FROM member WHERE id = ?",
                (rs, i) -> new ProfileAvatar(rs.getString("handle"), rs.getString("nickname"), rs.getString("profile_image_url")),
                id).stream().findFirst()).orElse(null);
    }

    /** 090: 켜면 꼬리말 [문의하기]가 메일 대신 사이트 문의 양식(/contact)으로 간다. */
    @ModelAttribute("contactPage")
    public boolean contactPage() {
        return contactPage;
    }

    @ModelAttribute("contactEmail")
    public String contactEmail() {
        return contactEmail;
    }

    /** 078 강성찬 개인 확장: 머리말 검색창 아래 추천 태그(홈 인기 태그와 같은 10분 캐시 순위). */
    @ModelAttribute("headerTags")
    public java.util.List<com.team.blog.post.application.TagListingQuery.TagCount> headerTags() {
        return tagListingQuery.top(10);
    }
}
