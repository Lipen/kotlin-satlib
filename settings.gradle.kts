rootProject.name = "kotlin-satlib"

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

fun myInclude(name:String) {
    include(name)
    project(":$name").projectDir = file("${rootProject.name}-$name")
}

myInclude("core")
myInclude("utils")
myInclude("jni")
myInclude("solvers-jni")
myInclude("jna")
myInclude("solvers-jna")
include("tests-utils")
