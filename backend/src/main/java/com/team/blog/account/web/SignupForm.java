package com.team.blog.account.web;

import com.team.blog.account.application.EmailSignupCommand;

/** 이메일 가입 폼(contracts/web-routes.md §1). 다시 그릴 때 비밀번호는 채우지 않는다. */
public record SignupForm(String email, String handle, String password, String passwordConfirm, String nickname,
                         Boolean agreeTerms, Boolean agreePrivacy, String ageGroup, String guardianName,
                         String guardianEmail) {

    public static SignupForm empty() {
        return new SignupForm(null, null, null, null, null, false, false, null, null, null);
    }

    EmailSignupCommand toCommand() {
        return new EmailSignupCommand(email, handle, password, passwordConfirm, nickname,
                Boolean.TRUE.equals(agreeTerms), Boolean.TRUE.equals(agreePrivacy), ageGroup, guardianName, guardianEmail);
    }

    /** 화면에 다시 채울 값(비밀번호 원문 제외). */
    SignupForm withoutPasswords() {
        return new SignupForm(email, handle, null, null, nickname,
                Boolean.TRUE.equals(agreeTerms), Boolean.TRUE.equals(agreePrivacy), ageGroup, guardianName, guardianEmail);
    }

    /** 10-21 chan: 14세 미만을 골랐는지(화면에서 보호자 칸을 펼침). */
    public boolean under14() {
        return com.team.blog.account.application.AgeGroup.UNDER14.equals(ageGroup);
    }

    @Override
    public String toString() {
        return "SignupForm[handle=" + handle + "]";
    }
}
