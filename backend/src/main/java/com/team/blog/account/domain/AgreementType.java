package com.team.blog.account.domain;

/** 회원 동의 종류. {@code ck_member_agreement_type} 값과 같다. 가입은 {@code TERMS}·{@code PRIVACY}(필수 2개). */
public enum AgreementType {
    TERMS,
    PRIVACY,
    AI,
    /** 가입할 때 "만 14세 이상이에요"를 고름(10-21 chan). */
    AGE14,
    /** 14세 미만 회원의 보호자(법정대리인)가 동의 메일에서 동의함(10-21 chan). */
    GUARDIAN
}
