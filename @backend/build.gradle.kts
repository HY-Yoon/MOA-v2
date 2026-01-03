plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "moa2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-web-services")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    // Kafka 주석 처리
    // implementation("org.springframework.boot:spring-boot-starter-kafka")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("generateTypeScript") {
    group = "generate"
    description = "Generate TypeScript enums from Java enums"

    doLast {
        val sharedDir = file("../@shared")
        sharedDir.mkdirs()

        val outputFile = File(sharedDir, "enums.ts")
        val typescript = StringBuilder()

        typescript.append("// Auto-generated from Java enums - DO NOT EDIT\n\n")

        fileTree("src/main/java/com/moa2/global/model") {
            include("*.java")
        }.forEach { javaFile ->
            val content = javaFile.readText()

            if (content.contains("public enum")) {
                val enumName = javaFile.nameWithoutExtension

                // 주석 제거 후 enum 추출
                val cleanedContent = content
                    .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "") // 블록 주석 제거
                    .replace(Regex("//.*"), "") // 라인 주석 제거

                val enumPattern = Regex("""enum\s+$enumName\s*\{([^}]+)\}""", RegexOption.DOT_MATCHES_ALL)
                val match = enumPattern.find(cleanedContent)

                if (match != null) {
                    val enumBody = match.groupValues[1]

                    val values = enumBody
                        .split(",")
                        .map {
                            it.trim()
                                .split(Regex("\\s+"))[0] // 첫 번째 단어만 (공백 전까지)
                                .replace(Regex("\\(.*?\\)"), "")
                                .replace(";", "")
                                .trim()
                        }
                        .filter {
                            it.isNotEmpty() &&
                                    it.matches(Regex("[A-Z][A-Z0-9_]*"))
                        }

                    if (values.isNotEmpty()) {
                        val tsValues = values.joinToString(" | ") { "\"$it\"" }
                        typescript.append("export type $enumName = $tsValues;\n\n")

                        println("$enumName: ${values.joinToString(", ")}")
                    }
                }
            }
        }

        outputFile.writeText(typescript.toString())
        println("Generated file: ${outputFile.absolutePath}")
        println("Total types: ${typescript.lines().count { it.startsWith("export type") }}")
    }
}

tasks.named("compileJava") {
    dependsOn("generateTypeScript")
}