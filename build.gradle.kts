plugins {
    java
    alias(libs.plugins.paperweight) apply false
}

allprojects {
    apply(plugin = "java")

    group = "ravioli.gravioli"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(rootProject.libs.jetbrains.annotations)
    }
}