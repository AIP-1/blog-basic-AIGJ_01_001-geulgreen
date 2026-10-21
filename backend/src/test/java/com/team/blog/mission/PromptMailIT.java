package com.team.blog.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.mission.application.PromptMailService;
import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** 10-21 오늘의 글감(강성찬 개인 확장): 글쓰기 화면 한 줄, 원하는 사람만 저녁 메일. */
class PromptMailIT extends IntegrationTestBase {

    /** 한국 시간 2026-10-21 20:00. */
    private static final Instant EVENING = Instant.parse("2026-10-21T11:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PromptMailService prompts;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private String page(String url, long who) throws Exception {
        return mockMvc.perform(get(url).with(TestAuth.member(who))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void editorShowsTodayTopicOnlyOnBlankDraft() throws Exception {
        clock.set(EVENING);
        long me = member("promptw");
        String title = prompts.todayFor(me).orElseThrow().title();
        long blank = posts.draft(me, "", "", 0);
        assertThat(page("/write/" + blank, me)).contains("id=\"topic-note\"").contains(title);
        long started = posts.draft(me, "쓰던 글", "", 0);
        assertThat(page("/write/" + started, me)).doesNotContain("id=\"topic-note\"");
    }

    @Test
    void settingIsOffByDefaultAndToggles() throws Exception {
        long me = member("promptset");
        assertThat(page("/settings", me)).contains("저녁 글감 메일");
        assertThat(prompts.subscribed(me)).isFalse();
        mockMvc.perform(post("/settings/prompt-mail").param("on", "on").with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(prompts.subscribed(me)).isTrue();
        mockMvc.perform(post("/settings/prompt-mail").with(csrf()).with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(prompts.subscribed(me)).isFalse();
    }

    @Test
    void eveningMailOnlyToSubscribersWhoHaveNotWrittenToday() throws Exception {
        clock.set(EVENING);
        long idle = member("promptidle");
        long wrote = member("promptwrote");
        long off = member("promptoff");
        long yesterday = member("promptyday");
        for (long m : new long[] {idle, wrote, yesterday}) {
            jdbc.update("INSERT INTO prompt_mail (member_id) VALUES (?)", m);
        }
        posts.published(wrote, "오늘 쓴 글", "본문", 1, Instant.parse("2026-10-21T01:00:00Z")); // 한국 10시
        posts.published(yesterday, "어제 쓴 글", "본문", 1, Instant.parse("2026-10-20T14:00:00Z")); // 한국 20일 23시
        assertThat(prompts.sendEvening()).isEqualTo(2);
        assertThat(prompts.sendEvening()).isZero(); // 하루 한 통
        var mail = mailpit.awaitMessagesTo("promptidle@example.com", 1).get(0);
        assertThat(mail.subject()).startsWith("[글그린] 오늘의 글감: ").contains(prompts.todayFor(idle).orElseThrow().title());
        assertThat(mail.fromName()).isEqualTo("글그린");
        assertThat(mailpit.awaitMessagesTo("promptyday@example.com", 1)).hasSize(1);
        assertThat(mailpit.countAfterQuietPeriod("promptwrote@example.com")).isZero();
        assertThat(mailpit.countAfterQuietPeriod("promptoff@example.com")).isZero();
    }
}
