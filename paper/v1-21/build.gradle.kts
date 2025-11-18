plugins {
    `java-library`
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(projects.paper)
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

