import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.lipen"

plugins {
    kotlin("jvm") version Versions.kotlin
    id("org.jmailen.kotlinter") version Versions.kotlinter
    id("com.github.ben-manes.versions") version Versions.gradle_versions
    id("fr.brouillard.oss.gradle.jgitver") version Versions.jgitver
    id("me.champeau.gradle.jmh") version Versions.jmh_gradle_plugin
    `maven-publish`
}

repositories {
    maven(url = "https://jitpack.io")
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(Libs.multiarray)
    implementation(Libs.kotlin_jnisat)

    testImplementation(Libs.junit_jupiter_api)
    testRuntimeOnly(Libs.junit_jupiter_engine)
    testImplementation(Libs.kluent)
}

jmh {
    jmhVersion = Versions.jmh
    humanOutputFile = project.file("${project.buildDir}/reports/jmh/human.txt")
    resultsFile = project.file("${project.buildDir}/reports/jmh/results.json")
    resultFormat = "JSON"
}

kotlinter {
    ignoreFailures = true
    experimentalRules = true
    disabledRules = arrayOf("import-ordering")
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
    gradleVersion = "6.2.2"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
