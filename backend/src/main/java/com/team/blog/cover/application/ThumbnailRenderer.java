package com.team.blog.cover.application;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * 110 강성찬 개인 확장: 썸네일 그림(1600x1000 PNG). 글쓰기 화면의 썸네일 만들기(static/js/editor/thumbnail.js)와 같은 배치를
 * 서버에서 그린다(AI 연결 도구용). 글꼴은 앱에 넣은 Pretendard(OFL, resources/fonts/pretendard)라 서버에 깔린 글꼴과 상관없다.
 * 색은 theme.css의 --thumb-* 값과 같다(그림 파일이라 화면 테마와 상관없이 고정).
 */
@Component
public class ThumbnailRenderer {

    static final int W = 1600;
    static final int H = 1000;
    // 1.91:1로 가운데를 자르면 위아래 81px씩 잘린다. 거기에 여백을 더한 안전 영역(화면 JS와 같은 값)
    private static final int SAFE_X = 140;
    private static final int SAFE_TOP = 170;
    private static final int SAFE_BOTTOM = 830;

    /** 배경·제목·작은 글자 색. theme.css --thumb-{name}-bg/fg/sub와 같은 값. */
    public record Theme(Color bg, Color fg, Color sub) {
    }

    public static final Map<String, Theme> THEMES = Map.of(
            "forest", new Theme(new Color(0x3D6B55), new Color(0xFFFFFF), new Color(0xD7E9DF)),
            "cream", new Theme(new Color(0xFBF7F1), new Color(0x2E2A26), new Color(0x6B6158)),
            "sunset", new Theme(new Color(0xA85A22), new Color(0xFFFFFF), new Color(0xFBE6D6)),
            "ink", new Theme(new Color(0x2E2A26), new Color(0xFBF7F1), new Color(0xC9BFB3)));

    private final Font bold = load("fonts/pretendard/Pretendard-Bold.otf");
    private final Font medium = load("fonts/pretendard/Pretendard-Medium.otf");

    /** {@code theme}이 없거나 모르는 이름이면 forest. */
    public byte[] render(String title, String series, String blog, String date, String theme) {
        Theme t = THEMES.getOrDefault(theme == null ? "" : theme, THEMES.get("forest"));
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setColor(t.bg());
            g.fillRect(0, 0, W, H);
            int maxWidth = W - SAFE_X * 2;
            String text = title == null || title.isBlank() ? "제목 없음" : title.strip();
            // 제목: 3줄에 들 때까지 글자를 줄이고(64px까지), 그래도 길면 3줄째 끝을 …로
            int size = 104;
            for (;; size -= 8) {
                g.setFont(bold.deriveFont((float) size));
                FontMetrics fm = g.getFontMetrics();
                if (wrapLines(text, fm::stringWidth, maxWidth, 99).size() <= 3 || size <= 64) {
                    break;
                }
            }
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = wrapLines(text, fm::stringWidth, maxWidth, 3);
            int lineH = Math.round(size * 1.3f);
            int top = (SAFE_TOP + SAFE_BOTTOM) / 2 - lineH * lines.size() / 2;
            if (series != null && !series.isBlank()) {
                g.setFont(medium.deriveFont(Font.PLAIN, 40f));
                g.setColor(t.sub());
                g.drawString(wrapLines(series.strip(), g.getFontMetrics()::stringWidth, maxWidth, 1).get(0), SAFE_X, top - 36);
            }
            g.setFont(bold.deriveFont((float) size));
            g.setColor(t.fg());
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(lines.get(i), SAFE_X, top + lineH * (i + 1) - Math.round(size * 0.3f));
            }
            g.setFont(medium.deriveFont(36f));
            g.setColor(t.sub());
            String footer = String.join("  ·  ", java.util.stream.Stream.of(blog, date).filter(s -> s != null && !s.isBlank()).toList());
            g.drawString(footer, SAFE_X, SAFE_BOTTOM);
            g.fillRect(SAFE_X, SAFE_BOTTOM - 76, 64, 6);
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 띄어쓰기 단위로 maxWidth 안에 나눈다. 한 단어가 넘치면 글자 단위. 최대 maxLines줄(넘치면 마지막 줄 끝을 …). */
    static List<String> wrapLines(String text, ToIntFunction<String> measure, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.strip().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String next = line.isEmpty() ? word : line + " " + word;
            if (measure.applyAsInt(next) <= maxWidth) {
                line = next;
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line);
            }
            StringBuilder chunk = new StringBuilder();
            for (int cp : word.codePoints().toArray()) {
                String ch = new String(Character.toChars(cp));
                if (chunk.length() > 0 && measure.applyAsInt(chunk + ch) > maxWidth) {
                    lines.add(chunk.toString());
                    chunk.setLength(0);
                }
                chunk.append(ch);
            }
            line = chunk.toString();
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
            String last = lines.get(maxLines - 1);
            while (!last.isEmpty() && measure.applyAsInt(last + "…") > maxWidth) {
                last = last.substring(0, last.offsetByCodePoints(last.length(), -1));
            }
            lines.set(maxLines - 1, last + "…");
        }
        return lines;
    }

    private static Font load(String path) {
        try (InputStream in = ThumbnailRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("글꼴 파일이 없어요: " + path);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            throw new IllegalStateException("글꼴을 읽지 못했어요: " + path, e);
        }
    }
}
