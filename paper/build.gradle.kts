plugins {
    `java-library`
    alias(libs.plugins.paperweight)
}

group = "ravioli.gravioli"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(projects.core)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.get())
}