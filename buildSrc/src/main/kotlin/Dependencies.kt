@file:Suppress("PublicApiImplicitType")

object Versions {
    const val kotlin = "1.3.61"
    const val ktlint = "8.0.0"
    const val gradle_versions = "0.28.0"
    const val jgitver = "0.8.0"
    const val jmh_gradle_plugin = "0.5.0"
    const val jmh = "1.23"
    const val junit = "5.5.0-M1"
    const val kluent = "1.49"
}

object Libs {
    const val junit_jupiter_api = "org.junit.jupiter:junit-jupiter-api:${Versions.junit}"
    const val junit_jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
}
