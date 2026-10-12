package com.team.blog.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 개인정보 처리방침(016 FR-018, 021 FR-035). 누구나 볼 수 있는 정적 화면. */
@Controller
public class PrivacyController {

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }

    /** 이용약관(가입 동의 대상, 강성찬 개인 확장 홈 리뉴얼의 꼬리말 링크). */
    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }
}
