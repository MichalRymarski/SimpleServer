import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("gg.jte.gradle") version "3.2.4"
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass = "mr.server.SimpleServerKt"
}

// jte templates: src/main/jte -> precompiled classes in jte-classes/
jte {
    precompile()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

repositories {
    mavenCentral()
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(false)
            jvmTarget.set(JVM_21)
            freeCompilerArgs.add("-jvm-default=enable")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(platform("org.http4k:http4k-bom:6.60.0.0"))

    // http4k - core + server
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-helidon")
    implementation("org.http4k:http4k-client-helidon")
    implementation("org.http4k:http4k-client-websocket")

    // http4k - formats, templates, config
    implementation("org.http4k:http4k-config")
    implementation("org.http4k:http4k-format-jackson")
    implementation("org.http4k:http4k-multipart")
    implementation("org.http4k:http4k-template-jte")
    implementation("org.http4k:http4k-web-datastar")

    // http4k - contracts + security
    implementation("org.http4k:http4k-api-openapi")
    implementation("org.http4k:http4k-security-digest")
    implementation("org.http4k:http4k-security-oauth")
    implementation("org.http4k:http4k-connect-storage-jdbc")

    // http4k pro - mcp, hot reload
    implementation("org.http4k.pro:http4k-ai-mcp-sdk")
    implementation("org.http4k.pro:http4k-tools-hotreload")

    // logging - slf4j facade + logback backend (+ helidon bridge)
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.helidon.logging:helidon-logging-slf4j:4.5.4")

    // jte - direct api use (precompiled engine) + gradle plugin below
    implementation("gg.jte:jte:3.2.4")

    // test
    testImplementation("org.http4k:http4k-testing-hamkrest")
    testImplementation("org.http4k:http4k-testing-kotest")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.1.3")
    testImplementation("org.junit.platform:junit-platform-launcher:6.1.3")
}

// runtime only: must NOT touch the Kotlin compiler's own classpath (breaks the build)
listOf("runtimeClasspath", "testRuntimeClasspath").forEach { name ->
    configurations.named(name) {
        resolutionStrategy {
            // keep JTE's embedded compiler readable against the Kotlin 2.4
            // metadata on the classpath (http4k BOM -> stdlib 2.4.0, but
            // jte-kotlin pins kotlin-compiler-embeddable 2.2.x).
            force("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")
        }
    }
}

// dev server with hot reload: ./gradlew hotReload [-PhotreloadPort=8123]
tasks.register<JavaExec>("hotReload") {
    group = "application"
    description = "Run the hot-reload dev server (default port 8000)."
    dependsOn("classes", "testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("HotReloadServerKt")
    systemProperty("hotreload.port", providers.gradleProperty("hotreloadPort").getOrElse("8000"))
}

