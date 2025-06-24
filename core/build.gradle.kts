plugins {
    `java-library`
    alias(libs.plugins.paperweight)
}

group = "ravioli.gravioli"
version = "1.0-SNAPSHOT"

dependencies {
    api(projects.api)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.get())
}