plugins {
    `java-library`
}

dependencies {
    api(projects.api)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "ravioli-views-core"

            artifact(tasks.sourcesJar)
            artifact(tasks.javadocJar)
        }
    }
}