import io.github.liurenjie1024.gradle.rust.CargoBuildTask

plugins {
    id(Plugins.Jmh.id)
    id(Plugins.Rust.id)
}

dependencies {
    implementation(project(":utils"))
}

jmh {
    jmhVersion = Versions.jmh
    humanOutputFile = project.file("${project.buildDir}/reports/jmh/human.txt")
    resultsFile = project.file("${project.buildDir}/reports/jmh/results.json")
    resultFormat = "JSON"
}

tasks.withType(CargoBuildTask::class.java).configureEach {
    // verbose = true
    release = true
}

tasks.register("copyRustLibDllToResources") {
    doLast {
        val libName = "jsplr.dll"
        val src = projectDir.resolve("target/release").resolve(libName)
        val dest = projectDir.resolve("src/main/resources/lib/win64").resolve(libName)
        if (dest.exists()) {
            dest.delete()
        }
        println("Copying $src to $dest...")
        src.copyTo(dest, overwrite = true)
        println("Done copying rust lib dll.")
    }
}
