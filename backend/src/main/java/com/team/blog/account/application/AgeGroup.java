package com.team.blog.account.application;

/** 10-21 chan: 가입할 때 고르는 나이. 14세 미만은 보호자 동의를 받아야 계정이 열린다(개인정보 보호법 제22조의2). */
public final class AgeGroup {

    public static final String OVER14 = "OVER14";
    public static final String UNDER14 = "UNDER14";

    private AgeGroup() {
    }

    public static boolean chosen(String value) {
        return OVER14.equals(value) || UNDER14.equals(value);
    }
}
