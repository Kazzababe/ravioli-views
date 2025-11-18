import io.papermc.paperweight.util.path
import kotlin.io.path.absolutePathString

plugins {
    `java-library`
    alias(libs.plugins.paperweight)
    alias(libs.plugins.shadow)
    `maven-publish`
}

val adapters by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(projects.core)

    adapters(projects.paper.v120)
    adapters(projects.paper.v121)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.get())
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    reobfJar {
        outputJar.set(file("${layout.buildDirectory.path.absolutePathString()}/libs/${project.name}-${project.version}.jar"))
    }

    shadowJar {
        dependsOn(adapters)

        from(adapters.resolve().map { file ->
            if (file.isDirectory) {
                file
            } else {
                zipTree(file)
            }
        })
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
