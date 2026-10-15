package com.team.blog.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 결과 상자(toast.js)의 성공·오류는 글을 보고 짐작하지 않고 data-toast="success|error"로만 정한다(강성찬 개인 확장).
 * 결과 안내 칸에 종류가 빠지거나, 스크립트가 실패 문구를 종류 없이 쓰면 여기서 실패한다.
 */
class ResultToastKindTest {

    private static final Path RES = Path.of("../frontend/src/main/resources");
    private static final Pattern TAG = Pattern.compile("<[a-z]+\\b[^>]*>");
    // 결과 안내로 보는 칸: 결과 줄(form-status), 폼 오류(form-error), 상태 안내(notice + role=status)
    private static final Pattern RESULT = Pattern.compile("class=\"[^\"]*\\b(form-status|form-error)\\b|class=\"notice[^\"]*\"[^>]*role=\"status\"|role=\"status\"[^>]*class=\"notice");
    private static final Pattern KIND = Pattern.compile("\\s(data-toast=\"(success|error)\"|th:data-toast=\"\\$\\{[^}]+}\"|data-toast-inline)[\\s>]");
    private static final Pattern ANY_TOAST = Pattern.compile("\\sdata-toast(=\"([^\"]*)\")?[\\s>]");
    private static final Pattern SELECTOR = Pattern.compile("(?:id|class)=\"([^\"]+)\"");

    @Test
    void everyResultMessageInTemplatesStatesItsKind() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path f : templates()) {
            Matcher m = TAG.matcher(Files.readString(f));
            while (m.find()) {
                String tag = m.group();
                Matcher any = ANY_TOAST.matcher(tag);
                if (any.find() && !"success".equals(any.group(2)) && !"error".equals(any.group(2))) {
                    missing.add(f + ": data-toast 값은 success|error " + tag);
                }
                if (RESULT.matcher(tag).find() && !KIND.matcher(tag).find()) {
                    missing.add(f + ": 결과 안내에 data-toast 종류(또는 data-toast-inline)가 없음 " + tag);
                }
            }
        }
        assertThat(missing).isEmpty();
    }

    /** 결과 칸을 채우는 스크립트가 실패 문구를 쓰면, 그 파일은 종류(dataset.toast 또는 'error' 인자)를 알려야 한다. */
    // 결과 칸에는 성공만 쓰고 실패는 다른 곳(칸 옆 오류·행 오류·복사 창)에 쓰는 스크립트
    private static final List<String> FAILURES_GO_ELSEWHERE = List.of(
            "manage.js",         // 실패는 행마다 .row-error
            "reading-extras.js", // 복사 실패는 window.prompt
            "handle-change.js"); // 실패는 칸 옆 [data-error-for=handle]

    @Test
    void scriptsThatWriteFailuresIntoResultLinesMarkThemAsErrors() throws IOException {
        List<String> selectors = new ArrayList<>();
        for (Path f : templates()) {
            Matcher m = TAG.matcher(Files.readString(f));
            while (m.find()) {
                if (m.group().contains(" data-toast=")) {
                    Matcher s = SELECTOR.matcher(m.group());
                    while (s.find()) {
                        for (String token : s.group(1).split("\\s+")) {
                            if (!token.isEmpty() && !List.of("help", "notice", "form-status", "form-error", "field-error").contains(token)) {
                                selectors.add(token);
                            }
                        }
                    }
                }
            }
        }
        List<String> unmarked = new ArrayList<>();
        try (Stream<Path> files = Files.walk(RES.resolve("static/js"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".js") && !p.endsWith("toast.js")).toList()) {
                String js = Files.readString(f);
                boolean writesResult = selectors.stream().anyMatch(s -> js.contains("'" + s + "'") || js.contains("'." + s + "'"));
                if (writesResult && js.contains("못했") && !js.contains("dataset.toast") && !js.contains(", 'error')")
                        && !FAILURES_GO_ELSEWHERE.contains(f.getFileName().toString())) {
                    unmarked.add(f.toString());
                }
            }
        }
        assertThat(unmarked).as("실패 문구를 종류 없이 결과 칸에 쓰는 스크립트").isEmpty();
        assertThat(Files.readString(RES.resolve("static/js/toast.js"))).as("toast.js는 글로 종류를 짐작하지 않는다")
                .doesNotContain("못했").doesNotContain("RegExp").doesNotContain(".test(");
    }

    private static List<Path> templates() throws IOException {
        try (Stream<Path> files = Files.walk(RES.resolve("templates"))) {
            return files.filter(p -> p.toString().endsWith(".html")).toList();
        }
    }
}
