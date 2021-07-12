rootProject.name = "kotlin-satlib"

plugins {
    id("com.gradle.enterprise") version "3.6.3"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

include("lib")
include("jni")
include("utils")
