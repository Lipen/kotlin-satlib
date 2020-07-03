@file:Suppress("PublicApiImplicitType")

object Versions {
    const val kotlin = "1.3.72"
    const val kotlinter = "2.4.1"
    const val gradle_versions = "0.28.0"
    const val jgitver = "0.9.1"
    const val junit = "5.6.1"
    const val kluent = "1.61"
    const val okio = "2.6.0"
    const val multiarray = "0.6.1"
    const val kotlin_jnisat = "0.15.0"
    const val klock = "1.11.12"
    const val coroutines = "1.3.5"
    const val jmh_gradle_plugin = "0.5.0"
    const val jmh = "1.23"
    const val shadow = "5.2.0"
}

object Libs {
    const val junit_jupiter = "org.junit.jupiter:junit-jupiter:${Versions.junit}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val okio = "com.squareup.okio:okio:${Versions.okio}"
    const val multiarray = "com.github.lipen:MultiArray:${Versions.multiarray}"
    const val kotlin_jnisat = "com.github.lipen:kotlin-jnisat:${Versions.kotlin_jnisat}"
    const val klock = "com.soywiz.korlibs.klock:klock-jvm:${Versions.klock}"
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
}
