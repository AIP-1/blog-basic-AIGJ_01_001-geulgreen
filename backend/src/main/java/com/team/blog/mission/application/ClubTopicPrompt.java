package com.team.blog.mission.application;

import java.util.List;

/**
 * 10-29 chan: 방장이 ChatGPT·Gemini·Claude에게 방 글감을 추천받는다. API 키를 맡기지 않고, 이 질문을 복사해 AI에 붙여 넣은 뒤
 * 받은 답을 다시 붙여 넣어 방장이 고친 것만 올린다. 방 주제를 보고 멤버들이 흥미를 가질 글감을, 지난 1년 글감과 겹치지 않게.
 * 답 형식은 오늘의 주제(TopicAi)와 같은 "글감|한 줄 설명" 줄.
 */
public final class ClubTopicPrompt {

    /** 한 번에 받을 글감 수. */
    public static final int COUNT = 5;

    private ClubTopicPrompt() {
    }

    public static String of(String name, String subject, String intro, List<String> yearTitles) {
        StringBuilder b = new StringBuilder();
        b.append("우리 글쓰기 모임 '").append(name).append("'에서 멤버들이 함께 쓸 글감 ").append(COUNT).append("개를 추천해 줘.\n");
        b.append("방 주제: ").append(subject.isBlank() ? "(아직 없어요. 방 이름을 보고 짐작해 줘)" : subject).append('\n');
        if (!intro.isBlank()) {
            b.append("방 소개: ").append(intro).append('\n');
        }
        b.append("""
                멤버들은 이 주제를 좋아해서 모였어. 주제 안에서 "이건 꼭 써 보고 싶다" 싶을 만큼 흥미로운 글감으로 골라 줘.
                규칙:
                - 초등학생·중학생도 쓸 수 있게 쉽고 밝은 글감으로(정치·종교·건강 진단·돈 투자·개인정보를 묻는 글감은 빼고).
                - 아래 '지난 1년 동안 올린 글감'과 같거나 비슷한 글감은 빼 줘.
                - 경험, 추천, 비교, 상상, 정리처럼 서로 다른 각도로.
                - 한 줄에 하나씩 "글감|한 줄 설명" 형식으로만 답해. 글감은 25자 이내, 설명은 60자 이내 한 문장. 번호·따옴표·마크다운 없이.
                지난 1년 동안 올린 글감:
                """);
        if (yearTitles.isEmpty()) {
            b.append("- 아직 없어요\n");
        }
        yearTitles.forEach(t -> b.append("- ").append(t).append('\n'));
        return b.toString().strip();
    }

    /** 겹침 비교용: 띄어쓰기·문장부호를 빼고 소문자로. */
    public static String key(String title) {
        return title.toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s\\p{P}\\p{S}]", "");
    }
}
