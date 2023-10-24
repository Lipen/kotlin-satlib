plugins {
    id(Plugins.Jmh.id)
}

dependencies {
    implementation(project(":utils"))
}

jmh {
    jmhVersion = Versions.jmh
    humanOutputFile = layout.buildDirectory.file("reports/jmh/human.txt").get().asFile
    resultsFile = layout.buildDirectory.file("reports/jmh/results.json").get().asFile
    resultFormat = "JSON"
}
