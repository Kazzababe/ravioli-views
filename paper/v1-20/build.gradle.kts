plugins {
    `java-library`
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(projects.paper)
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

