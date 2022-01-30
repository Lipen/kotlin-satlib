dependencies {
    api(project(":jni"))
    implementation(project(":utils"))

    implementation(Libs.Okio.okio)
    implementation(Libs.MultiArray.multiarray)
    implementation(Libs.KotlinxCoroutines.kotlinx_coroutines_core)

    testImplementation(Libs.Klock.klock_jvm)
}
