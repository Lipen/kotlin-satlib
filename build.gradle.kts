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

fun Project.configureKotlinConventions() {
    apply(plugin = "kotlin")
    apply(plugin = Plugins.Kotlinter.id)
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    dependencies {
        implementation(platform(kotlin("bom")))
        implementation(kotlin("stdlib-jdk8"))

        implementation(Libs.KotlinLogging.kotlin_logging)

        testImplementation(Libs.JUnit.jupiter_api)
        testRuntimeOnly(Libs.JUnit.jupiter_engine)
        testImplementation(Libs.JUnit.jupiter_params)
        testImplementation(Libs.Kluent.kluent)
        testImplementation(Libs.Log4j.log4j_core)
        testImplementation(Libs.Log4j.log4j_slf4j2_impl)
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
        withSourcesJar()
        withJavadocJar()
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

allprojects {
    configureKotlinConventions()
}

subprojects {
    group = "${rootProject.group}.${rootProject.name}"
    version = rootProject.version
}

dependencies {
    api(project(":core"))
    api(project(":jni"))
    api(project(":solvers-jni"))
    api(project(":jna"))
    api(project(":solvers-jna"))
    implementation(project(":utils"))
    implementation(project(":tests-utils"))

    // // FIXME: temporary logging
    // runtimeOnly(Libs.Log4j.log4j_core)
    // runtimeOnly(Libs.Log4j.log4j_slf4j2_impl)

    testImplementation(Libs.Klock.klock_jvm)
    testImplementation(Libs.Okio.okio)
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
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
