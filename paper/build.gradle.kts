plugins {
    `java-library`
    alias(libs.plugins.paperweight)
    alias(libs.plugins.shadow)
    `maven-publish`
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(projects.core)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.get())
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    reobfJar {
        outputJar.set(file("${layout.buildDirectory}/libs/${project.name}-$version.jar"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "ravioli-views-paper"

            artifact(tasks.reobfJar) {
                classifier = ""
            }

            artifact(tasks.sourcesJar)
            artifact(tasks.javadocJar)
        }
    }
}
