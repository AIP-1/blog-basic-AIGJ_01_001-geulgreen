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
        assertThat(mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(owner)).param("name", " "))
                .andReturn().getResponse().getRedirectedUrl()).isEqualTo("/missions/clubs");
        // 기간·규칙 없이 이름만으로 만든다(예전 칸은 자리값)
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(owner))
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
        // 10-28 글쓴이가 고른 방에만: 친구는 이번 주 글을 방에 올리고, 다른 글은 안 올린다. 모임장의 지난주 글도 안 올림
        long weekPost = posts.published(friend, "이번 주 글", "본문", 1, NOW.plus(Duration.ofMinutes(30)));
        posts.published(friend, "방에 안 올린 글", "본문", 1, NOW.plus(Duration.ofMinutes(40)));
        posts.published(owner, "지난주 글", "본문", 1, NOW.minus(Duration.ofDays(6)));
        mockMvc.perform(post("/posts/" + weekPost + "/clubs/" + id).with(csrf()).with(TestAuth.member(friend)));
        // 남의 글이나 멤버가 아닌 방에는 못 올린다
        assertThat(mockMvc.perform(post("/posts/" + weekPost + "/clubs/" + id).with(csrf()).with(TestAuth.member(owner)))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
        clock.set(NOW.plus(Duration.ofHours(1)));
        // 방 목록: 마지막 글과, 모임장이 마지막으로 본 뒤 올라온 남의 글 수
        assertThat(page("/missions/clubs", owner)).contains("3반 모임").contains("반친구: 이번 주 글").contains("aria-label=\"새 글 1편\"")
                .contains("aria-current=\"page\">함께 쓰기</a>").doesNotContain("이번 기간");
        String board = page(location, friend);
        assertThat(board).contains("오늘 <strong>1명</strong>이 썼어요").contains("이번 주 글").doesNotContain("지난주 글").doesNotContain("방에 안 올린 글")
                .contains("반친구님이 들어왔어요").contains("/i/").contains("/" + code).contains("링크 복사").contains("함께 쓰기에서 나가기")
                .doesNotContain("함께 쓰기 삭제").doesNotContain("이번 기간").doesNotContain("번째");
        // 멤버 줄: 이 방에 마지막으로 올린 때(친구 오늘, 모임장은 올린 글이 없음)
        assertThat(board).containsPattern("(?s)반친구</span>.*?</a>\\s*<span class=\"room-state is-today\">오늘 씀</span>")
                .containsPattern("(?s)모임장</span>.*?</a>\\s*<span class=\"room-state\">아직 없어요</span>");
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
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(owner))
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
        // 10-26 먼저 쓰고 보기: 방장은 아직 이 글감으로 안 써서 친구 글이 흐리게(글감 줄은 "내가 쓰면 열려요")
        assertThat(page(location, owner)).contains("class=\"room-chip\"").contains("1명 썼어요 · 내가 쓰면 열려요").contains("글감 내리기")
                .contains("room-bubble is-locked");
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
    void writeFirstLocksTopicPostsUntilIWriteOrADayPasses() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("firstowner", "먼저방장", "firstowner@example.com", "Blog#2026ok", true);
        long mate = members.localMember("firstmate", "먼저친구", "firstmate@example.com", "Blog#2026ok", true);
        long outsider = members.localMember("firstout", "먼저남남", "firstout@example.com", "Blog#2026ok", true);
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(owner))
                .param("name", "먼저 방").param("joinMode", "LINK")).andReturn().getResponse().getRedirectedUrl();
        long club = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?)", club, mate);
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(owner)).param("title", "요즘 듣는 노래"));
        long topic = jdbc.queryForObject("SELECT id FROM writing_club_topic WHERE club_id = ?", Long.class, club);
        String write = mockMvc.perform(post(location + "/topics/" + topic + "/write").with(csrf()).with(TestAuth.member(mate)))
                .andReturn().getResponse().getRedirectedUrl();
        long mates = Long.parseLong(write.substring(write.lastIndexOf('/') + 1));
        posts.publish(mates, NOW.plus(Duration.ofMinutes(10)));
        long free = posts.published(mate, "자유롭게 쓴 글", "본문", 1, NOW.plus(Duration.ofMinutes(20)));
        mockMvc.perform(post("/posts/" + free + "/clubs/" + club).with(csrf()).with(TestAuth.member(mate)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        // 방장은 아직 안 써서 친구의 글감 글은 제목 없이 흐린 자리표시, 자유 글은 그대로, [쓰기]는 강조
        String board = page(location, owner);
        assertThat(board).contains("room-bubble is-locked").contains("내가 이 글감으로 쓰면 열려요").contains("자유롭게 쓴 글")
                .doesNotContain("/posts/" + mates + "\"").contains("btn btn-sm btn-primary");
        // 쓴 사람 자신에게는 잠기지 않는다
        assertThat(page(location, mate)).doesNotContain("room-bubble is-locked").contains("나도 썼어요 · 1명이 썼어요");
        // 글 화면은 언제나 열려 있고, 위에 방 띠(비공개방이라 멤버만)
        String postUrl = "/firstmate/posts/" + mates;
        assertThat(page(postUrl, owner)).contains("먼저 방에서 함께 쓴 글").contains("방 글감 「요즘 듣는 노래」").contains("방 보기");
        assertThat(page(postUrl, outsider)).doesNotContain("club-band");
        // 방장도 그 글감으로 쓰면 열리고, 글 화면 아래에 같은 글감 친구 글
        String mine = mockMvc.perform(post(location + "/topics/" + topic + "/write").with(csrf()).with(TestAuth.member(owner)))
                .andReturn().getResponse().getRedirectedUrl();
        long ownerPost = Long.parseLong(mine.substring(mine.lastIndexOf('/') + 1));
        posts.publish(ownerPost, NOW.plus(Duration.ofMinutes(70)));
        clock.set(NOW.plus(Duration.ofHours(2)));
        assertThat(page(location, owner)).doesNotContain("room-bubble is-locked").contains("/posts/" + mates + "\"")
                .contains("나도 썼어요 · 2명이 썼어요");
        assertThat(page(postUrl, owner)).contains("같은 글감으로 쓴 친구 글 1편").contains("/posts/" + ownerPost);
        // 공개방이면 멤버가 아니어도 띠가 보인다
        mockMvc.perform(post(location + "/settings").with(csrf()).with(TestAuth.member(owner))
                .param("joinMode", "APPROVAL").param("memberCap", "30").param("writeFirst", "true"));
        assertThat(page(postUrl, outsider)).contains("먼저 방에서 함께 쓴 글");
        // 새 글감: 하루가 지나면 안 쓴 사람에게도 열린다
        mockMvc.perform(post(location + "/topics").with(csrf()).with(TestAuth.member(owner)).param("title", "내 책상"));
        long topic2 = jdbc.queryForObject("SELECT max(id) FROM writing_club_topic WHERE club_id = ?", Long.class, club);
        String w2 = mockMvc.perform(post(location + "/topics/" + topic2 + "/write").with(csrf()).with(TestAuth.member(mate)))
                .andReturn().getResponse().getRedirectedUrl();
        posts.publish(Long.parseLong(w2.substring(w2.lastIndexOf('/') + 1)), NOW.plus(Duration.ofHours(3)));
        clock.set(NOW.plus(Duration.ofHours(4)));
        assertThat(page(location, owner)).contains("room-bubble is-locked");
        clock.set(NOW.plus(Duration.ofHours(27)));
        assertThat(page(location, owner)).doesNotContain("room-bubble is-locked");
        // 방장이 끄면(체크 해제) 잠금이 없다
        clock.set(NOW.plus(Duration.ofHours(4)));
        mockMvc.perform(post(location + "/settings").with(csrf()).with(TestAuth.member(owner))
                .param("joinMode", "APPROVAL").param("memberCap", "30"));
        assertThat(jdbc.queryForObject("SELECT write_first FROM writing_club WHERE id = ?", Boolean.class, club)).isFalse();
        assertThat(page(location, owner)).doesNotContain("room-bubble is-locked");
    }

    @Test
    void emptyScreenLeadsIntoOfficialRoomWithOneClick() throws Exception {
        clock.set(NOW);
        long admin = members.localMember("officialadmin", "운영자", "officialadmin@example.com", "Blog#2026ok", true);
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE id = ?", admin);
        long host = members.localMember("openhost", "열린방장", "openhost@example.com", "Blog#2026ok", true);
        long newbie = members.localMember("newbie", "새싹", "newbie@example.com", "Blog#2026ok", true);
        // 공식 방이 없으면 열린 방과 [방 만들기]만
        String open = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(host))
                .param("name", "고양이 일기").param("joinMode", "OPEN")).andReturn().getResponse().getRedirectedUrl();
        assertThat(page("/missions/clubs", newbie)).contains("친구들과 같이 쓰면 꾸준해져요").contains("고양이 일기").doesNotContain("club-official");
        // 운영자가 만든 공개방(승인 방식)이 공식 방: 맨 위 카드, [들어가기] 한 번에 승인 없이 바로 들어온다
        String official = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(admin))
                .param("name", "글그린").param("intro", "모두 함께").param("joinMode", "APPROVAL")).andReturn().getResponse().getRedirectedUrl();
        String empty = page("/missions/clubs", newbie);
        assertThat(empty).contains("club-official").contains("글그린").contains(">공식<").contains("action=\"" + official + "/join\"")
                .contains("고양이 일기");
        mockMvc.perform(post(official + "/join").with(csrf()).with(TestAuth.member(newbie)));
        long officialId = Long.parseLong(official.substring(official.lastIndexOf('/') + 1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_member WHERE club_id = ? AND member_id = ?", Integer.class,
                officialId, newbie)).isEqualTo(1);
        // 방이 생기면 첫 화면 대신 방 목록
        assertThat(page("/missions/clubs", newbie)).doesNotContain("club-official").contains("참여 중인 방");
        // 운영자가 아닌 사람의 승인 방은 그대로 요청
        String approval = mockMvc.perform(post("/missions/clubs").with(csrf()).param("subject", "자유 주제").with(TestAuth.member(host))
                .param("name", "책 한 줄").param("joinMode", "APPROVAL")).andReturn().getResponse().getRedirectedUrl();
        mockMvc.perform(post(approval + "/join").with(csrf()).with(TestAuth.member(newbie)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_member m JOIN writing_club c ON c.id = m.club_id WHERE c.name = '책 한 줄' AND m.member_id = ?",
                Integer.class, newbie)).isZero();
        assertThat(open).isNotNull();
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
        long again = posts.published(guest, "다시 연 방의 글", "본문", 1, NOW.plus(Duration.ofMinutes(5)));
        mockMvc.perform(post("/posts/" + again + "/clubs/" + club).with(csrf()).with(TestAuth.member(guest)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        assertThat(page("/missions/clubs/" + club, owner)).contains("다시 연 방의 글").doesNotContain("끝났어요").doesNotContain("끝난");
    }

    @Test
    void roomSubjectPickedRoomsAndTakeDown() throws Exception {
        clock.set(NOW);
        long owner = members.localMember("subjowner", "주제방장", "subjowner@example.com", "Blog#2026ok", true);
        long mate = members.localMember("subjmate", "주제친구", "subjmate@example.com", "Blog#2026ok", true);
        long outsider = members.localMember("subjout", "주제남남", "subjout@example.com", "Blog#2026ok", true);
        // 10-28 방 주제 없이는 만들지 않는다
        mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner)).param("name", "주제 없는 방").param("joinMode", "OPEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club", Integer.class)).isZero();
        String location = mockMvc.perform(post("/missions/clubs").with(csrf()).with(TestAuth.member(owner)).param("name", "마크 방")
                .param("subject", " 마인크래프트 이야기 ").param("joinMode", "OPEN")).andReturn().getResponse().getRedirectedUrl();
        long club = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        jdbc.update("INSERT INTO writing_club_member (club_id, member_id) VALUES (?, ?)", club, mate);
        assertThat(page(location, mate)).contains("class=\"room-subject\"").contains("마인크래프트 이야기").doesNotContain("방 주제 정하기");
        assertThat(page("/missions/clubs/find?q=마인크래프트", outsider)).contains("마크 방");
        // 올릴 방: 고르기 전에는 안 골라져 있고, 고르면 방에 보이고, 빼면 사라진다
        long postId = posts.published(mate, "벚꽃 집 지었어요", "본문", 1, NOW.plus(Duration.ofMinutes(5)));
        clock.set(NOW.plus(Duration.ofHours(1)));
        assertThat(page("/write/" + postId, mate)).contains("올릴 방").doesNotContain("checked=\"checked\">\n");
        assertThat(page(location, owner)).doesNotContain("벚꽃 집 지었어요");
        mockMvc.perform(post("/posts/" + postId + "/clubs/" + club).with(csrf()).with(TestAuth.member(mate)));
        assertThat(page(location, owner)).contains("벚꽃 집 지었어요").contains("방에서 내리기");
        assertThat(page(location, mate)).doesNotContain("방에서 내리기");
        // 글 화면 띠: 방 글감이 아니어도 "○○에 올린 글"과 방 주제
        assertThat(page("/subjmate/posts/" + postId, outsider)).contains("마크 방에 올린 글").contains("방 주제 「마인크래프트 이야기」");
        mockMvc.perform(post("/posts/" + postId + "/clubs/" + club).with(csrf()).with(TestAuth.member(mate)).param("on", "false"));
        assertThat(page(location, owner)).doesNotContain("벚꽃 집 지었어요");
        // 방장·관리자는 방에서 내린다(글은 그대로). 멤버는 못 내린다
        mockMvc.perform(post("/posts/" + postId + "/clubs/" + club).with(csrf()).with(TestAuth.member(mate)));
        assertThat(mockMvc.perform(post(location + "/posts/" + postId + "/remove").with(csrf()).with(TestAuth.member(mate)))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_post", Integer.class)).isEqualTo(1);
        mockMvc.perform(post(location + "/posts/" + postId + "/remove").with(csrf()).with(TestAuth.member(owner)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM writing_club_post", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM post WHERE id = ? AND deleted_at IS NULL", Integer.class, postId)).isEqualTo(1);
        assertThat(page("/subjmate/posts/" + postId, outsider)).doesNotContain("club-band");
        // 방장만 주제를 바꾼다
        mockMvc.perform(post(location + "/subject").with(csrf()).with(TestAuth.member(mate)).param("subject", "몰래 바꿈"));
        mockMvc.perform(post(location + "/subject").with(csrf()).with(TestAuth.member(owner)).param("subject", "마크 건축 자랑"));
        assertThat(jdbc.queryForObject("SELECT subject FROM writing_club WHERE id = ?", String.class, club)).isEqualTo("마크 건축 자랑");
        // 주제가 빈 예전 방: 방장에게 [방 주제 정하기]
        jdbc.update("UPDATE writing_club SET subject = '' WHERE id = ?", club);
        assertThat(page(location, owner)).contains("방 주제 정하기").doesNotContain("class=\"room-subject\"");
    }
}
