# polyglot-adapter-core

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=openjdk)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![Tests](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)
![Renovate](https://img.shields.io/badge/dependencies-renovate-1f8ceb?logo=renovatebot)

---

## 📑 Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Project Structure](#project-structure)
- [Core Components](#core-components)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Development](#development)
- [Automation](#automation)
- [License](#license)

---

## Overview

**polyglot-adapter-core** is a lightweight SDK that provides a unified API for executing multi-language code via **GraalVM Polyglot**.  
Currently supports **Python (GraalPy)** and **JavaScript (GraalJS)**.

---

## Features
- Unified high-level API: `PolyglotAdapter`
- GraalPy and GraalJS executors (`PyExecutor`, `JsExecutor`)
- Type-safe execution model (`EvalResult<T>`)
- Sandbox-based context creation with access control
- Lightweight, dependency-free core (no Spring, no Guice)

---

## Project Structure

```
.
├── polyglot-adapter-core/
│   ├── pom.xml
│   └── src/
│       ├── main/java/io/github/ih0rd/adapter/...
│       └── test/java/io/github/ih0rd/adapter/...
├── examples/java-example/           # usage examples
├── scripts/                         # automation utilities
│   ├── bump.sh
│   ├── release.sh
│   └── clean-remote-tags.sh
├── Taskfile.yaml                    # unified build/test tasks
├── renovate.json                    # dependency management config
├── pom.xml
└── README.md
```

---

## Core Components

| Component                             | Description                                              |
|---------------------------------------|----------------------------------------------------------|
| **PolyglotAdapter**                   | Facade for evaluating language-specific implementations. |
| **BaseExecutor**                      | Common executor interface for any supported language.    |
| **PyExecutor / JsExecutor**           | Executors for Python and JavaScript.                     |
| **PolyglotContextFactory**            | Creates isolated GraalVM contexts.                       |
| **EvalResult**                        | Immutable record holding result and type info.           |
| **ResourcesProvider**                 | Resolves language resource directories.                  |
| **EvaluationException**               | Unified runtime exception wrapper.                       |
| **CommonUtils / StringCaseConverter** | Reflection and name utilities.                           |

---

## Requirements
- **JDK 25+**
- **Maven 3.9+**
- **GraalVM 25.x+**
- Installed Python for GraalVM:
  ```bash
  gu install python
  ```

---

## Installation

Add to your Maven project:

```xml
<dependency>
  <groupId>io.github.ih0rd</groupId>
  <artifactId>polyglot-adapter-core</artifactId>
  <version>0.0.10</version>
</dependency>
```

---

## Usage

Full examples are available under [`examples/java-example`](./examples/java-example).

```shell
#java
try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
    EvalResult<Integer> sum = adapter.evaluate("add", MyApi.class, 10, 5);
    System.out.println(sum.value()); // → 15
}

#Python implementation:
class MyApi:
    def add(self, a: int, b: int) -> int:
        return a + b
```

---

## Configuration

| Property                              | Description                        | Default           |
|---------------------------------------|------------------------------------|-------------------|
| `polyglot.py.polyglot-resources.path` | Root path for Python modules       | `src/main/python` |
| `allowAllAccess`                      | Enables full host access           | `true`            |
| `allowNativeAccess`                   | Enables native interop             | `true`            |
| `allowExperimentalOptions`            | Enables experimental Graal options | `true`            |

---

## Error Handling

All runtime exceptions are wrapped in `EvaluationException`.

| Error                   | Description                           |
|-------------------------|---------------------------------------|
| `Missing resource file` | Python/JS module not found            |
| `No matching method`    | Java interface method not implemented |
| `Invocation failed`     | Underlying Graal or reflection error  |

---

## Development

Unified build/test/release via [Taskfile](./Taskfile.yaml):

| Command                      | Description                    |
|------------------------------|--------------------------------|
| `task clean`                 | Clean build artifacts          |
| `task build`                 | Build the project              |
| `task test`                  | Run tests                      |
| `task release VERSION=X.Y.Z` | Tag and release version        |
| `task bump TYPE=patch`       | Bump patch/minor/major version |

---

## Automation

| Tool               | Purpose                                      |
|--------------------|----------------------------------------------|
| **GitHub Actions** | CI/CD: build, test, release                  |
| **Renovate Bot**   | Automated dependency update PRs              |
| **Taskfile.dev**   | Unified commands for build/test/release      |
| **Shell Scripts**  | Silent release & bump utilities (`scripts/`) |

---

## License

Licensed under the **Apache License 2.0**. See [LICENSE](./LICENSE).
