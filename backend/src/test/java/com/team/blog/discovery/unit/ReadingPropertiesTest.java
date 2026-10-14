package com.team.blog.discovery.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.discovery.application.ReadingProperties;
import org.junit.jupiter.api.Test;

/** 092 후속: 카카오톡 공유는 키가 있고 공유가 켜져 있을 때만. */
class ReadingPropertiesTest {

    @Test
    void kakaoNeedsKeyAndShare() {
        assertThat(new ReadingProperties(true, true, true, true, "abc").kakaoEnabled()).isTrue();
        assertThat(new ReadingProperties(true, true, true, true, "  ").kakaoEnabled()).isFalse();
        assertThat(new ReadingProperties(true, true, true, true, null).kakaoEnabled()).isFalse();
        assertThat(new ReadingProperties(true, true, true, false, "abc").kakaoEnabled()).isFalse();
    }
}
