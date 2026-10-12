// 화면 모듈: templates/·static/을 jar로 묶어 backend가 classpath:/templates/·classpath:/static/으로 읽는다.
// 브라우저에서 쓰는 라이브러리(webjars)도 여기서 고른다. 화면 JS 자체 검사: node frontend/src/test/js/*.test.mjs
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // 코드 문법 강조(브라우저, 우리 서버의 /webjars/에서 제공 — 02 기술 표)
    runtimeOnly("org.webjars.npm:highlightjs__cdn-assets:11.11.1")
    // 060 보이는 대로 쓰기(강성찬 개인 확장): Markdown ↔ 화면 변환(브라우저, /webjars/에서 제공)
    runtimeOnly("org.webjars.npm:marked:15.0.12")
    runtimeOnly("org.webjars.npm:turndown:7.2.0") { isTransitive = false } // Node용 DOM(domino)은 필요 없음
    runtimeOnly("org.webjars.npm:turndown-plugin-gfm:1.0.2")
}
