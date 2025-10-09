# polyglot-adapter-core

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=openjdk)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![GraalPy](https://img.shields.io/badge/GraalPy-25.x-3776AB?logo=python)
![Tests](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

---

## 📑 Table of Contents
- [Overview](#-overview)
- [Key Features](#-key-features)
- [Project Structure](#-project-structure)
- [Main Components](#-main-components)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [How It Works](#-how-it-works)
- [Configuration](#-configuration)
- [Error Handling](#-error-handling)
- [Development Workflow](#-development-workflow)
- [Examples](#-examples)
- [License](#-license)

---

## 📖 Overview

**polyglot-adapter-core** is the core SDK providing a unified API for executing multi-language code via **GraalVM Polyglot** — currently optimized for **Python (GraalPy 25.x)**.  
It wraps low-level Graal API details into a clean, type-safe Java interface.

---

## ✨ Key Features
- 🔹 Unified high-level API: `PolyglotAdapter`
- 🔹 Python execution via GraalPy
- 🔹 Extensible executors (`BaseExecutor` → `PyExecutor`)
- 🔹 Context factory with sandbox and access controls
- 🔹 Reflection-based invocation utilities
- 🔹 Minimal, dependency-light core (no Spring, no Guice)

---

## 📂 Project Structure

```
polyglot-adapter-core/
├── pom.xml
├── mvnw, mvnw.cmd
└── src/
    ├── main/java/io/github/ih0rd/adapter/
    │   ├── api/
    │   │   ├── PolyglotAdapter.java
    │   │   ├── context/
    │   │   │   ├── EvalResult.java
    │   │   │   ├── Language.java
    │   │   │   ├── PolyglotContextFactory.java
    │   │   │   └── ResourcesProvider.java
    │   │   └── executors/
    │   │       ├── BaseExecutor.java
    │   │       └── PyExecutor.java
    │   ├── exceptions/
    │   │   └── EvaluationException.java
    │   └── utils/
    │       ├── CommonUtils.java
    │       ├── Constants.java
    │       └── StringCaseConverter.java
    └── test/java/io/github/ih0rd/adapter/
        ├── api/
        │   ├── PolyglotAdapterTest.java
        │   ├── context/
        │   │   ├── LanguageTest.java
        │   │   └── PolyglotContextFactoryTest.java
        │   └── executors/
        │       ├── PyExecutorCreationTest.java
        │       └── PyExecutorTest.java
        ├── exceptions/
        │   └── EvaluationExceptionTest.java
        └── utils/
            ├── CommonUtilsTest.java
            └── StringCaseConverterTest.java
```

---

## 🧩 Main Components

| Component                             | Description                                                   |
|---------------------------------------|---------------------------------------------------------------|
| **PolyglotAdapter**                   | Facade for evaluating Python (and future language) methods.   |
| **BaseExecutor**                      | Generic executor interface for any supported language.        |
| **PyExecutor**                        | Python implementation of `BaseExecutor` using GraalPy.        |
| **PolyglotContextFactory**            | Builds and configures Graal polyglot contexts.                |
| **EvalResult**                        | Lightweight immutable record holding method result and type.  |
| **ResourcesProvider**                 | Resolves Python resource directories (classpath or external). |
| **EvaluationException**               | Unified runtime wrapper for all execution errors.             |
| **CommonUtils / StringCaseConverter** | Helpers for reflection and name normalization.                |

---

## ⚙️ Requirements
- JDK **25+**
- Maven **3.9+**
- GraalVM **25.x+**
- Python language installed for GraalVM:
  ```bash
  gu install python
  ```

---

## 📦 Installation
Add dependency:
```xml
<dependency>
  <groupId>io.github.ih0rd</groupId>
  <artifactId>polyglot-adapter-core</artifactId>
  <version>0.0.3-SNAPSHOT</version>
</dependency>
```

---

## 🔍 How It Works

1. `PolyglotAdapter.python()` loads a Python implementation mapped from a Java interface.
2. It creates a `Context` using `PolyglotContextFactory(Language.PYTHON)`.
3. The `PyExecutor` loads the corresponding `.py` file (by naming convention).
4. Reflection invokes the appropriate Python method and wraps the output in `EvalResult<T>`.

---

## 🧰 Example

```java
try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
    EvalResult<Integer> sum = adapter.evaluate("add", MyApi.class, 10, 5);
    System.out.println(sum.value()); // → 15
}
```

Where:
```java
public interface MyApi {
    int add(int a, int b);
}
```
and in `resources/python/my_api.py`:
```python
class MyApi:
    def add(self, a: int, b: int) -> int:
        return a + b
```

---

## ⚙️ Configuration

| Property                              | Description                       | Default           |
|---------------------------------------|-----------------------------------|-------------------|
| `polyglot.py.polyglot-resources.path` | Custom Python resource root       | `src/main/python` |
| `allowAllAccess`                      | Enables full host access          | `true`            |
| `allowExperimentalOptions`            | Allows experimental GraalPy flags | `true`            |
| `allowNativeAccess`                   | Enables native interop            | `true`            |

---

## ❗ Error Handling

All runtime issues are wrapped in **`EvaluationException`**, e.g.:

| Error                   | Description                               |
|-------------------------|-------------------------------------------|
| `Missing resource file` | Python module not found                   |
| `No matching method`    | Java interface method not in Python class |
| `Invocation failed`     | Underlying GraalPy or reflection error    |

---

## 🧪 Development Workflow

Project includes `Taskfile.yml` for unified build/test commands.

| Command                      | Action                         |
|------------------------------|--------------------------------|
| `task clean`                 | Clean Maven + python-resources |
| `task build`                 | Build package                  |
| `task test`                  | Run tests                      |
| `task verify`                | Test + Checkstyle              |
| `task release VERSION=X.Y.Z` | Tag and release version        |
| `task bump TYPE=patch        | minor                          |major` | Version bump |

All scripts in [`scripts/`](./scripts) are optional helpers.

---

## 💡 Examples
See [examples/java-example](../examples/java-example) for an end-to-end integration demo using GraalPy and `PolyglotAdapter`.

---

## 📜 License
Apache License 2.0 — see [LICENSE](./LICENSE)
