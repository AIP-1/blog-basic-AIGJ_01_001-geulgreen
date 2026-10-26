package com.team.blog.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 099 내 문의(강성찬 개인 확장): 본인 문의만 보고, 답장이 오면 알림이 그 문의로 간다. */
class MyInquiriesIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    private long member(String handle) {
        return members.localMember(handle, handle, handle + "@example.com", "Blog#2026ok", true);
    }

    private String page(String url, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get(url).with(who)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private long send(long who, String title) throws Exception {
        mockMvc.perform(post("/contact").param("kind", "BUG").param("title", title).param("content", "글이 안 올라가요")
                .with(csrf()).with(TestAuth.member(who)).with(r -> {
                    r.setRemoteAddr("198.51.100." + who % 200);
                    return r;
                })).andExpect(status().is3xxRedirection());
        return jdbc.queryForObject("SELECT id FROM inquiry WHERE title = ?", Long.class, title);
    }

    @Test
    void ownerSeesListDetailAndReplyNotice() throws Exception {
        long admin = member("myinqboss");
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", admin);
        long me = member("myinqme");
        long other = member("myinqother");

        assertThat(page("/contact/mine", TestAuth.member(me))).contains("아직 보낸 문의가 없어요").contains("내 문의");
        long id = send(me, "내 버그 문의");
        send(other, "남의 문의");

        String list = page("/contact/mine", TestAuth.member(me));
        assertThat(list).contains("내 버그 문의").contains("답변 대기").doesNotContain("남의 문의");
        assertThat(page("/contact/mine/" + id, TestAuth.member(me))).contains("글이 안 올라가요").contains("답장이 오면 알림과 메일로 알려 드려요");
        // 남의 문의는 404, 비회원은 로그인으로
        mockMvc.perform(get("/contact/mine/{id}", id).with(TestAuth.member(other))).andExpect(status().isNotFound());
        mockMvc.perform(get("/contact/mine")).andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/inquiries/{id}/reply", id).param("reply", "고쳤어요, 다시 올려 보세요").with(csrf())
                .with(TestAuth.admin(admin))).andExpect(status().is3xxRedirection());
        assertThat(page("/contact/mine", TestAuth.member(me))).contains("답변 완료");
        assertThat(page("/contact/mine/" + id, TestAuth.member(me))).contains("고쳤어요, 다시 올려 보세요");

        // 답장 알림은 보낸 회원에게만, 그 문의로 간다
        assertThat(page("/api/notifications", TestAuth.member(me))).contains("보내신 문의에 답장이 왔어요")
                .contains("/contact/mine/" + id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE type = 'INQUIRY_REPLIED'", Integer.class))
                .isEqualTo(1);
    }

    /** 10-26 문의 대화: 회원이 내 문의에서 다시 답하면 새 문의로 돌아가 관리자에게 알림, 관리자는 또 답장한다. */
    @Test
    void ownerAndAdminTalkInThread() throws Exception {
        long admin = member("thrboss");
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", admin);
        long me = member("thrme");
        long other = member("throther");
        long id = send(me, "대화 문의");

        mockMvc.perform(post("/admin/inquiries/{id}/reply", id).param("reply", "첫 답장이에요").with(csrf())
                .with(TestAuth.admin(admin))).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/contact/mine/{id}/reply", id).param("reply", "아직 안 돼요").with(csrf())
                .with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT status FROM inquiry WHERE id = ?", String.class, id)).isEqualTo("NEW");
        assertThat(page("/api/notifications", TestAuth.member(admin))).contains("다시 답했어요")
                .contains("/admin/inquiries/" + id);

        // 남의 문의엔 답할 수 없고, 빈 답장은 거절
        mockMvc.perform(post("/contact/mine/{id}/reply", id).param("reply", "끼어들기").with(csrf())
                .with(TestAuth.member(other))).andExpect(status().isNotFound());
        mockMvc.perform(post("/contact/mine/{id}/reply", id).param("reply", "  ").with(csrf())
                .with(TestAuth.member(me))).andExpect(status().isBadRequest());
        // csrf 없이는 안 된다
        mockMvc.perform(post("/contact/mine/{id}/reply", id).param("reply", "csrf 없음")
                .with(TestAuth.member(me))).andExpect(status().isForbidden());

        assertThat(page("/admin/inquiries/" + id, TestAuth.admin(admin))).contains("첫 답장이에요").contains("아직 안 돼요")
                .contains("답장 없이 처리 완료");
        mockMvc.perform(post("/admin/inquiries/{id}/reply", id).param("reply", "두 번째 답장이에요").with(csrf())
                .with(TestAuth.admin(admin))).andExpect(status().is3xxRedirection());

        String detail = page("/contact/mine/" + id, TestAuth.member(me));
        assertThat(detail).contains("첫 답장이에요").contains("아직 안 돼요").contains("두 번째 답장이에요").contains("답변 완료")
                .doesNotContain("끼어들기");
        assertThat(detail.indexOf("첫 답장이에요")).isLessThan(detail.indexOf("아직 안 돼요"));
        assertThat(detail.indexOf("아직 안 돼요")).isLessThan(detail.indexOf("두 번째 답장이에요"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inquiry_message WHERE inquiry_id = ?", Integer.class, id))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE type = 'INQUIRY_REPLIED' AND receiver_id = ?",
                Integer.class, me)).isEqualTo(2);

        // 고맙다는 답장처럼 더 답할 게 없으면 답장 없이 처리 완료
        mockMvc.perform(post("/contact/mine/{id}/reply", id).param("reply", "고마워요").with(csrf())
                .with(TestAuth.member(me))).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/inquiries/{id}/close", id).with(csrf()).with(TestAuth.admin(admin)))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT status FROM inquiry WHERE id = ?", String.class, id)).isEqualTo("DONE");
        // 관리자가 아니면 처리 완료도 못 한다(없는 주소와 같은 404)
        mockMvc.perform(post("/admin/inquiries/{id}/close", id).with(csrf()).with(TestAuth.member(me)))
                .andExpect(status().isNotFound());
    }
}
