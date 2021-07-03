import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.Lipen"

plugins {
    idea
    kotlin("jvm") version Versions.kotlin
    with(Plugins.Kotlinter) { id(id) version (version) }
    with(Plugins.Jgitver) { id(id) version (version) }
    with(Plugins.GradleVersions) { id(id) version (version) }
    with(Plugins.Shadow) { id(id) version (version) }
    with(Plugins.Jmh) { id(id) version (version) apply false }
    `maven-publish`
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = Plugins.Kotlinter.id)
    apply(plugin = "maven-publish")

    repositories {
        maven(url = "https://jitpack.io")
        jcenter()
    }

    dependencies {
        implementation(platform(kotlin("bom")))
        implementation(kotlin("stdlib-jdk8"))
        implementation(Libs.KotlinLogging.kotlin_logging)

        testImplementation(Libs.JUnit.jupiter_api)
        testRuntimeOnly(Libs.JUnit.jupiter_engine)
        testImplementation(Libs.JUnit.jupiter_params)
        testImplementation(Libs.Kluent.kluent)
        testImplementation(Libs.Log4j.log4j_slf4j_impl)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
            showStandardStreams = true
            events(
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_ERROR
            )
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    kotlinter {
        ignoreFailures = true
        experimentalRules = true
        disabledRules = arrayOf("import-ordering")
    }

    java {
        @Suppress("UnstableApiUsage")
        withSourcesJar()
        // @Suppress("UnstableApiUsage")
        // withJavadocJar()
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
}

subprojects {
    group = "${rootProject.group}.${rootProject.name}"
    version = rootProject.version
}

dependencies {
    testImplementation(project(":core"))
    testImplementation(project(":utils"))
    testImplementation(Libs.Okio.okio)
    testImplementation(Libs.MultiArray.multiarray)
    testImplementation(Libs.Klock.klock_jvm)
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

jgitver {
    strategy("MAVEN")
}

tasks.shadowJar {
    minimize()
}

tasks.wrapper {
    gradleVersion = "7.1.1"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
