import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.lipen"

plugins {
    kotlin("jvm") version Versions.kotlin
    id("org.jmailen.kotlinter") version Versions.kotlinter
    id("com.github.ben-manes.versions") version Versions.gradle_versions
    id("fr.brouillard.oss.gradle.jgitver") version Versions.jgitver
    id("de.undercouch.download") version Versions.gradle_download
    `maven-publish`
}

repositories {
    maven(url = "https://jitpack.io")
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(Libs.okio)
    implementation(Libs.multiarray)
    implementation(Libs.kotlin_jnisat)

    testImplementation(Libs.junit_jupiter_api)
    testRuntimeOnly(Libs.junit_jupiter_engine)
    testImplementation(Libs.kluent)
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
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.register<Download>("downloadSharedLibs") {
    val urlTemplate = "https://github.com/Lipen/kotlin-jnisat/releases/download/${Versions.kotlin_jnisat}/%s"
    src(
        listOf(
            urlTemplate.format("libjminisat.so"),
            urlTemplate.format("libjcadical.so")
        )
    )
    dest(File("src/main/resources/lib/linux64"))
    overwrite(true)
}

tasks.wrapper {
    gradleVersion = "6.2.2"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
