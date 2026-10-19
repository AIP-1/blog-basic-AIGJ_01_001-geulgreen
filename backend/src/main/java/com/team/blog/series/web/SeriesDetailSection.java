package com.team.blog.series.web;

import com.team.blog.post.application.PostDetail;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.series.application.SeriesService;
import com.team.blog.shared.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** 글 화면(041): 시리즈 이름·몇 번째·목록·이전편/다음편(누구나), 글쓴이에게는 시리즈 넣기·빼기. */
@Component
public class SeriesDetailSection implements PostDetailSection {

    private final SeriesService series;

    public SeriesDetailSection(SeriesService series) {
        this.series = series;
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        if (!series.enabled()) {
            return;
        }
        // 113: 미션 시리즈는 글 화면에 상자를 그리지 않는다(시리즈 화면·시리즈 탭에는 그대로)
        series.ofPost(post.id()).filter(s -> !series.isMission(s.id())).ifPresent(s -> {
            List<SeriesService.Item> items = series.items(s.id());
            int index = -1;
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id() == post.id()) {
                    index = i;
                }
            }
            model.addAttribute("postSeries", s);
            model.addAttribute("seriesItems", items);
            model.addAttribute("seriesIndex", index);
            if (index > 0) {
                model.addAttribute("seriesPrev", items.get(index - 1));
            }
            if (index >= 0 && index < items.size() - 1) {
                model.addAttribute("seriesNext", items.get(index + 1));
            }
        });
        if (viewer.map(v -> v.memberId() == post.authorId()).orElse(false)) {
            model.addAttribute("mySeries", series.list(post.authorId(), true));
        }
    }
}
