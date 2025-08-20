# Ravioli Views  ![Maven Central](https://img.shields.io/maven-central/v/dev.mckelle/ravioli-views-core)

Ravioli Views is a small framework for building inventory‑based UIs in Paper/Spigot plugins.
It exposes a React‑like component model, a diff engine, and lifecycle hooks so you can describe an interface as simple
Java objects instead of slot‑by‑slot code.

For detailed guides, tutorials, and API references, please see the [**wiki
**](https://github.com/Kazzababe/ravioli-views/wiki).

---

## 📚 Modules

| Module    | Coordinates                       | What it contains                                                               |
|-----------|-----------------------------------|--------------------------------------------------------------------------------|
| **API**   | `dev.mckelle:ravioli-views-api`   | Public interfaces, annotations, and functional types. Stable across versions.  |
| **Core**  | `dev.mckelle:ravioli-views-core`  | Diff engine, hooks, state management, and shared utilities.                    |
| **Paper** | `dev.mckelle:ravioli-views-paper` | Paper‑specific event listeners and helpers that wire the Core into the server. |

*(All modules are available on Maven Central—badge above always shows the latest released version.)*

---

## 📦 Installation

### Gradle • Kotlin DSL

```kotlin
repositories {
    mavenCentral()
}

// Only the platform specific dependency is required
dependencies {
    implementation("dev.mckelle:ravioli-views-paper:1.0.0")
}
```

### Gradle • Groovy DSL

```groovy
repositories {
    mavenCentral()
}

// Only the platform specific dependency is required
dependencies {
    implementation 'dev.mckelle:ravioli-views-paper:1.0.0'
}
```

### Maven

```xml
<!-- Only the platform specific dependency is required -->
<dependency>
    <groupId>dev.mckelle</groupId>
    <artifactId>ravioli-views-paper</artifactId>
    <version>1.0.0</version>
</dependency>
```

> **Tip** The version in the snippets should match the number in the badge above.

---

## 🛠️ Example usage

See the **example** modules within each platform module.

- **Paper** - [`Example Module`](./paper/example)

---

## 📄 License

Ravioli Views is released under the MIT License — see [LICENSE](https://opensource.org/license/mit) for details.
