dependencies {
    api(Libs.Jna.jna)

    // FIXME: temporary logging
    runtimeOnly(Libs.Log4j.log4j_core)
    runtimeOnly(Libs.Log4j.log4j_slf4j2_impl)
}
