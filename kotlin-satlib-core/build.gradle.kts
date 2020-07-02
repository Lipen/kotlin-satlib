dependencies {
    api(project(":jni"))

    implementation(Libs.okio)
    implementation(Libs.multiarray)
    implementation(Libs.coroutines)

    testImplementation(Libs.klock)
}
