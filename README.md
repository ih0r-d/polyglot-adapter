# polyglot-adapter

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=openjdk)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ih0r-d/polyglot-adapter.svg?label=maven%20central)](https://central.sonatype.com/artifact/io.github.ih0r-d/polyglot-adapter)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

---

## 🚀 Overview

**polyglot-adapter** is a lightweight Java SDK providing a **unified executor-based API** for executing and embedding multi-language code (Python, JavaScript) via **GraalVM Polyglot**. 
It simplifies context creation, host access management, and interlanguage communication while preserving full control over GraalVM configuration.

> ✅ Focused on developer experience — predictable, fast, fully composable.

---

## ✨ Key Features
- **Unified `BaseExecutor` API** with native `Value` interop (`Value.as(...)`)
- **Automatic host-to-guest binding** via Java interfaces (`bind()`)
- **Composable `Context.Builder` API** through `.apply(...)`
- **Extensible HostAccess** with `.extendHostAccess(...)`
- Built-in **type mappings** (`Value → Path`, user-extendable)
- Virtual File System (VFS) integration for GraalPy
- Fully **dependency-free** (no frameworks)
- 100% compatible with **GraalVM 25.x+**
---

## 🧩 Architecture

```
polyglot-adapter/
├── api/
│   ├── context/
│   │   ├── Language.java
│   │   ├── PolyglotContextFactory.java
│   │   └── ResourcesProvider.java
│   └── executors/
│       ├── BaseExecutor.java
│       ├── PyExecutor.java
│       └── JsExecutor.java
├── exceptions/
│   └── EvaluationException.java
└── utils/
    ├── CommonUtils.java
    └── StringCaseConverter.java
```

---

## ⚙️ Requirements
- **JDK 25+**
- **Maven 3.9+**
- **GraalVM 25.x+**

---

## 📦 Installation
```xml
<dependency>
  <groupId>io.github.ih0r-d</groupId>
  <artifactId>polyglot-adapter</artifactId>
  <version>0.1.0</version>
</dependency>
```
---
## 🧩 Optional Language Runtimes (Maven)

> Add only the runtimes you actually use. Marked as `optional` to avoid transitive pulls.

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


## 🧠 Usage Example (Python)

```shell
try (var executor = PyExecutor.createDefault()) {
    MyApi api = executor.bind(MyApi.class);
    System.out.println(api.add(3, 5)); // 8
}
```

**Python side:**
```shell
class MyApi:
    def add(self, a, b): return a + b

polyglot.export_value("MyApi", MyApi)
```

---

## ⚙️ Configuration

### Defaults

```shell
.allowAllAccess(true)
.allowExperimentalOptions(true)
```
> Full access for interop, experimental options for latest GraalPy / GraalJS engines.  
> These defaults follow **Oracle’s embedding best practices** for SDKs (not sandboxes).

| Option                           | Purpose                                                                        |
|----------------------------------|--------------------------------------------------------------------------------|
| `allowAllAccess(true)`           | Enables complete Java ↔ guest interoperability (IO, threads, polyglot bridge). |
| `allowExperimentalOptions(true)` | Activates all evolving GraalVM engine flags.                                   |

If sandboxing is needed:
```shell
.apply(b -> b.allowAllAccess(false))
.apply(b -> b.allowIO(false))
.hostAccess(HostAccess.NONE);
```

---

### 🧱 Default Type Mappings (LOW precedence)

The SDK adds safe default mappings for convenience:

```shell
builder.targetTypeMapping(
    Value.class, Path.class,
    Value::isString, v -> Path.of(v.asString()),
    HostAccess.TargetMappingPrecedence.LOW
);
```

Users can extend or override mappings freely via:
```shell
.extendHostAccess(b -> b.targetTypeMapping(
    Value.class, Instant.class,
    Value::isString, v -> Instant.parse(v.asString())
));
```

---

### 🧰 Builder Quick Reference

| Method                                           | Description                                          |
|--------------------------------------------------|------------------------------------------------------|
| `apply(Consumer<Context.Builder>)`               | Direct low-level context configuration.              |
| `extendHostAccess(Consumer<HostAccess.Builder>)` | Extend or override SDK default mappings.             |
| `withSafePythonDefaults()`                       | Disable GraalPy C API, hide warnings, redirect logs. |
| `withNodeSupport()`                              | Enable Node.js compatibility for GraalJS.            |
| `option(String, String)`                         | Add single engine option.                            |
| `options(Map<String,String>)`                    | Add multiple engine options.                         |

---

## 🧪 Testing
Run all tests (JUnit 5):
```bash
mvn clean test
```

Includes:
- Context creation tests (Python / JS)
- Executor binding and async evaluation tests

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