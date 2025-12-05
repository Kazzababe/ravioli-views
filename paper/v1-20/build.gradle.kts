plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(projects.paper)
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

