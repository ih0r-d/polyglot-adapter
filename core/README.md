# polyglot-adapter-core

## Badges
![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=java)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![GraalPy](https://img.shields.io/badge/GraalPy-25.x-3776AB?logo=python)
![Tests](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

Note: You can replace the above shields with GitHub-native clickable badges once the repository CI and coverage are configured, for example:
[![GitHub Actions](https://github.com/ih0r/polyglot-adapter/actions/workflows/maven.yml/badge.svg)](https://github.com/ih0r/polyglot-adapter/actions/workflows/maven.yml)
[![Coverage (Codecov)](https://codecov.io/gh/ih0r/polyglot-adapter/branch/main/graph/badge.svg)](https://codecov.io/gh/ih0r/polyglot-adapter)

Overview
polyglot-adapter-core is a lightweight Java library that provides a unified, high-level API for executing code across languages via GraalVM, with a current focus on Python (GraalPy 25.x). It abstracts polyglot context creation and script execution behind a simple adapter interface.

Key features
- Simple, unified API via PolyglotAdapter
- Python execution through GraalPy (org.graalvm.python)
- Pluggable executors (BaseExecutor) for future languages
- Convenient context factory with safe defaults and custom configuration
- Utility helpers for reflection and file/resource handling

Modules and main types
- io.github.ih0r.adapter.api.PolyglotAdapter: High-level facade
- io.github.ih0r.adapter.api.executors.PyExecutor: Internal Python executor (GraalPy)
- io.github.ih0r.adapter.api.executors.BaseExecutor: Contract for executors
- io.github.ih0r.adapter.api.context.PolyglotContextFactory: Builder for org.graalvm.polyglot.Context
- Utilities: CommonUtils, StringCaseConverter, Constants
- Exceptions: EvaluationException

Requirements
- JDK 25+
- Maven 3.9+
- Runtime: GraalVM 25.x with Python language (GraalPy 25.x) for actual polyglot execution
- Note: Unit tests in this module do NOT require a full GraalPy runtime; dependencies are resolved via Maven and tests use fakes/mocks.

Installation
This module is part of a multi-module project. In your own project, add the dependency (coordinates may change depending on release):

<dependency>
  <groupId>io.github.ih0r</groupId>
  <artifactId>polyglot-adapter-core</artifactId>
  <version>0.1.0</version>
</dependency>

Quick start
- Default Python adapter
try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
    Map<String,Object> result = adapter.evaluate("my_method", MyApi.class, 1, 2);
}

- Custom context configuration (GraalVM 25 / GraalPy 25)
PolyglotContextFactory.Builder builder = new PolyglotContextFactory.Builder()
        .allowExperimentalOptions(true)
        .allowAllAccess(true);
try (PolyglotAdapter adapter = PolyglotAdapter.python(builder)) {
    Map<String,Object> result = adapter.evaluate("my_method", MyApi.class);
}

- Generic construction (for testing or future executors)
BaseExecutor exec = /* your executor */;
try (PolyglotAdapter adapter = PolyglotAdapter.of(exec)) {
    Map<String,Object> result = adapter.evaluate("my_method", MyApi.class);
}

Defining your Python API
- Create a Java interface that mirrors the Python class API:
public interface MyApi {
    int add(int a, int b);
    void ping();
}

- Provide a Python class with the same name as the interface (class name must match exactly), stored either:
  1) On the classpath at resources/python/my_api.py, or
  2) On the local filesystem at src/main/python/my_api.py (during development)

Example: resources/python/my_api.py
class MyApi:
    def add(self, a: int, b: int) -> int:
        return a + b

    def ping(self) -> None:
        pass

How it works
- PyExecutor resolves the Python file name from the Java interface simple name using camelCase → snake_case.
- It first tries to load the script from classpath at python/<file>.py; if not found, it falls back to the local directory defined by Constants.PROJ_RESOURCES_PATH (src/main/python).
- A GraalPy Context is created using PolyglotContextFactory (either with an embedded VFS or a given resource directory).
- The top-level Python class is instantiated and mapped to the Java interface; then the requested method is invoked via reflection helpers in CommonUtils.

Configuration
- PolyglotContextFactory.Builder exposes common GraalVM flags such as allowAllAccess, HostAccess, IOAccess, allowCreateThread, allowNativeAccess, allowExperimentalOptions, PolyglotAccess, and an optional resourceDir to point to a prepared GraalPy resource directory.
- When targeting GraalVM 25.x with GraalPy 25.x, ensure the Python language is available in your distribution (e.g., via gu install python if using a non-complete build).
- The module’s pom.xml may include graalpy-maven-plugin or similar tooling to pre-process Python resources. Adjust as needed for your build.

Error handling
- Any reflective or execution errors are wrapped into EvaluationException with descriptive messages (e.g., missing method, missing Python file, class name mismatch).

Testing and coverage
- Unit tests use JUnit 5 and are designed to avoid requiring a full GraalPy runtime.
- Tests cover: PolyglotAdapter delegation, PyExecutor creation paths and error messages, CommonUtils behaviors, StringCaseConverter conversion, Language enum, EvaluationException.
- To run tests: mvn -q -DskipITs -DskipNativeTests test

Compatibility notes
- Recommended stack: JDK 25+, GraalVM 25.x, GraalPy 25.x.
- Earlier JDKs are not officially supported by this module’s documentation.

Development tips
- Place Python sources under src/main/python during development; they can be packaged into resources/python at build time if desired.
- Ensure your Java interface method signatures match the Python implementation.
- When adding support for another language, implement BaseExecutor and wire it into PolyglotAdapter similarly to PyExecutor.

Roadmap
- Additional language executors (JS, R, etc.)
- Optional real GraalPy integration tests behind a profile
- Better camelCase → snake_case for acronyms (configurable)

References
- GraalVM: https://www.graalvm.org/
- GraalPy: https://www.graalvm.org/python/
- Polyglot API: https://www.graalvm.org/reference-manual/polyglot/

License
Apache License 2.0. See LICENSE file (or project root) for details.
