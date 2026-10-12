package com.team.blog.discovery.web;

import com.team.blog.discovery.application.RssProperties;
import com.team.blog.guestbook.application.GuestbookProperties;
import com.team.blog.schedule.application.ScheduleProperties;
import com.team.blog.series.application.SeriesProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** 블로그 머리의 방명록(040)·시리즈(041)·RSS(044) 링크, 글쓰기의 예약 발행(042)을 켤지(강성찬 개인 확장). */
@ControllerAdvice
public class BlogExtrasAdvice {

    private final GuestbookProperties guestbook;
    private final SeriesProperties series;
    private final RssProperties rss;
    private final ScheduleProperties schedule;

    public BlogExtrasAdvice(GuestbookProperties guestbook, SeriesProperties series, RssProperties rss,
                            ScheduleProperties schedule) {
        this.guestbook = guestbook;
        this.series = series;
        this.rss = rss;
        this.schedule = schedule;
    }

    @ModelAttribute
    public void flags(Model model) {
        model.addAttribute("guestbookEnabled", guestbook.enabled());
        model.addAttribute("seriesEnabled", series.enabled());
        model.addAttribute("rssEnabled", rss.enabled());
        model.addAttribute("scheduleEnabled", schedule.enabled());
    }
}
