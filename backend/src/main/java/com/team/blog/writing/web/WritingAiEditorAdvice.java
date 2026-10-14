package com.team.blog.writing.web;

import com.team.blog.post.web.EditorController;
import com.team.blog.writing.application.WritingAiService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 글쓰기 화면(059): 서식 창에 "AI로 고치기" 칸을 보일지, 085 글 점검을 쓸지. */
@ControllerAdvice(assignableTypes = EditorController.class)
public class WritingAiEditorAdvice {

    private final WritingAiService service;

    public WritingAiEditorAdvice(WritingAiService service) {
        this.service = service;
    }

    @ModelAttribute("writingAi")
    public boolean writingAi() {
        return service.available();
    }

    /** 085 글 점검(강성찬 개인 확장): 미리보기 대신 서식 추천·맞춤법 검사. */
    @ModelAttribute("writeCheck")
    public boolean writeCheck() {
        return service.checkEnabled();
    }
}
