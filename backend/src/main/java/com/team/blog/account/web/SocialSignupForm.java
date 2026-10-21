package com.team.blog.account.web;

import com.team.blog.account.application.SocialSignupCommand;

/** 소셜 가입 마무리 폼. {@code handle}은 본문만(접두어는 고정 글자). */
public record SocialSignupForm(String nickname, String handle, Boolean agreeTerms, Boolean agreePrivacy,
                               Boolean useSocialPicture, String email, String ageGroup, String guardianName,
                               String guardianEmail) {

    /** 10-21 chan: 14세 미만을 골랐는지(화면에서 보호자 칸을 펼침). */
    public boolean under14() {
        return com.team.blog.account.application.AgeGroup.UNDER14.equals(ageGroup);
    }

    SocialSignupCommand toCommand() {
        return new SocialSignupCommand(nickname, handle, Boolean.TRUE.equals(agreeTerms),
                Boolean.TRUE.equals(agreePrivacy), Boolean.TRUE.equals(useSocialPicture), email, ageGroup, guardianName, guardianEmail);
    }
}
