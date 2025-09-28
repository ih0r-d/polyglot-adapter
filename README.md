# polyglot-adapter-core

## Badges
![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=java)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![GraalPy](https://img.shields.io/badge/GraalPy-25.x-3776AB?logo=python)
![Tests](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

---

## Overview
polyglot-adapter is a lightweight Java library that provides a unified, high-level API for executing code across languages via GraalVM, with a current focus on Python (GraalPy 25.x). It abstracts polyglot context creation and script execution behind a simple adapter interface.

---

## Key features
- Simple, unified API via `PolyglotAdapter`
- Python execution through GraalPy
- Pluggable executors (`BaseExecutor`) for future languages
- Convenient context factory with safe defaults and custom configuration
- Utility helpers for reflection and file/resource handling

---

## Modules and main types
- `io.github.ih0rd.adapter.api.PolyglotAdapter`: High-level facade
- `io.github.ih0rd.adapter.api.executors.PyExecutor`: Internal Python executor (GraalPy)
- `io.github.ih0rd.adapter.api.executors.BaseExecutor`: Contract for executors
- `io.github.ih0rd.adapter.api.context.PolyglotContextFactory`: Builder for `org.graalvm.polyglot.Context`
- `io.github.ih0rd.adapter.api.context.Language`: Enum for supported languages
- `io.github.ih0rd.adapter.api.context.ResourcesProvider`: Centralized resolver for per-language resource paths
- Utilities: `CommonUtils`, `StringCaseConverter`
- Exceptions: `EvaluationException`

---

## Requirements
- JDK 25+
- Maven 3.9+
- Runtime: GraalVM 25.x with Python language installed (`gu install python`)

---

## Installation

```xml
<dependency>
  <groupId>io.github.ih0r</groupId>
  <artifactId>polyglot-adapter</artifactId>
  <version>0.0.1</version>
</dependency>
```


## How it works
- `PyExecutor` resolves the Python file name from the Java interface simple name using camelCase → snake_case.
- It loads the script from the classpath under `resources/python/<file>.py`.
- If not found, it falls back to a filesystem path provided by `ResourcesProvider.get(Language.PYTHON)`:
    - Default: `${user.dir}/src/main/python`
    - Overridable: `-Dpolyglot.py.polyglot-resources.path=/custom/path/to/python`
- A `Context` is created using `PolyglotContextFactory(Language.PYTHON)`.
- The top-level Python class is instantiated and mapped to the Java interface.
- The requested method is invoked via reflection helpers in `CommonUtils`.

---

## Configuration
- `PolyglotContextFactory.Builder` exposes common GraalVM flags:
    - `allowAllAccess`
    - `HostAccess`
    - `IOAccess`
    - `allowCreateThread`
    - `allowNativeAccess`
    - `allowExperimentalOptions`
    - `PolyglotAccess`
- Python files are expected on the classpath (`resources/python`).  
  During development, override with:
  ```bash
  -Dpolyglot.py.polyglot-resources.path=/absolute/path/to/python
  ```

---

## Error handling
- Any reflective or execution errors are wrapped into `EvaluationException`.
- Examples:
    - Python file not found
    - Java interface name does not match Python class name
    - Method missing in Python class

---

## Testing
- Unit tests use JUnit 5.
- Core unit tests can run without a real GraalPy runtime; integration tests belong in the `examples` module.
- To run:
  ```bash
  mvn test
  ```

---

## Compatibility
- JDK 25+
- GraalVM 25.x
- GraalPy 25.x

---

## Roadmap
- Executors for JS, R, and others
- Optional real GraalPy integration tests behind a profile
- Improved camelCase → snake_case handling for acronyms

---

## Examples

You can find working usage samples in the [examples](./examples) directory.  
Currently available:

- [Java Example](./examples/java-example) — demonstrates Python integration with GraalPy and the `PolyglotAdapter`.


## References
- https://www.graalvm.org/
- https://www.graalvm.org/python/
- https://www.graalvm.org/reference-manual/polyglot/

---

## License
Apache License 2.0 — see LICENSE in project root
