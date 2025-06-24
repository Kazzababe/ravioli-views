plugins {
    alias(libs.plugins.paperweight)
    alias(libs.plugins.shadow)
}

group = "ravioli.gravioli"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(projects.paper)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.get())
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    assemble {
        dependsOn("shadowJar")
    }
}