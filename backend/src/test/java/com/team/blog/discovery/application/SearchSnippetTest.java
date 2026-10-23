package com.team.blog.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 10-22 UX 재점검: 검색 미리보기는 마크다운 기호 없이 글자만 보여 준다. */
class SearchSnippetTest {

    @Test
    void stripsMarkdownMarksFromPreview() {
        String md = "# 변수\n\n`let`과 **const**의 차이를 [정리](https://example.com)했어요.\n\n## 자주 하는 실수\n\n- 선언 전에 쓰기\n> 인용\n```js\nlet a = 1;\n```\n![사진](/img.png)";
        SearchSnippet.Result r = SearchSnippet.of(md, "제목", List.of("변수"));
        String plain = r.parts().stream().map(SearchSnippet.Part::text).reduce("", String::concat);
        assertThat(plain).startsWith("변수 let과 const의 차이를 정리했어요. 자주 하는 실수");
        assertThat(plain).doesNotContain("#").doesNotContain("`").doesNotContain("**").doesNotContain("](");
        assertThat(r.html()).contains("<mark>변수</mark>");
    }

    @Test
    void flatKeepsPlainTextAndCollapsesSpaces() {
        assertThat(SearchSnippet.flat("오늘은   비가\n와서  창밖만 봤어요")).isEqualTo("오늘은 비가 와서 창밖만 봤어요");
        assertThat(SearchSnippet.flat(null)).isEmpty();
    }
}
