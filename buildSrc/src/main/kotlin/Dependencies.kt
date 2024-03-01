@file:Suppress("PublicApiImplicitType", "MemberVisibilityCanBePrivate", "unused", "ConstPropertyName")

object Versions {
    const val jgitver = "0.9.1"
    const val jmh = "1.23"
    const val jmh_gradle_plugin = "0.5.3"
    const val jna = "5.14.0"
    const val junit = "5.10.2"
    const val klock = "4.0.10"
    const val kluent = "1.73"
    const val kotlin = "1.9.22"
    const val kotlin_logging = "6.0.3"
    const val kotlinx_coroutines = "1.8.0"
    const val log4j = "2.23.0"
    const val multiarray = "0.13.1"
    const val okio = "3.8.0"
    const val shadow = "8.1.1"
    const val slf4j = "2.0.12"
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
        const val klock = "com.soywiz.korlibs.klock:klock:$version"
    }

    // https://github.com/square/okio
    object Okio {
        const val version = Versions.okio
        const val okio = "com.squareup.okio:okio:$version"
    }

    // https://github.com/Lipen/MultiArray
    object MultiArray {
        const val version = Versions.multiarray
        const val multiarray = "com.github.Lipen:MultiArray:$version"
    }

    // https://github.com/oshai/kotlin-logging/
    object KotlinLogging {
        const val version = Versions.kotlin_logging
        const val kotlin_logging = "io.github.oshai:kotlin-logging:$version"
    }

    // https://github.com/apache/logging-log4j2
    object Log4j {
        const val version = Versions.log4j
        const val log4j_core = "org.apache.logging.log4j:log4j-core:$version"
        const val log4j_slf4j2_impl = "org.apache.logging.log4j:log4j-slf4j2-impl:$version"
    }

    // https://github.com/Kotlin/kotlinx.coroutines
    object KotlinxCoroutines {
        const val version = Versions.kotlinx_coroutines
        const val kotlinx_coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
    }

    // https://github.com/java-native-access/jna
    object Jna {
        const val version = Versions.jna
        const val jna = "net.java.dev.jna:jna:$version"
    }

    // https://github.com/qos-ch/slf4j
    object Slf4j {
        const val version = Versions.slf4j
        const val slf4j_simple = "org.slf4j:slf4j-simple:$version"
    }
}

object Plugins {
    // https://github.com/jgitver/gradle-jgitver-plugin
    object Jgitver {
        const val version = Versions.jgitver
        const val id = "fr.brouillard.oss.gradle.jgitver"
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
