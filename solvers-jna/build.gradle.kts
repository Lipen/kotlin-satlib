dependencies {
    implementation(project(":core"))
    implementation(project(":jna"))
    implementation(project(":utils"))

    // FIXME: temporary logging
    runtimeOnly(Libs.Log4j.log4j_core)
    runtimeOnly(Libs.Log4j.log4j_slf4j2_impl)
}
