rootProject.name = "kotlin-satlib"

include("core")
include("jni")
include("utils")
include("jna")
include("solvers-jna")

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
