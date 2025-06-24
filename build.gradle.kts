plugins {
    java
    alias(libs.plugins.paperweight) apply false
}

group = "ravioli.gravioli"
version = "1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(rootProject.libs.jetbrains.annotations)
    }
}