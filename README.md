# polyglot-adapter

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=openjdk)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ih0r-d/polyglot-adapter.svg?label=maven%20central)](https://central.sonatype.com/artifact/io.github.ih0r-d/polyglot-adapter)
[![codecov](https://codecov.io/gh/ih0r-d/polyglot-adapter/graph/badge.svg)](https://codecov.io/gh/ih0r-d/testcontainers-with-coverage)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

---

## 🚀 Overview

**polyglot-adapter** is a lightweight Java SDK that provides an **executor-based API** for executing and embedding multi-language code (Python, JavaScript) via **GraalVM Polyglot**.

It hides the boilerplate of context creation, resources resolution and host access configuration, while keeping full control over `Context.Builder` and `HostAccess` for advanced use cases.

> ✅ Focus on developer experience — predictable, fast, minimal API surface.

---

## ✨ Key Features

- **Executor-based API** — `PyExecutor` and `JsExecutor` built on shared `AbstractPolyglotExecutor`.
- **Automatic host-to-guest binding** via Java interfaces (`bind(MyApi.class)`).
- **Script discovery & caching**:
    - Python: instance cache for exported classes per interface.
    - JavaScript: source cache for loaded modules per interface.
- **Composable context configuration** via helper methods (options, host access, type mappings).
- **Built-in type mappings** (e.g. `Value → Path`), user-extendable.
- **Virtual File System (VFS) integration for GraalPy** — works with virtualenv + native extensions (NumPy, etc.).
- **Metadata & diagnostics** — each executor exposes a `metadata()` snapshot (language id, resources path, cache sizes, loaded interfaces).
- **No framework dependencies** — just Java + GraalVM.
- 100% compatible with **GraalVM 25.x+**.

---

## 🧩 Architecture (core module)

Current project layout:

```text
src
├── main
│ └── java
│     └── io
│         └── github
│             └── ih0rd
│                 └── adapter
│                     ├── context
│                     │ ├── AbstractPolyglotExecutor.java
│                     │ ├── JsExecutor.java
│                     │ ├── PolyglotHelper.java
│                     │ ├── PyExecutor.java
│                     │ ├── ResourcesProvider.java
│                     │ └── SupportedLanguage.java
│                     ├── exceptions
│                     │ ├── BindingException.java
│                     │ ├── EvaluationException.java
│                     │ ├── InvocationException.java
│                     │ └── ScriptNotFoundException.java
│                     └── utils
│                         ├── CommonUtils.java
│                         ├── Constants.java
│                         └── StringCaseConverter.java
└── test
    ├── java
    │ └── io
    │     └── github
    │         └── ih0rd
    │             └── adapter
    │                 ├── DummyApi.java
    │                 ├── DummyApiBoxed.java
    │                 ├── context
    │                 │ ├── BaseExecutorTest.java
    │                 │ ├── PolyglotHelperTest.java
    │                 │ ├── PyExecutorTest.java
    │                 │ ├── ResourcesProviderTest.java
    │                 │ └── SupportedLanguageTest.java
    │                 ├── exceptions
    │                 │ └── EvaluationExceptionTest.java
    │                 └── utils
    │                     ├── CommonUtilsTest.java
    │                     ├── ConstantsTest.java
    │                     └── StringCaseConverterTest.java
    ├── js
    │ └── dummy_api.js
    └── python
        └── dummy_api.py
```

High-level roles:

- `AbstractPolyglotExecutor` — shared executor base (context lifecycle, resource loading, source cache, error mapping, metadata).
- `PyExecutor` / `JsExecutor` — language-specific executors on top of the base.
- `ResourcesProvider` — resolves script locations from classpath / filesystem.
- `SupportedLanguage` — language identifiers and engine ids.
- `*Exception` — thin wrappers around common failure scenarios (binding, evaluation, invocation).

---

## ⚙️ Requirements

- **JDK 25+**
- **Maven 3.9+**
- **GraalVM 25.x+** (JDK distribution with Python / JS installed where required)

---

## 📦 Installation

```xml
<dependency>
  <groupId>io.github.ih0r-d</groupId>
  <artifactId>polyglot-adapter</artifactId>
  <version>0.0.20</version>
</dependency>
```

> Optional language runtimes are **not** pulled transitively — you choose which GraalVM languages to add.

---

## 🧩 Optional Language Runtimes (Maven)

Add only the runtimes you actually need.

### 🐍 GraalPy

```xml
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-embedding</artifactId>
  <version>25.0.1</version>
  <optional>true</optional>
</dependency>
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-launcher</artifactId>
  <version>25.0.1</version>
  <optional>true</optional>
</dependency>
```

### 🕸 GraalJS

```xml
<dependency>
  <groupId>org.graalvm.js</groupId>
  <artifactId>js</artifactId>
  <version>25.0.1</version>
  <type>pom</type>
  <optional>true</optional>
</dependency>
```

---

## 📁 Script Layout & Naming

By default `ResourcesProvider` looks for scripts under language-specific folders on the classpath / filesystem.

Recommended layout:

```text
src/main/resources/
├── python/
│   └── my_api.py
└── js/
    └── my_api.js
```

Interface names are converted to script names via `StringCaseConverter`. For example:

- `DummyApi` → `dummy_api.py` / `dummy_api.js`
- `ForecastService` → `forecast_service.py` / `forecast_service.js`

You can plug a custom `ResourcesProvider` if your layout differs.

---

## 🧠 Usage Example (Python)

**Java side:**

```java
try (var executor = PyExecutor.createDefault()) {
    MyApi api = executor.bind(MyApi.class);
    System.out.println(api.add(3, 5)); // 8
}
```

**Python side (`python/my_api.py`):**

```python
class MyApi:
    def add(self, a, b):
        return a + b

polyglot.export_value("MyApi", MyApi)
```

`bind(MyApi.class)`:

1. Resolves the script via `ResourcesProvider`.
2. Evaluates it in a GraalPy `Context`.
3. Looks up exported symbol (`MyApi`).
4. Creates a Java proxy implementing `MyApi` backed by the Python object.

---

## 🧠 Usage Example (JavaScript)

**Java side:**

```java
try (var executor = JsExecutor.createDefault()) {
    ForecastService api = executor.bind(ForecastService.class);
    var forecast = api.forecast(List.of(1.0, 2.0, 3.0));
    System.out.println(forecast);
}
```

**JavaScript side (`js/forecast_service.js`):**

```javascript
class ForecastService {
  forecast(series) {
    const last = series[series.length - 1] ?? 0;
    return {
      forecast: [last + 1, last + 2, last + 3]
    };
  }
}

polyglot.export('ForecastService', ForecastService);
```

---

## 🔍 Metadata & Diagnostics

Each executor exposes a `metadata()` method with a lightweight snapshot of its state.

**Common fields (`AbstractPolyglotExecutor`):**

```json
{
  "executorType": "io.github.ih0rd.adapter.context.PyExecutor",
  "languageId": "python",
  "resourcesPath": "src/main/python",
  "sourceCacheSize": 1
}
```

**Python-specific (`PyExecutor`):**

```json
{
  "cachedInterfaces": [
    "io.github.demo.ForecastService",
    "io.github.demo.StatsApi"
  ],
  "instanceCacheSize": 2
}
```

**JavaScript-specific (`JsExecutor`):**

```json
{
  "loadedInterfaces": [
    "io.github.demo.ForecastService"
  ]
}
```

This is useful for:

- verifying that scripts were discovered and cached,
- exposing internal state via metrics / Actuator,
- debugging resource-path / naming issues without enabling heavy logging.

Example:

```java
try (var executor = PyExecutor.createDefault()) {
    Map<String, Object> meta = executor.metadata();
    meta.forEach((k, v) -> System.out.println(k + " = " + v));
}
```

---

## ⚙️ Context Configuration

By default executors configure a GraalVM `Context` with sensible defaults:

- `allowAllAccess(true)` — full Java ↔ guest interop.
- `allowExperimentalOptions(true)` — required for latest GraalPy / GraalJS options.

Advanced configuration is exposed via helper methods (exact API may evolve, but the ideas stay the same):

### 1. Low-level context tweaks

```java
var executor = PyExecutor.create(builder -> {
    builder
        .allowAllAccess(true)
        .allowExperimentalOptions(true);
    // raw engine options if needed
    builder
        .option("python.IsolateNativeModules", "true")
        .option("python.WarnExperimentalFeatures", "false");
});
```

### 2. HostAccess extensions

The library installs **LOW-precedence** mappings so users can override them:

```java
// internally
hostAccessBuilder.targetTypeMapping(
    com.oracle.truffle.api.interop.Value.class,
    java.nio.file.Path.class,
    Value::isString,
    v -> Path.of(v.asString()),
    HostAccess.TargetMappingPrecedence.LOW
);
```

You can register your own mappings with higher precedence, or additional ones:

```java
// user code (conceptual)
extendHostAccess(builder -> builder.targetTypeMapping(
    com.oracle.truffle.api.interop.Value.class,
    java.time.Instant.class,
    Value::isString,
    v -> Instant.parse(v.asString())
));
```

### 3. GraalPy “safe defaults”

For Python, the library provides a helper configuration (e.g. `withSafePythonDefaults()`) that:

- prepares GraalPy for multiple contexts using native modules (NumPy and friends),
- reduces noisy experimental warnings,
- works with the embedded virtual environment mounted into GraalPy VFS.

This keeps the default experience smooth while still allowing you to override options if needed.

### 4. Node.js support for GraalJS

If you need Node.js compat for JS code, enable it explicitly (e.g. `withNodeSupport()`), which configures the JS engine with the required options. Keeping this opt-in avoids hidden runtime cost for plain JS scripts.

---

## 🧪 Testing

Run all tests (JUnit 5):

```bash
mvn clean test
```

Included test coverage:

- Context creation (Python / JS)
- Resource resolution & naming
- Executor binding and invocation
- Error mapping and exceptions

---

## 🧑‍💻 Development

| Command                      | Action                   |
|------------------------------|--------------------------|
| `mvn clean verify`           | Build & run tests        |
| `mvn deploy -P release`      | Publish to Maven Central |
| `task bump TYPE=minor`       | Version bump             |
| `task release VERSION=X.Y.Z` | Tag & release version    |

---

## 📜 License

Licensed under the **Apache License 2.0**.  
See [LICENSE](./LICENSE) for details.
