import de.undercouch.gradle.tasks.download.DownloadAction
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
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    implementation(Libs.okio)
    implementation(Libs.multiarray)
    implementation(Libs.kotlin_jnisat)
    implementation(Libs.coroutines)

    testImplementation(Libs.junit_jupiter)
    testImplementation(Libs.kluent)
    testImplementation(Libs.klock)
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

fun Task.download(action: DownloadAction.() -> Unit) =
    download.configure(delegateClosureOf(action))

val osArch: String = run {
    val osName = System.getProperty("os.name")
    val os = when {
        osName.startsWith("Linux") -> "linux"
        osName.startsWith("Windows") -> "win"
        osName.startsWith("Mac OS X") || osName.startsWith("Darwin") -> "osx"
        else -> return@run "unknown"
    }
    val arch = when (System.getProperty("os.arch")) {
        "x86", "i386" -> "32"
        "x86_64", "amd64" -> "64"
        else -> return@run "unknown"
    }
    "$os$arch"
}

tasks.register("downloadLibs") {
    doLast {
        val urlTemplate = "https://github.com/Lipen/kotlin-jnisat/releases/download/${Versions.kotlin_jnisat}/%s"
        val libDir = projectDir.resolve("src/main/resources/lib/$osArch")
            .also { it.mkdirs() }
            .also { check(it.exists()) { "'$it' does not exist" } }

        when (osArch) {
            "linux64" -> {
                for (name in listOf("libjminisat.so", "libjcadical.so", "libjcms.so")) {
                    download {
                        src(urlTemplate.format(name))
                        dest(libDir)
                    }
                }
                val solverLibDir = projectDir.resolve("libs")
                    .also { it.mkdirs() }
                    .also { check(it.exists()) { "'$it' does not exist" } }
                for (name in listOf("libminisat.so", "libcadical.so", "libcryptominisat5.so")) {
                    download {
                        src(urlTemplate.format(name))
                        dest(solverLibDir)
                    }
                }
            }
            "win64" -> {
                download {
                    src(urlTemplate.format("jminisat.dll"))
                    dest(libDir)
                }
                download {
                    src(urlTemplate.format("minisat.dll"))
                    dest(projectDir)
                }
            }
            else -> {
                error("$osArch is unsupported")
            }
        }
    }
}

tasks.wrapper {
    gradleVersion = "6.3"
    distributionType = Wrapper.DistributionType.ALL
}

defaultTasks("clean", "build")
