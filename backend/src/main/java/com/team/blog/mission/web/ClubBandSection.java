package com.team.blog.mission.web;

import com.team.blog.mission.application.ClubService;
import com.team.blog.mission.application.MissionService;
import com.team.blog.post.application.CardDates;
import com.team.blog.post.application.PostDetail;
import com.team.blog.post.web.PostDetailSection;
import com.team.blog.shared.security.CurrentUser;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * 10-26 글 화면(강성찬 개인 확장): 함께 쓰기 방 글감으로 쓴 공개 글이면 제목 위에 "○○에서 함께 쓴 글" 띠와 [방 보기],
 * 본문 아래에 같은 글감으로 쓴 친구 글(화살표 줄). 비공개방이면 방 멤버에게만.
 */
@Component
public class ClubBandSection implements PostDetailSection {

    private final MissionService missions;
    private final ClubService clubs;
    private final Clock clock;

    public ClubBandSection(MissionService missions, ClubService clubs, Clock clock) {
        this.missions = missions;
        this.clubs = clubs;
        this.clock = clock;
    }

    /** 친구 글 한 줄: 화면에 쓸 시간 글자("2시간 전")를 붙인다. */
    public record Other(ClubService.BandPost post, String time) {
    }

    @Override
    public void contribute(Model model, PostDetail post, Optional<CurrentUser> viewer, Map<String, String> params) {
        if (!missions.enabled() || !post.isPublic()) {
            return;
        }
        clubs.band(post.id(), post.authorId(), viewer.map(CurrentUser::memberId)).ifPresent(band -> {
            model.addAttribute("clubBand", band);
            model.addAttribute("clubBandOthers", band.others().stream()
                    .map(p -> new Other(p, CardDates.label(p.at(), clock.instant()))).toList());
        });
    }
}
