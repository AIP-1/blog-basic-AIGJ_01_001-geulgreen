package com.team.blog.account.application;

/** 10-21 chan: 14세 미만인데 보호자 이름·이메일이 없거나 잘못됨. 메시지는 화면에 그대로 보인다. */
public class GuardianInfoException extends RuntimeException {

    public GuardianInfoException(String message) {
        super(message);
    }
}
