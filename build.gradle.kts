import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.lipen"

plugins {
    idea
    kotlin("jvm") version Versions.kotlin
    id("org.jmailen.kotlinter") version Versions.kotlinter
    id("com.github.ben-manes.versions") version Versions.gradle_versions
    id("fr.brouillard.oss.gradle.jgitver") version Versions.jgitver
    id("me.champeau.gradle.jmh") version Versions.jmh_gradle_plugin
    `maven-publish`
    id("com.github.johnrengelman.shadow") version Versions.shadow
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(Libs.junit_jupiter_api)
    testRuntimeOnly(Libs.junit_jupiter_engine)
    testImplementation(Libs.kluent)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    @Suppress("UnstableApiUsage")
    useJUnitPlatform()
    testLogging.events(
        // TestLogEvent.PASSED,
        TestLogEvent.FAILED,
        TestLogEvent.SKIPPED,
        TestLogEvent.STANDARD_ERROR
    )
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

kotlinter {
    ignoreFailures = true
    experimentalRules = true
}

jgitver {
    strategy("MAVEN")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven(url = "$buildDir/repository")
    }
}

jmh {
    jmhVersion = Versions.jmh
    humanOutputFile = project.file("${project.buildDir}/reports/jmh/human.txt")
    resultsFile = project.file("${project.buildDir}/reports/jmh/results.json")
    resultFormat = "JSON"
}

tasks.shadowJar {
    minimize()
}

tasks.wrapper {
    gradleVersion = "6.2"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
