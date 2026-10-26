package com.team.blog.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.team.blog.support.IntegrationTestBase;
import com.team.blog.support.TestAuth;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 111 후보 함께 쓰기: 만들기, 초대 링크로만 참여, 멤버만 봄, 나가기·내보내기·삭제.
 * 10-25 기간 없앰: 방이 열린 날부터 멤버들의 공개 글이 시간 순으로, "오늘 N명이 썼어요", 방 목록의 마지막 글·새 글 수, 방 글감.
 */
class ClubIT extends IntegrationTestBase {

    // 한국 시간 2026-10-16 12:00
    private static final Instant NOW = Instant.parse("2026-10-16T03:00:00Z");

    @Autowired
    JdbcTemplate jdbc;

    private String page(String url, long who) throws Exception {
        return mockMvc.perform(get(url).with(TestAuth.member(who))).andReturn().getResponse().getContentAsString();
    }

    @Test
    void inviteOnlyRoomShowsMembersPostsSinceItOpened() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("clubowner", "모임장", "clubowner@example.com", "Blog#2026ok", true);
        long friend = members.localMember("clubfriend", "반친구", "clubfriend@example.com", "Blog#2026ok", true);
        long stranger = members.localMember("clubstranger", "남남", "clubstranger@example.com", "Blog#2026ok", true);
        // 이름이 비면 만들지 않고 만들기 화면으로
        assertThat(mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner)).param("name", " "))
                .andReturn().getResponse().getRedirectedUrl()).isEqualTo("/missions/clubs");
        // 기간·규칙 없이 이름만으로 만든다(예전 칸은 자리값)
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner))
                .param("name", "3반 모임").param("intro", "같이 써요").param("joinMode", "LINK")).andReturn().getResponse().getRedirectedUrl();
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        assertThat(jdbc.queryForObject("SELECT start_date::text || ' ' || end_date::text FROM writing_club WHERE id = ?", String.class, id))
                .isEqualTo("2026-10-16 9999-12-31");
        String code = jdbc.queryForObject("SELECT invite_code FROM writing_club WHERE id = ?", String.class, id);
        // 멤버가 아니면 없는 방
        assertThat(mockMvc.perform(get(location).with(TestAuth.member(stranger))).andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(page("/missions/clubs/join/" + code, friend)).contains("3반 모임").contains("참여하기").doesNotContain("일에 1편");
        mockMvc.perform(post("/missions/clubs/join/" + code).with(csrf()).with(TestAuth.member(friend)));
        // 방이 열린 날(10-16)부터의 공개 글만: 친구의 오늘 글은 보이고, 모임장의 지난주 글은 안 보임
        posts.published(friend, "이번 주 글", "본문", 1, NOW.plus(Duration.ofMinutes(30)));
        posts.published(owner, "지난주 글", "본문", 1, NOW.minus(Duration.ofDays(6)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        // 방 목록: 마지막 글과, 모임장이 마지막으로 본 뒤 올라온 남의 글 수
        assertThat(page("/missions/clubs", owner)).contains("3반 모임").contains("반친구: 이번 주 글").contains("aria-label=\"새 글 1편\"")
                .contains("aria-current=\"page\">함께 쓰기</a>").doesNotContain("이번 기간");
        String board = page(location, friend);
        assertThat(board).contains("오늘 <strong>1명</strong>이 썼어요").contains("이번 주 글").doesNotContain("지난주 글")
                .contains("반친구님이 들어왔어요").contains("/missions/clubs/join/" + code).contains("함께 쓰기에서 나가기")
                .doesNotContain("함께 쓰기 삭제").doesNotContain("이번 기간").doesNotContain("번째");
        // 멤버 줄: 마지막으로 쓴 때(친구 오늘, 모임장 6일 전)
        assertThat(board).containsPattern("(?s)반친구</span>.*?</a>\\s*<span class=\"room-state is-today\">오늘 씀</span>")
                .containsPattern("(?s)모임장</span>.*?</a>\\s*<span class=\"room-state\">6일 전</span>");
        // 모임장이 방을 보면 새 글 수가 사라진다
        assertThat(page(location, owner)).contains("함께 쓰기 삭제").contains("내보내기");
        assertThat(page("/missions/clubs", owner)).doesNotContain("새 글 1편");
        // 만든 사람: 내보내기, 삭제
        mockMvc.perform(post(location + "/members/" + friend + "/remove").with(csrf()).with(TestAuth.member(owner)));
        assertThat(mockMvc.perform(get(location).with(TestAuth.member(friend))).andReturn().getResponse().getStatus()).isEqualTo(404);
        // 남이 지우려 하면 404, 방은 남는다
        mockMvc.perform(post(location + "/delete").with(csrf()).with(TestAuth.member(stranger)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club", Integer.class)).isEqualTo(1);
        mockMvc.perform(post(location + "/delete").with(csrf()).with(TestAuth.member(owner)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_member", Integer.class)).isZero();
    }

    @Test
    void roomTopicPinnedWrittenAndReplaced() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("topicowner", "글감방장", "topicowner@example.com", "Blog#2026ok", true);
        long mate = members.localMember("topicmate", "글감친구", "topicmate@example.com", "Blog#2026ok", true);
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner))
                .param("name", "글감 방").param("joinMode", "LINK")).andReturn().getResponse().getRedirectedUrl();
        long club = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?)", club, mate);
        // 글감이 없으면 멤버에게는 글감 줄이 없고, 관리자에게는 올리기 안내
        assertThat(page(location, mate)).doesNotContain("room-pin");
        assertThat(page(location, owner)).contains("글감을 올려 보세요");
        // 관리자가 아니면 못 올린다
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(mate)).param("title", "몰래 글감"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_topic", Integer.class)).isZero();
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(owner)).param("title", "내 책상 위 풍경")
                .param("hint", "사진 한 장과 함께"));
        long topic = jdbc.queryForObject("SELECT id FROM writing_club_topic WHERE club_id = ?", Long.class, club);
        assertThat(page(location, mate)).contains("방 글감").contains("내 책상 위 풍경").contains("· 0명 썼어요").contains("사진 한 장과 함께")
                .contains("글감방장님이 방 글감 「내 책상 위 풍경」을 올렸어요").doesNotContain("글감 내리기");
        // [이 글감으로 쓰기]: 글감 제목으로 임시글, 다시 누르면 같은 임시글
        String write = mockMvc.perform(post(location + "/topics/" + topic + "/write").with(csrf()).with(TestAuth.member(mate)))
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(mockMvc.perform(post(location + "/topics/" + topic + "/write").with(csrf()).with(TestAuth.member(mate)))
                .andReturn().getResponse().getRedirectedUrl()).isEqualTo(write);
        long postId = Long.parseLong(write.substring(write.lastIndexOf('/') + 1));
        assertThat(jdbc.queryForObject("SELECT title FROM post WHERE id = ?", String.class, postId)).isEqualTo("내 책상 위 풍경");
        assertThat(page(write, mate)).contains("방 글감: 내 책상 위 풍경").contains("글감 방 방에 올라가요").doesNotContain("topic-note-pick");
        // 멤버가 아니면 그 글감으로 못 쓴다
        long stranger = members.localMember("topicstranger", "글감남남", "topicstranger@example.com", "Blog#2026ok", true);
        assertThat(mockMvc.perform(post(location + "/topics/" + topic + "/write").with(csrf()).with(TestAuth.member(stranger)))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
        // 발행하면 방에 "방 글감" 표시, 쓴 사람 수
        posts.publish(postId, NOW.plus(Duration.ofMinutes(10)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        assertThat(page(location, owner)).contains("class=\"room-chip\"").contains("· 1명 썼어요").contains("글감 내리기");
        assertThat(page("/missions/clubs", owner)).contains("방 글감 · 내 책상 위 풍경");
        // 새 글감을 올리면 이전 것은 지난 글감
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(owner)).param("title", "요즘 듣는 노래"));
        assertThat(page(location, mate)).contains("요즘 듣는 노래").contains("class=\"room-past\"").containsPattern("room-past-title\">내 책상 위 풍경");
        // 글감을 지워도 글은 남는다(표시만 사라짐)
        mockMvc.perform(post(location + "/topics/" + topic + "/delete").with(csrf()).with(TestAuth.member(owner)));
        assertThat(page(location, mate)).doesNotContain("class=\"room-past\"").doesNotContain("class=\"room-chip\"").contains("내 책상 위 풍경");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ?", Integer.class, postId)).isEqualTo(1);
    }

    @Test
    void oldRoomsWithEndedPeriodOpenAgain() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("oldowner", "옛방장", "oldowner@example.com", "Blog#2026ok", true);
        long guest = members.localMember("oldguest", "옛손님", "oldguest@example.com", "Blog#2026ok", true);
        // 예전에 만든 방: 기간이 이미 끝났다
        long club = jdbc.queryForObject("""
                INSERT INTO writing_club (owner_id, name, every_days, start_date, end_date, invite_code, join_mode)
                VALUES (?, '지난 방', 3, '2026-09-01', '2026-09-30', 'oldroomcode000000000000000000001', 'OPEN') RETURNING id
                """, Long.class, owner);
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?)", club, owner);
        assertThat(page("/missions/clubs/find?q=지난", guest)).contains("지난 방");
        mockMvc.perform(post("/missions/clubs/" + club + "/join").with(csrf()).with(TestAuth.member(guest)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_member WHERE club_id = ?", Integer.class, club)).isEqualTo(2);
        posts.published(guest, "다시 연 방의 글", "본문", 1, NOW.plus(Duration.ofMinutes(5)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        assertThat(page("/missions/clubs/" + club, owner)).contains("다시 연 방의 글").doesNotContain("끝났어요").doesNotContain("끝난");
    }
}
