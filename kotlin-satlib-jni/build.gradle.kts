plugins {
    id(Plugins.Jmh.id)
}

dependencies {
    implementation(project(":utils"))
}

jmh {
    jmhVersion = Versions.jmh
    humanOutputFile = file("$buildDir/reports/jmh/human.txt")
    resultsFile = file("$buildDir/reports/jmh/results.json")
    resultFormat = "JSON"
}
