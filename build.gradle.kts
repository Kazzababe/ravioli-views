plugins {
    java
    `maven-publish`
    alias(libs.plugins.paperweight) apply false
}

allprojects {
    group = "ravioli.gravioli"
    version = "0.9.3"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(rootProject.libs.jetbrains.annotations)
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }
}