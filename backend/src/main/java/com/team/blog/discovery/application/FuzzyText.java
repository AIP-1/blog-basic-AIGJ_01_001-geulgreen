package com.team.blog.discovery.application;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 094 강성찬 개인 확장: 검색 오타 허용용 글자 다루기. 띄어쓰기·대소문자를 지운 키, 한글 자모 분해, 편집 거리, 조사(을/를).
 */
public final class FuzzyText {

    private FuzzyText() {
    }

    /** 비교 키: NFC → 소문자 → 공백 제거. */
    public static String key(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    /** 완성형 한글을 초성·중성·종성으로 푼다("갈" → ㄱㅏㄹ). 한 글자 오타가 거리 1~2로 잡힌다. */
    public static String jamo(String s) {
        StringBuilder b = new StringBuilder(s.length() * 3);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int idx = c - 0xAC00;
                b.append((char) (0x1100 + idx / 588)).append((char) (0x1161 + idx % 588 / 28));
                if (idx % 28 != 0) {
                    b.append((char) (0x11A7 + idx % 28));
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /** 자모 길이에 따라 봐줄 거리: 4 이하 1, 9 이하 2, 그 위 3. */
    public static int allowance(String jamo) {
        int n = jamo.length();
        if (n <= 4) {
            return 1;
        }
        return n <= 9 ? 2 : 3;
    }

    /** 편집 거리(삽입·삭제·바꾸기 1). {@code max}를 넘으면 일찍 그만두고 max+1. */
    public static int distance(String a, String b, int max) {
        if (Math.abs(a.length() - b.length()) > max) {
            return max + 1;
        }
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            int rowMin = cur[0];
            for (int j = 1; j <= b.length(); j++) {
                int sub = prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                cur[j] = Math.min(sub, Math.min(prev[j], cur[j - 1]) + 1);
                rowMin = Math.min(rowMin, cur[j]);
            }
            if (rowMin > max) {
                return max + 1;
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    /** 글자·숫자 덩어리로 나눈다(2글자 이상만). 마크다운 기호·문장부호는 버린다. */
    public static List<String> words(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) {
            return out;
        }
        for (String w : Normalizer.normalize(s, Normalizer.Form.NFC).split("[^\\p{L}\\p{N}_+#.-]+")) {
            String t = w.replaceAll("(?:^[_+#.-]+)|(?:[_.-]+$)", ""); // C++·C# 끝 기호는 남긴다
            if (t.codePointCount(0, t.length()) >= 2) {
                out.add(t);
            }
        }
        return out;
    }

    /** 끝 글자 받침에 맞춘 목적격 조사. 한글이 아니면 "을(를)". */
    public static String objectParticle(String word) {
        if (word.isEmpty()) {
            return "을(를)";
        }
        char c = word.charAt(word.length() - 1);
        if (c < 0xAC00 || c > 0xD7A3) {
            return "을(를)";
        }
        return (c - 0xAC00) % 28 == 0 ? "를" : "을";
    }
}
