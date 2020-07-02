rootProject.name = "kotlin-satlib"

plugins {
    id("com.gradle.enterprise") version "3.3.4"
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
myInclude("jni")
