publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "ravioli-views-api"

            artifact(tasks.sourcesJar)
            artifact(tasks.javadocJar)
        }
    }
}