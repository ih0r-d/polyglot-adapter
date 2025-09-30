# polyglot-adapter-core
![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=java)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![GraalPy](https://img.shields.io/badge/GraalPy-25.x-3776AB?logo=python)
![Tests](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)
---

## 📑 Table of Contents
- [Badges](#-badges)
- [Overview](#-overview)
- [Key Features](#-key-features)
- [Modules and Main Types](#-modules-and-main-types)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [How It Works](#-how-it-works)
- [Configuration](#-configuration)
- [Error Handling](#-error-handling)
- [Roadmap](#-roadmap)
- [Examples](#-examples)
- [Development Workflow](#-development-workflow-taskfile--scripts)
- [References](#-references)
- [License](#-license)

---

## 📖 Overview
**polyglot-adapter** is a lightweight Java library that provides a unified, high-level API for executing code across languages via GraalVM, with a current focus on Python (GraalPy 25.x).  
It abstracts polyglot context creation and script execution behind a simple adapter interface.

---

## ✨ Key Features
- Unified API via `PolyglotAdapter`
- Python execution through GraalPy
- Pluggable executors (`BaseExecutor`) for future languages
- Context factory with safe defaults and custom configuration
- Utility helpers for reflection and resource handling

---

## 🧩 Modules and Main Types
- `io.github.ih0rd.adapter.api.PolyglotAdapter`: High-level facade
- `io.github.ih0rd.adapter.api.executors.PyExecutor`: Python executor (GraalPy)
- `io.github.ih0rd.adapter.api.executors.BaseExecutor`: Contract for executors
- `io.github.ih0rd.adapter.api.context.PolyglotContextFactory`: Context builder
- `io.github.ih0rd.adapter.api.context.Language`: Supported languages enum
- `io.github.ih0rd.adapter.api.context.ResourcesProvider`: Resource path resolver
- Utilities: `CommonUtils`, `StringCaseConverter`
- Exceptions: `EvaluationException`

---

## ⚙️ Requirements
- JDK 25+
- Maven 3.9+
- GraalVM 25.x with Python (`gu install python`)

---

## 📦 Installation
```xml
<dependency>
  <groupId>io.github.ih0r</groupId>
  <artifactId>polyglot-adapter</artifactId>
  <version>0.0.1</version>
</dependency>
```

---

## 🔍 How It Works
1. `PyExecutor` maps Java interface → Python file (camelCase → snake_case).
2. Loads from classpath `resources/python/<file>.py` or fallback path:
    - Default: `${user.dir}/src/main/python`
    - Overridable: `-Dpolyglot.py.polyglot-resources.path=/custom/path`
3. Creates `Context` via `PolyglotContextFactory(Language.PYTHON)`.
4. Instantiates Python class and binds to Java interface.
5. Invokes methods using reflection helpers.

---

## 🛠 Configuration
- Exposed flags via `PolyglotContextFactory.Builder`:
    - `allowAllAccess`, `HostAccess`, `IOAccess`, `allowCreateThread`
    - `allowNativeAccess`, `allowExperimentalOptions`, `PolyglotAccess`
- Override Python resources path:
  ```bash
  -Dpolyglot.py.polyglot-resources.path=/absolute/path/to/python
  ```

---

## ❗ Error Handling
- All runtime issues wrapped in `EvaluationException`:
    - Python file not found
    - Java interface ↔ Python class mismatch
    - Missing method

---

## 🗺 Roadmap
- Executors for JS, R, etc.
- Optional GraalPy integration tests
- Better camelCase → snake_case for acronyms

---

## 🛠 Development Workflow (Taskfile + Scripts)

Project includes [Taskfile](./Taskfile.yml) and helper scripts in [`scripts/`](./scripts).

### Main tasks
- `task clean` — clean build outputs
- `task build` — package project
- `task test` — run tests
- `task verify` — tests + checkstyle
- `task release VERSION=0.0.X` — full release flow
- `task bump TYPE=patch|minor|major` — bump to next snapshot
- `task check` — run tests + checkstyle (quiet unless error)
- `task format` — apply code formatting
- `task clean-remote-tags` — delete remote git tags not present locally

### Logging
- Maven output silenced (quiet mode)
- Only project-specific logs shown
- On error → Maven error logs displayed

---
## 💡 Examples
See [examples](./examples):
- [Java Example](./examples/java-example) — demonstrates Python integration with GraalPy and `PolyglotAdapter`.
---

## 📚 References
- [GraalVM](https://www.graalvm.org/)
- [GraalPy](https://www.graalvm.org/python/)
- [Polyglot Reference](https://www.graalvm.org/reference-manual/polyglot/)

---

## 📜 License
Apache License 2.0 — see [LICENSE](./LICENSE)
