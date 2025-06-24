plugins {
    `java-library`
    alias(libs.plugins.paperweight)
}

dependencies {
    api(projects.api)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.get())
}