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
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.github.Kazzababe.ravioli-views"
            artifactId = "paper"

            artifact(tasks.reobfJar) {
                classifier = ""
            }
        }
    }

    repositories {
        mavenLocal()
    }
}