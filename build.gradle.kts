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
    apply(plugin = "fr.brouillard.oss.gradle.jgitver")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        maven(url = "https://jitpack.io")
        jcenter()
    }

    dependencies {
        implementation(platform(kotlin("bom")))
        implementation(kotlin("stdlib-jdk8"))

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

    jgitver {
        strategy("MAVEN")
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

subprojects {
    group = "${rootProject.group}.${rootProject.name}"
}

dependencies {
    api(project(":core"))
    implementation(project("utils"))

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

tasks.shadowJar {
    minimize()
}

tasks.wrapper {
    gradleVersion = "6.5"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
