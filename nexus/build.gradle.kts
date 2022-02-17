plugins {
    application
    kotlin("plugin.serialization")
    id(Plugins.Shadow.id)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":utils"))

    implementation(Libs.Okio.okio)
    implementation(Libs.MultiArray.multiarray)
    implementation(Libs.Klock.klock_jvm)
    implementation(Libs.Clikt.clikt)
    implementation(Libs.Mordant.mordant)
    implementation(Libs.Log4j.log4j_slf4j_impl)
    implementation(Libs.KotlinxSerialization.serialization_json)
}

application {
    mainClass.set("com.github.lipen.satlib.nexus.NexusKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8")
}

tasks.withType<JavaExec> {
    args("--help")
}

tasks.startScripts {
    applicationName = "nexus"
}

tasks.jar {
    manifest.attributes("Main-Class" to application.mainClass)
    manifest.attributes("Multi-Release" to true) // needed for log4j
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
    archiveVersion.set("")
    minimize {
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
    }
}
