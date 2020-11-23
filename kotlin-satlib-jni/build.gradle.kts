plugins {
    id(Plugins.Jmh.id)
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
