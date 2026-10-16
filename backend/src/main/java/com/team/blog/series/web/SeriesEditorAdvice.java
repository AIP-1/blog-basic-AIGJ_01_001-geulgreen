package com.team.blog.series.web;

import com.team.blog.post.web.EditorController;
import com.team.blog.series.application.SeriesService;
import com.team.blog.shared.security.CurrentUser;
import com.team.blog.shared.security.CurrentUserProvider;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

/** 106 강성찬 개인 확장: 글쓰기 발행 설정의 시리즈 칸(내 시리즈 목록·이 글이 든 시리즈). */
@ControllerAdvice(assignableTypes = EditorController.class)
public class SeriesEditorAdvice {

    private final SeriesService series;
    private final CurrentUserProvider currentUserProvider;

    public SeriesEditorAdvice(SeriesService series, CurrentUserProvider currentUserProvider) {
        this.series = series;
        this.currentUserProvider = currentUserProvider;
    }

    @ModelAttribute
    public void seriesField(@PathVariable(value = "postId", required = false) Long postId, Model model) {
        if (postId == null || !series.enabled()) {
            return;
        }
        currentUserProvider.current().map(CurrentUser::memberId).ifPresent(me -> {
            model.addAttribute("editorSeries", series.list(me, true));
            model.addAttribute("editorSeriesId", series.ofPost(postId).map(SeriesService.Series::id).orElse(null));
        });
    }
}
