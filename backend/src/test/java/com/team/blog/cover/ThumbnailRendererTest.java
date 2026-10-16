package com.team.blog.cover;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.blog.cover.application.ThumbnailRenderer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** 110 썸네일 그림: 앱에 넣은 글꼴로 1600x1000 PNG, 한글 제목이 그려진다(배경색만 있는 그림이 아니다). */
class ThumbnailRendererTest {

    @Test
    void rendersKoreanTitleOnThemeBackground() throws Exception {
        byte[] png = new ThumbnailRenderer().render("스프링 부트 입문 3편: JdbcTemplate으로 DB 붙이기", "스프링 부트 입문",
                "@writer", "2026.10.16", "forest");
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(img.getWidth()).isEqualTo(1600);
        assertThat(img.getHeight()).isEqualTo(1000);
        assertThat(img.getRGB(10, 10) & 0xFFFFFF).isEqualTo(0x3D6B55);
        int white = 0;
        for (int x = 140; x < 1460; x += 2) {
            for (int y = 300; y < 700; y += 2) {
                if ((img.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                    white++;
                }
            }
        }
        assertThat(white).as("제목 글자 픽셀").isGreaterThan(2000);
    }
}
