import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.lipen"

plugins {
    kotlin("jvm") version Versions.kotlin
    id("org.jlleitschuh.gradle.ktlint") version Versions.ktlint
    id("com.github.ben-manes.versions") version Versions.gradle_versions
    id("fr.brouillard.oss.gradle.jgitver") version Versions.jgitver
    `maven-publish`
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(Libs.junit_jupiter_api)
    testRuntimeOnly(Libs.junit_jupiter_engine)
    testImplementation(Libs.kluent)
}

ktlint {
    ignoreFailures.set(true)
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

tasks.wrapper {
    gradleVersion = "6.2"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
