import org.jreleaser.model.Active

plugins {
    java
    `maven-publish`
    id("signing")
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.paperweight) apply false
}

allprojects {
    group = "dev.mckelle"
    version = "1.1.9"
}

val projectName = "Ravioli Views"
val projectDescription =
    "A modern, React-inspired GUI library for Paper/Spigot servers that uses a declarative, component-based architecture with hooks to simplify the creation of complex, reactive inventory UIs."
val projectUrl = "https://github.com/Kazzababe/ravioli-views"
val projectScmUrl = "scm:git:git://github.com/Kazzababe/ravioli-views.git"
val projectScmConnection = "scm:git:ssh://git@github.com/Kazzababe/ravioli-views.git"
val projectRepoUrl = "https://github.com/Kazzababe"

val authorAlias = "Kazzababe"
val authorName = "Mckelle Gravelle"
val authorEmail = "GravelleMckelle@gmail.com"

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

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

    publishing {
        repositories {
            mavenLocal()

            maven {
                url = uri(rootProject.layout.projectDirectory.dir("target/staging-deploy"))
            }
        }

        publications.withType<MavenPublication> {
            pom {
                name.set(projectName)
                description.set(projectDescription)
                url.set(projectUrl)

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/license/mit")
                    }
                }
                developers {
                    developer {
                        id.set(authorAlias)
                        name.set(authorName)
                        email.set(authorEmail)
                        organizationUrl.set(projectRepoUrl)
                    }
                }
                scm {
                    connection.set(projectScmUrl)
                    developerConnection.set(projectScmConnection)
                    url.set(projectUrl)
                }
            }
        }
    }

    signing {
        useGpgCmd()

        sign(publishing.publications)
    }

    tasks.withType<PublishToMavenRepository> {
        val reobfJarTask = tasks.findByName("reobfJar")

        if (reobfJarTask != null) {
            dependsOn(reobfJarTask)
        }
    }
}

jreleaser {
    assemble {
        active.set(Active.ALWAYS)

        javaArchive {
            register("api") {
                extraProperties.put("skipRelease", "true")
            }

            register("core") {
                extraProperties.put("skipRelease", "true")
            }
        }
    }

    release {
        github {
            repoOwner.set(authorAlias)
            tagName.set("v${project.version}")
            releaseName.set("Ravioli Views ${project.version}")
            overwrite.set(true)
            uploadAssets.set(Active.RELEASE)
        }
    }

    project {
        name.set(projectName)
        description.set(projectDescription)
        authors.set(listOf(authorName))
        inceptionYear.set("2025")
        license.set("MIT")

        links {
            homepage.set("https://github.com/Kazzabe/ravioli-views")
        }
    }

    signing {
        active.set(Active.NEVER)
    }

    deploy {
        maven {
            mavenCentral {
                register("sonatype") {
                    sign.set(false)
                    active.set(Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")

                    username.set(providers.gradleProperty("ossrhUsername"))
                    password.set(providers.gradleProperty("ossrhPassword"))

                    stagingRepository("target/staging-deploy")
                }
            }
        }
    }
}

tasks.named<Delete>("clean") {
    delete(fileTree("target/staging-deploy"))
}