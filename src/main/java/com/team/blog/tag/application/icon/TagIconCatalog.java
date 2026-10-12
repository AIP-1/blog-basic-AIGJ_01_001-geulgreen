package com.team.blog.tag.application.icon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 056 태그 아이콘(강성찬 개인 확장): 쓸 수 있는 아이콘(그림은 {@code fragments/tag-icons.html})과 기본 색, 내장 규칙.
 * 규칙은 위에서부터 태그 이름에 낱말이 들어 있는지 보고 처음 맞는 것을 쓴다(더 구체적인 것을 위에). 아무것도 맞지 않으면 #.
 */
public final class TagIconCatalog {

    public record Style(String icon, String tone) {
    }

    private record Rule(String icon, String tone, List<String> words) {
    }

    /** 아이콘 키 → AI에게 줄 설명. 순서는 AI 목록 순서. */
    public static final Map<String, String> ICONS = new LinkedHashMap<>();

    /** 아이콘 키 → 기본 색. */
    static final Map<String, String> DEFAULT_TONE = new LinkedHashMap<>();

    /** 색 이름(화면의 {@code tone-…} 클래스). */
    public static final List<String> TONES = List.of("green", "pink", "sand", "blue", "indigo", "orange", "yellow", "purple",
            "teal", "gray");

    private static final List<Rule> RULES = List.of(
            rule("github", "gray", "github", "깃허브", "깃헙", "git"),
            rule("whale", "blue", "docker", "도커", "kubernetes", "k8s", "컨테이너", "container"),
            rule("java", "orange", "java", "자바", "jvm", "jpa", "kotlin", "코틀린"),
            rule("leaf", "green", "thymeleaf"),
            rule("flower", "pink", "spring", "스프링", "봄", "꽃", "flower", "벚꽃"),
            rule("database", "indigo", "postgres", "mysql", "sql", "데이터베이스", "flyway", "redis", "mongo", "oracle", "오라클", "db"),
            rule("cloud", "blue", "배포", "deploy", "cloud", "클라우드", "aws", "서버", "server", "인프라", "infra"),
            rule("globe", "teal", "web", "웹", "html", "css", "javascript", "typescript", "react", "vue", "프론트", "frontend"),
            rule("phone", "purple", "android", "안드로이드", "ios", "모바일", "mobile", "flutter", "swift", "앱"),
            rule("lock", "gray", "보안", "security", "인증", "auth", "oauth", "로그인", "login", "암호"),
            rule("bug", "orange", "버그", "bug", "디버깅", "debug", "에러", "error", "트러블", "오류", "장애"),
            rule("chart", "green", "성능", "performance", "통계", "데이터", "data", "분석", "지표", "트래픽"),
            rule("code", "purple", "개발", "코드", "code", "코딩", "프로그래밍", "programming", "알고리즘", "algorithm", "백엔드",
                    "backend", "리팩터링", "refactor", "api", "테스트", "test", "dev"),
            rule("book", "sand", "독서", "책", "book", "소설", "서적", "읽기", "문학", "에세이", "시집"),
            rule("pencil", "yellow", "공부", "스터디", "study", "시험", "자격증", "학습", "til", "글쓰기", "필사"),
            rule("cap", "indigo", "학교", "수업", "강의", "교육", "대학", "과제", "졸업"),
            rule("palm", "green", "제주", "바다", "beach", "해변", "섬", "휴양"),
            rule("plane", "blue", "여행", "travel", "trip", "해외", "비행", "휴가"),
            rule("pin", "sand", "부산", "서울", "대구", "인천", "광주", "대전", "울산", "강릉", "경주", "전주", "여수", "도쿄", "오사카",
                    "파리", "뉴욕", "동네", "지역", "장소"),
            rule("camera", "blue", "사진", "photo", "카메라", "camera", "필름", "출사"),
            rule("paw", "orange", "산책", "고양이", "냥", "강아지", "반려", "pet", "cat", "dog", "동물"),
            rule("coffee", "orange", "커피", "coffee", "카페", "cafe", "라떼"),
            rule("bowl", "orange", "맛집", "요리", "음식", "레시피", "food", "먹방", "디저트", "베이킹", "점심", "저녁"),
            rule("maple", "orange", "가을", "autumn", "단풍"),
            rule("sun", "yellow", "여름", "summer", "날씨", "weather", "햇살"),
            rule("snow", "blue", "겨울", "winter", "크리스마스", "snow"),
            rule("mountain", "green", "등산", "산행", "캠핑", "camping", "자연", "숲", "하이킹", "트레킹"),
            rule("music", "purple", "음악", "music", "노래", "플레이리스트", "playlist", "피아노", "콘서트"),
            rule("film", "indigo", "영화", "movie", "드라마", "drama", "넷플릭스", "netflix", "애니"),
            rule("game", "purple", "게임", "game", "닌텐도", "steam", "플스"),
            rule("heart", "pink", "사랑", "연애", "감사", "행복", "감성", "마음"),
            rule("sprout", "green", "입문", "성장", "습관", "루틴", "기초", "새싹", "초보"),
            rule("star", "yellow", "시작", "처음", "첫", "추천", "best", "베스트"),
            rule("chat", "purple", "일문", "질문", "대화", "이야기", "생각", "고민", "잡담", "qna", "토론"),
            rule("leaf", "green", "일상", "daily", "하루", "기록", "일기", "diary", "오늘", "주말", "기분", "life", "라이프"),
            rule("bulb", "yellow", "팁", "tip", "아이디어", "idea", "노하우"),
            rule("gear", "gray", "설정", "도구", "tool", "툴", "환경", "세팅", "setup", "intellij", "vscode", "맥북"),
            rule("rocket", "indigo", "출시", "launch", "스타트업", "startup", "프로젝트", "project", "사이드", "창업"),
            rule("coin", "yellow", "경제", "재테크", "money", "주식", "투자", "부동산", "가계부", "절약"),
            rule("dumbbell", "teal", "운동", "헬스", "건강", "다이어트", "러닝", "running", "요가", "fitness", "수영"),
            rule("calendar", "teal", "회고", "일정", "계획", "월간", "결산", "plan", "목표", "다짐", "주간"));

    static {
        icon("leaf", "green", "일상·기록·잎");
        icon("flower", "pink", "꽃·봄·spring");
        icon("book", "sand", "책·독서·소설");
        icon("plane", "blue", "여행·비행");
        icon("database", "indigo", "데이터베이스·SQL");
        icon("maple", "orange", "가을·단풍");
        icon("cloud", "blue", "배포·서버·클라우드");
        icon("pin", "sand", "지역·장소·도시");
        icon("camera", "blue", "사진·카메라");
        icon("paw", "orange", "산책·반려동물");
        icon("chart", "green", "성능·통계·데이터");
        icon("star", "yellow", "시작·추천");
        icon("chat", "purple", "대화·질문·생각");
        icon("palm", "green", "바다·섬·휴양지");
        icon("coffee", "orange", "커피·카페");
        icon("code", "purple", "개발·코드·프로그래밍");
        icon("whale", "blue", "도커·컨테이너");
        icon("github", "gray", "깃허브·깃");
        icon("java", "orange", "자바·JVM 언어");
        icon("sprout", "green", "입문·성장·습관");
        icon("heart", "pink", "사랑·감성·감사");
        icon("music", "purple", "음악·노래");
        icon("film", "indigo", "영화·드라마");
        icon("game", "purple", "게임");
        icon("bowl", "orange", "음식·요리·맛집");
        icon("sun", "yellow", "여름·날씨");
        icon("snow", "blue", "겨울·눈");
        icon("mountain", "green", "등산·캠핑·자연");
        icon("pencil", "yellow", "공부·글쓰기");
        icon("bulb", "yellow", "팁·아이디어");
        icon("bug", "orange", "버그·오류·디버깅");
        icon("lock", "gray", "보안·인증");
        icon("gear", "gray", "설정·도구");
        icon("globe", "teal", "웹·프론트엔드");
        icon("phone", "purple", "모바일·앱");
        icon("rocket", "indigo", "프로젝트·출시");
        icon("cap", "indigo", "학교·강의·교육");
        icon("coin", "yellow", "경제·돈");
        icon("dumbbell", "teal", "운동·건강");
        icon("calendar", "teal", "회고·일정·계획");
        icon("hash", "gray", "그 밖의 주제");
    }

    private TagIconCatalog() {
    }

    private static void icon(String key, String tone, String hint) {
        ICONS.put(key, hint);
        DEFAULT_TONE.put(key, tone);
    }

    private static Rule rule(String icon, String tone, String... words) {
        return new Rule(icon, tone, List.of(words));
    }

    /** 내장 규칙. 맞는 규칙이 없으면 #과 이름으로 정해지는 색. */
    public static Style byRule(String tagName) {
        String name = tagName == null ? "" : tagName.toLowerCase();
        for (Rule r : RULES) {
            for (String w : r.words()) {
                if (matches(name, w)) {
                    return new Style(r.icon(), r.tone());
                }
            }
        }
        return new Style("hash", TONES.get(Math.floorMod(name.hashCode(), TONES.size())));
    }

    /** 짧은 영어 낱말(3자 이하)은 낱말 첫머리에서만(예: api는 rapid에 안 맞음), 나머지는 어디든 들어 있으면. */
    static boolean matches(String name, String word) {
        if (word.length() > 3 || !word.chars().allMatch(c -> c < 128)) {
            return name.contains(word);
        }
        int from = 0;
        while (true) {
            int at = name.indexOf(word, from);
            if (at < 0) {
                return false;
            }
            if (at == 0 || !Character.isLetter(name.charAt(at - 1))) {
                return true;
            }
            from = at + 1;
        }
    }

    /** AI 답에서 아이콘 키 하나를 찾는다(목록에 없는 말은 버림). */
    public static Optional<Style> parseAiAnswer(String answer) {
        if (answer == null) {
            return Optional.empty();
        }
        String text = answer.toLowerCase();
        for (String key : ICONS.keySet()) {
            if (text.matches("(?s).*\\b" + key + "\\b.*")) {
                return Optional.of(new Style(key, DEFAULT_TONE.get(key)));
            }
        }
        return Optional.empty();
    }
}
