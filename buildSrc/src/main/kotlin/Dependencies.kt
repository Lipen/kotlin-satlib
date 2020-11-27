@file:Suppress("PublicApiImplicitType", "MemberVisibilityCanBePrivate")

object Versions {
    const val gradle_versions = "0.33.0"
    const val jgitver = "0.9.1"
    const val jmh = "1.23"
    const val jmh_gradle_plugin = "0.5.0"
    const val junit = "5.7.0"
    const val klock = "1.12.1"
    const val kluent = "1.63"
    const val kotlin = "1.4.20"
    const val kotlin_logging = "2.0.3"
    const val kotlinter = "3.2.0"
    const val kotlinx_coroutines = "1.4.1"
    const val log4j = "2.14.0"
    const val multiarray = "0.6.1"
    const val okio = "2.9.0"
    const val shadow = "5.2.0"
}

object Libs {
    // https://github.com/junit-team/junit5
    object JUnit {
        const val version = Versions.junit
        const val jupiter_api = "org.junit.jupiter:junit-jupiter-api:$version"
        const val jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:$version"
        const val jupiter_params = "org.junit.jupiter:junit-jupiter-params:$version"
    }

    // https://github.com/MarkusAmshove/Kluent
    object Kluent {
        const val version = Versions.kluent
        const val kluent = "org.amshove.kluent:kluent:$version"
    }

    // https://github.com/korlibs/klock
    object Klock {
        const val version = Versions.klock
        const val klock_jvm = "com.soywiz.korlibs.klock:klock-jvm:$version"
    }

    // https://github.com/square/okio
    object Okio {
        const val version = Versions.okio
        const val okio = "com.squareup.okio:okio:$version"
    }

    // https://github.com/Lipen/MultiArray
    object MultiArray {
        const val version = Versions.multiarray
        const val multiarray = "com.github.lipen:multiarray:$version"
    }

    // https://github.com/MicroUtils/kotlin-logging
    object KotlinLogging {
        const val version = Versions.kotlin_logging
        const val kotlin_logging = "io.github.microutils:kotlin-logging:$version"
    }

    // https://github.com/apache/logging-log4j2
    object Log4j {
        const val version = Versions.log4j
        const val log4j_slf4j_impl = "org.apache.logging.log4j:log4j-slf4j-impl:$version"
    }

    // https://github.com/Kotlin/kotlinx.coroutines
    object KotlinxCoroutines {
        const val version = Versions.kotlinx_coroutines
        const val kotlinx_coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
    }
}

object Plugins {
    // https://github.com/jgitver/gradle-jgitver-plugin
    object Jgitver {
        const val version = Versions.jgitver
        const val id = "fr.brouillard.oss.gradle.jgitver"
    }

    // https://github.com/ben-manes/gradle-versions-plugin
    object GradleVersions {
        const val version = Versions.gradle_versions
        const val id = "com.github.ben-manes.versions"
    }

    // https://github.com/JLLeitschuh/ktlint-gradle
    object Kotlinter {
        const val version = Versions.kotlinter
        const val id = "org.jmailen.kotlinter"
    }

    // https://github.com/johnrengelman/shadow
    object Shadow {
        const val version = Versions.shadow
        const val id = "com.github.johnrengelman.shadow"
    }

    // https://github.com/melix/jmh-gradle-plugin
    object Jmh {
        const val version = Versions.jmh_gradle_plugin
        const val id = "me.champeau.gradle.jmh"
    }
}
