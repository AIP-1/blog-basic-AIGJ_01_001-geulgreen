// 두 모듈의 플러그인 버전을 한곳에서 정한다(각 모듈은 버전 없이 쓴다).
plugins {
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    // 코드 품질 분석(학교 SonarQube, CI의 sonar job에서만 돈다)
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "blog-basic-AIGJ_01_001-geulgreen")
        property("sonar.projectName", "글그린 블로그")
    }
}

subprojects {
    group = "com.team"
    version = "0.0.1-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}
