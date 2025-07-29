plugins {
    `java-library`
    alias(libs.plugins.paperweight)
}

dependencies {
    api(projects.api)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.github.Kazzababe.ravioli-views"
            artifactId = "core"

            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}