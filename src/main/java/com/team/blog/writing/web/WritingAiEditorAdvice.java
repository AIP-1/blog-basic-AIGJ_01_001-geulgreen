package com.team.blog.writing.web;

import com.team.blog.post.web.EditorController;
import com.team.blog.writing.application.WritingAiService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 글쓰기 화면(059): 서식 창에 "AI로 고치기" 칸을 보일지. */
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
}
