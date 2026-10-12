// 두 모듈의 플러그인 버전을 한곳에서 정한다(각 모듈은 버전 없이 쓴다).
plugins {
    id("org.springframework.boot") version "4.1.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {
    group = "com.team"
    version = "0.0.1-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}
