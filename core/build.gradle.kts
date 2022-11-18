dependencies {
    api(project(":jni"))
    implementation(project(":utils"))

    api(Libs.MultiArray.multiarray)
    implementation(Libs.KotlinLogging.kotlin_logging)
    implementation(Libs.Okio.okio)
    implementation(Libs.KotlinxCoroutines.kotlinx_coroutines_core)

    testImplementation(Libs.Klock.klock_jvm)
}
