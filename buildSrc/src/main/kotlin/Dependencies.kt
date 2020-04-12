@file:Suppress("PublicApiImplicitType")

object Versions {
    const val kotlin = "1.3.71"
    const val kotlinter = "2.3.2"
    const val gradle_versions = "0.28.0"
    const val jgitver = "0.9.1"
    const val gradle_download = "4.0.4"
    const val junit = "5.6.1"
    const val kluent = "1.60"
    const val okio = "2.5.0"
    const val multiarray = "0.6.1"
    const val kotlin_jnisat = "0.12.1"
    const val klock = "1.8.6"
    const val coroutines = "1.3.5"
}

object Libs {
    const val junit_jupiter_api = "org.junit.jupiter:junit-jupiter-api:${Versions.junit}"
    const val junit_jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val okio = "com.squareup.okio:okio:${Versions.okio}"
    const val multiarray = "com.github.lipen:MultiArray:${Versions.multiarray}"
    const val kotlin_jnisat = "com.github.lipen:kotlin-jnisat:${Versions.kotlin_jnisat}"
    const val klock = "com.soywiz.korlibs.klock:klock-jvm:${Versions.klock}"
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
}
