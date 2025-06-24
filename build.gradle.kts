plugins {
    java
    alias(libs.plugins.paperweight) apply false
}

allprojects {
    apply(plugin = "java")

    group = "ravioli.gravioli"
    version = "0.9.0"

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(rootProject.libs.jetbrains.annotations)
    }
}