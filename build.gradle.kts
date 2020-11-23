import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.lipen"

plugins {
    idea
    kotlin("jvm") version Versions.kotlin
    id("org.jmailen.kotlinter") version Versions.kotlinter
    id("fr.brouillard.oss.gradle.jgitver") version Versions.jgitver
    `maven-publish`
    id("com.github.ben-manes.versions") version Versions.gradle_versions
    id("com.github.johnrengelman.shadow") version Versions.shadow
    id("me.champeau.gradle.jmh") version Versions.jmh_gradle_plugin apply false
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jmailen.kotlinter")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        jcenter()
        maven(url = "https://jitpack.io")
    }

    dependencies {
        implementation(platform(kotlin("bom")))
        implementation(kotlin("stdlib-jdk8"))
        implementation(Libs.kotlin_logging)
        implementation(Libs.log4j_slf4j)

        testImplementation(Libs.junit_jupiter)
        testImplementation(Libs.kluent)
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

    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifact(sourcesJar)
            }
        }
        repositories {
            maven(url = "$buildDir/repository")
        }
    }
}

configure(allprojects - project(":utils")) {
    dependencies {
        implementation(project(":utils"))
    }
}

subprojects {
    group = "${rootProject.group}.${rootProject.name}"
    version = rootProject.version
}

dependencies {
    api(project(":core"))

    testImplementation(Libs.okio)
    testImplementation(Libs.multiarray)
    testImplementation(Libs.klock)
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
    gradleVersion = "6.7.1"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
