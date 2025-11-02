# polyglot-adapter

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=openjdk)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![Tests](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ih0r-d/polyglot-adapter.svg?label=maven%20central)](https://central.sonatype.com/artifact/io.github.ih0r-d/polyglot-adapter)
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
- [Language Runtimes](#language-runtimes)
- [Usage](#usage)
- [Configuration](#configuration)
- [Error Handling](#error-handling)
- [Development](#development)
- [Automation](#automation)
- [License](#license)

---

## Overview

**polyglot-adapter** is a lightweight SDK providing an **executor-based bind API** for executing multi-language code via **GraalVM Polyglot**.
It supports **Python (GraalPy)** and **JavaScript (GraalJS)** as optional language runtimes.

---

## Features
- Executor-based bind API (no reflection on core side)
- Unified `BaseExecutor` interface
- Python and JavaScript executors (`PyExecutor`, `JsExecutor`)
- Type-safe `EvalResult<T>` model
- Context isolation via `PolyglotContextFactory`
- Lightweight, dependency-free core (no Spring / Guice)
- Designed for modular extensions

---

## Project Structure

```
src/
└── main/java/io/github/ih0rd/adapter/
    ├── api/
    │   ├── context/
    │   │   ├── EvalResult.java
    │   │   ├── Language.java
    │   │   ├── PolyglotContextFactory.java
    │   │   └── ResourcesProvider.java
    │   └── executors/
    │       ├── BaseExecutor.java
    │       ├── JsExecutor.java
    │       └── PyExecutor.java
    ├── exceptions/
    │   └── EvaluationException.java
    └── utils/
        ├── CommonUtils.java
        ├── Constants.java
        ├── StringCaseConverter.java
        └── ValueUnwrapper.java
```

---

## Core Components

| Component                             | Description                                           |
|---------------------------------------|-------------------------------------------------------|
| **BaseExecutor**                      | Common interface for all executors.                   |
| **PyExecutor / JsExecutor**           | Bind-based executors for Python and JavaScript.       |
| **PolyglotContextFactory**            | Creates sandboxed GraalVM contexts.                   |
| **EvalResult**                        | Immutable record holding result and type information. |
| **ResourcesProvider**                 | Resolves language resource directories.               |
| **EvaluationException**               | Unified runtime exception wrapper.                    |
| **CommonUtils / StringCaseConverter** | Utilities for reflection and name formatting.         |

---

## Requirements
- **JDK 25+**
- **Maven 3.9+**
- **GraalVM 25.x+**
- For Python:
  ```bash
  gu install python
  ```
- For JavaScript:
  ```bash
  gu install js
  ```

---

## Installation

```xml
<dependency>
  <groupId>io.github.ih0r-d</groupId>
  <artifactId>polyglot-adapter</artifactId>
  <version>0.0.15</version>
</dependency>
```

---

## Language Runtimes

These runtimes are **optional** and should be added only when needed.

### 🐍 Python
```xml
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-launcher</artifactId>
  <version>25.0.1</version>
</dependency>
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-embedding</artifactId>
  <version>25.0.1</version>
</dependency>
```

### 🕸 JavaScript
```xml
<dependency>
  <groupId>org.graalvm.js</groupId>
  <artifactId>js</artifactId>
  <version>25.0.1</version>
  <type>pom</type>
</dependency>
```

---

## Usage

Example using **executor-based bind API**:

```java
import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.executors.PyExecutor;

public class Example {
    void main() {
        
        var ctxBuilder = new PolyglotContextFactory.Builder(Language.PYTHON)
                .allowExperimentalOptions(true)
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .withSafePythonDefaults();

        try (var executor = PyExecutor.create(ctxBuilder)) {
            LibrariesApi api = executor.bind(LibrariesApi.class);

            var genUsers = api.genUsers(10);
            IO.println("genUsers → " + genUsers);

            var formatUsers = api.formatUsers(10);
            IO.println("formatUsers → " + formatUsers);

            var fakeParagraphs = api.fakeParagraphs(10);
            IO.println("fakeParagraphs → " + fakeParagraphs);
        }
    }
}

// Interface in Java
public interface LibrariesApi {

    List<Map<String, Object>> genUsers(int n);

    String formatUsers(int n);

    String fakeParagraphs(int n);
}
```

```shell
class LibrariesApi:
  def __init__(self):
    self.fake = Faker()

  def genUsers(self, n: int = 5):
    return [
      {"name": self.fake.name(), "email": self.fake.email(), "country": self.fake.country()}
      for _ in range(n)
    ]

  def formatUsers(self, n: int = 5) -> str:
    users = self.genUsers(n)
    data = [[u["name"], u["email"], u["country"]] for u in users]
    return tabulate(data, headers=["Name", "Email", "Country"], tablefmt="grid")

  def fakeParagraphs(self, n: int = 3) -> str:
    return "\n\n".join(self.fake.paragraph() for _ in range(n))

polyglot.export_value("LibrariesApi", LibrariesApi)

```

---

## Configuration

The `PolyglotContextFactory` provides a fluent builder for fine-tuning GraalVM `Context` behavior.  
Below are the most relevant configuration options.

| Method / Option                     | Description                                                                        | Default / Notes                  |
|-------------------------------------|------------------------------------------------------------------------------------|----------------------------------|
| `allowAllAccess(boolean)`           | Grants full access to Java host classes and members.                               | `true`                           |
| `allowNativeAccess(boolean)`        | Enables access to native interop (C API, JNI).                                     | `true`                           |
| `allowExperimentalOptions(boolean)` | Enables experimental GraalVM options.                                              | `false`                          |
| `allowCreateThread(boolean)`        | Allows context to spawn threads.                                                   | `true`                           |
| `hostAccess(HostAccess)`            | Controls how Java methods and fields are exposed to guest languages.               | `HostAccess.ALL`                 |
| `polyglotAccess(PolyglotAccess)`    | Configures inter-language communication policies.                                  | `PolyglotAccess.ALL`             |
| `resourceDirectory(String)`         | Defines GraalPy virtual resource directory for embedded files.                     | `org.graalvm.python.vfs`         |
| `resourcesPath(Path)`               | Sets filesystem path for runtime resources.                                        | resolved via `ResourcesProvider` |
| `withSafePythonDefaults()`          | Applies safe defaults for GraalPy: disables C API, hides warnings, redirects logs. | optional                         |
| `withNodeSupport()`                 | Enables Node.js compatibility in GraalJS (adds `require()`, `fs`, etc.).           | optional                         |
| `option(String, String)`            | Adds a custom GraalVM engine option.                                               | user-defined                     |
| `options(Map<String,String>)`       | Adds multiple engine options.                                                      | user-defined                     |

### Example

```java
var ctx = new PolyglotContextFactory.Builder(Language.PYTHON)
    .withSafePythonDefaults()
    .allowAllAccess(true)
    .option("python.CAPI", "false")
    .option("python.WarnExperimentalFeatures", "false")
    .build();
```
---
### 📘 Examples
For full, runnable examples of builder usage, see the [`examples/java-example`](./examples/java-example) directory.

---

## Error Handling

All runtime exceptions are wrapped in `EvaluationException`.

| Error                | Description                           |
|----------------------|---------------------------------------|
| `Missing resource`   | Script/module not found               |
| `Binding failed`     | Executor failed to initialize binding |
| `No matching method` | Java interface not implemented        |
| `Invocation failed`  | GraalVM or reflection error           |

---

## Development

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

Licensed under the **Apache License 2.0**.  
See [LICENSE](./LICENSE) for details.
