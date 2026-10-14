package com.team.blog.discovery.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.discovery.application.FuzzyText;
import org.junit.jupiter.api.Test;

/** 094 강성찬 개인 확장: 오타 비교용 글자 다루기. */
class FuzzyTextTest {

    private static int d(String a, String b) {
        return FuzzyText.distance(FuzzyText.jamo(FuzzyText.key(a)), FuzzyText.jamo(FuzzyText.key(b)), 5);
    }

    @Test
    void keyIgnoresSpacingAndCase() {
        assertThat(FuzzyText.key(" Spring  부트 ")).isEqualTo("spring부트");
    }

    @Test
    void oneSyllableTypoIsSmallInJamo() {
        assertThat(FuzzyText.jamo("갈")).hasSize(3);
        assertThat(d("트랜잭선", "트랜잭션")).isEqualTo(1);
        assertThat(d("스프링부투", "스프링 부트")).isEqualTo(1);
        assertThat(d("sprnig", "Spring")).isEqualTo(2);
        assertThat(FuzzyText.distance("abcdef", "uvwxyz", 2)).isEqualTo(3);
    }

    @Test
    void wordsDropMarkdownAndOneLetters() {
        assertThat(FuzzyText.words("## JPA 트랜잭션, a `@Transactional`! C++ node.js"))
                .containsExactly("JPA", "트랜잭션", "Transactional", "C++", "node.js");
    }

    @Test
    void objectParticleFollowsFinalConsonant() {
        assertThat(FuzzyText.objectParticle("트랜잭션")).isEqualTo("을");
        assertThat(FuzzyText.objectParticle("스프링 부트")).isEqualTo("를");
        assertThat(FuzzyText.objectParticle("spring")).isEqualTo("을(를)");
    }
}
