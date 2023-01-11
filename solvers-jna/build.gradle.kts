dependencies {
    implementation(project(":core"))
    implementation(project(":jna"))
    implementation(project(":utils"))

    // FIXME: temporary logging
    implementation(Libs.Log4j.log4j_core)
    implementation(Libs.Log4j.log4j_slf4j2_impl)
}
