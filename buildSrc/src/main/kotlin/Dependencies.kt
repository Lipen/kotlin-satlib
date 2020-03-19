@file:Suppress("PublicApiImplicitType")

object Versions {
    const val kotlin = "1.3.70"
    const val kotlinter = "2.3.2"
    const val gradle_versions = "0.28.0"
    const val jgitver = "0.9.1"
    const val jmh_gradle_plugin = "0.5.0"
    const val jmh = "1.23"
    const val junit = "5.6.0"
    const val kluent = "1.60"
    const val okio = "2.4.3"
    const val multiarray = "0.6.1"
    const val kotlin_jnisat = "0.8.0"
}

object Libs {
    const val junit_jupiter_api = "org.junit.jupiter:junit-jupiter-api:${Versions.junit}"
    const val junit_jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val okio = "com.squareup.okio:okio:${Versions.okio}"
    const val multiarray = "com.github.Lipen:MultiArray:${Versions.multiarray}"
    const val kotlin_jnisat = "com.github.Lipen:kotlin-jnisat:${Versions.kotlin_jnisat}"
}
