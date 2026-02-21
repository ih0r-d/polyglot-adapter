# polyglot-spring-boot-starter

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=openjdk)
![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

**Zero-config Spring Boot 4 starter** for `polyglot-adapter` that wires **Python / JavaScript** executors, registers **@PolyglotClient** interfaces as Spring beans, and exposes **Actuator + Micrometer** observability.

---

## Features

- Autoconfigures `PyExecutor` / `JsExecutor` when enabled via `polyglot.*`
- `PolyglotExecutors` facade bean for safe optional access
- `@PolyglotClient` + `@EnablePolyglotClients` scanning → creates client beans via `FactoryBean`
- Fail-fast validation for missing runtimes (configurable)
- Startup summary log (deterministic, production-friendly)
- Actuator: `/actuator/info` and `/actuator/health` contributors
- Micrometer: basic gauges for executor state / caches

---

## Requirements

- **Spring Boot 4.x**
- **JDK 25+**
- **GraalVM 25.x+**
- Add language runtime dependencies (Python/JS) only if you enable them

---

## Installation

Import the BOM (recommended) and add the starter:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.ih0rd</groupId>
      <artifactId>polyglot-bom</artifactId>
      <version>${polyglot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>io.github.ih0rd</groupId>
    <artifactId>polyglot-spring-boot-starter</artifactId>
  </dependency>
</dependencies>
```

### Add runtimes (only if you enable them)

#### GraalPy

```xml
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-embedding</artifactId>
  <version>25.0.1</version>
</dependency>
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-launcher</artifactId>
  <version>25.0.1</version>
</dependency>
```

#### GraalJS

```xml
<dependency>
  <groupId>org.graalvm.js</groupId>
  <artifactId>js</artifactId>
  <version>25.0.1</version>
  <type>pom</type>
</dependency>
```

---

## Auto-configuration

The starter registers auto-configurations via:

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Main configurations:

- `PolyglotAutoConfiguration` (core, properties, runtime checks, startup summary)
- `PolyglotPythonAutoConfiguration` (PyExecutor + optional warmup)
- `PolyglotJsAutoConfiguration` (JsExecutor + optional warmup)
- `PolyglotActuatorAutoConfiguration` (Info/Health)
- `PolyglotMetricsAutoConfiguration` (Micrometer binder)

---

## Configuration (`polyglot.*`)

All properties are under the `polyglot` prefix.

> Defaults are chosen for developer experience. Enable languages explicitly.

### Full property table

| Property                                |    Type |            Default | Description                                                                        |
|-----------------------------------------|--------:|-------------------:|------------------------------------------------------------------------------------|
| `polyglot.core.enabled`                 | boolean |             `true` | Master switch for the starter.                                                     |
| `polyglot.core.fail-fast`               | boolean |             `true` | If `true`, startup fails on critical errors (missing runtime, warmup failure).     |
| `polyglot.core.log-metadata-on-startup` | boolean |             `true` | Logs startup summary (see below).                                                  |
| `polyglot.core.log-level`               |  string |            `debug` | Starter log level hint (used for messages where applicable).                       |
| `polyglot.python.enabled`               | boolean |            `false` | Enables Python executor auto-config.                                               |
| `polyglot.python.resources-path`        |  string | `classpath:python` | Base resource path for Python scripts (propagated to adapter via system property). |
| `polyglot.python.warmup-on-startup`     | boolean |            `false` | Executes a noop expression at `ApplicationReadyEvent`.                             |
| `polyglot.python.preload-scripts`       |    list |               `[]` | **Planned**: preload scripts list (property exists; wire-up may be incremental).   |
| `polyglot.js.enabled`                   | boolean |            `false` | Enables JavaScript executor auto-config.                                           |
| `polyglot.js.resources-path`            |  string |     `classpath:js` | Base resource path for JS scripts (propagated to adapter via system property).     |
| `polyglot.js.warmup-on-startup`         | boolean |            `false` | Executes a noop expression at `ApplicationReadyEvent`.                             |
| `polyglot.js.preload-scripts`           |    list |               `[]` | **Planned**: preload scripts list (property exists; wire-up may be incremental).   |
| `polyglot.clients.base-packages`        |    list |               `[]` | Base packages to scan for `@PolyglotClient` interfaces (property-based scanning).  |
| `polyglot.actuator.info.enabled`        | boolean |             `true` | Adds polyglot section to `/actuator/info`.                                         |
| `polyglot.actuator.health.enabled`      | boolean |             `true` | Adds polyglot indicator to `/actuator/health`.                                     |
| `polyglot.metrics.enabled`              | boolean |             `true` | Registers Micrometer meters when Micrometer is present.                            |

### Example `application.yml`

```yaml
polyglot:
  core:
    enabled: true
    fail-fast: true
    log-metadata-on-startup: true
    log-level: debug

  python:
    enabled: true
    resources-path: classpath:python
    warmup-on-startup: true
    preload-scripts:
      - forecast.py
      - libraries.py
      - stats.py

  js:
    enabled: false

  clients:
    base-packages:
      - io.github.ih0rd.examples.contracts

  actuator:
    info:
      enabled: true
    health:
      enabled: true

  metrics:
    enabled: true
```

---

## Startup summary log

When `polyglot.core.log-metadata-on-startup=true`, the starter logs a stable summary.

Example output:

```text
---- Polyglot Starter ----------------------------------------
Core        : ENABLED, failFast=true, logLevel=DEBUG
Python      : ENABLED (available)
  resources : classpath:python
  warmup    : true
  preload   : none
  clients   : 3
JavaScript  : DISABLED
Executors   : python=ACTIVE, js=OFF
Startup     : polyglot=93 ms
--------------------------------------------------------------
```

Notes:
- `clients` is derived from executor metadata (e.g., `instanceCacheSize` for Python binding cache).
- `Startup : polyglot=... ms` measures time from bean construction to summary emission.

---

## Using executors directly

Inject `PolyglotExecutors`:

```java
import io.github.ih0rd.polyglot.spring.PolyglotExecutors;
import org.springframework.stereotype.Service;

@Service
public class DemoService {
  private final PolyglotExecutors polyglot;

  public DemoService(PolyglotExecutors polyglot) {
    this.polyglot = polyglot;
  }

  public String hello() {
    return polyglot.python()
        .map(py -> py.evaluate("'hello from python'", String.class))
        .orElse("python disabled");
  }
}
```

---

## `@PolyglotClient` (typed bindings as Spring beans)

### 1) Define a contract

```java
import io.github.ih0rd.contract.SupportedLanguage;
import io.github.ih0rd.polyglot.spring.client.PolyglotClient;

@PolyglotClient(languages = SupportedLanguage.PYTHON)
public interface ForecastService {
  java.util.Map<String, Object> forecast(
      java.util.List<Double> y, int steps, int seasonPeriod);
}
```

### 2) Provide a guest implementation (Python)

```python
import polyglot

class ForecastService:
    def forecast(self, y, steps, season_period=4):
        return {"forecast": [1.0, 2.0], "season_period": season_period}

polyglot.export_value("ForecastService", ForecastService)
```

### 3) Enable scanning

You can enable scanning via annotation:

```java
import io.github.ih0rd.polyglot.spring.client.EnablePolyglotClients;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePolyglotClients(basePackages = "io.github.ih0rd.examples.contracts")
public class App {}
```

Or via properties:

```yaml
polyglot:
  clients:
    base-packages:
      - io.github.ih0rd.examples.contracts
```

After scanning, the interface becomes a Spring bean (created by `PolyglotClientFactoryBean`) and can be injected normally.

---

## Actuator

### `/actuator/info`

Adds a `polyglot` section including configuration and runtime availability (per enabled language).

### `/actuator/health`

Adds a health component reflecting:
- language enabled flags
- executor availability (runtime presence, bean presence)
- shallow status (no guest code execution by default)

---

## Micrometer metrics

When `polyglot.metrics.enabled=true` and Micrometer is present, the starter registers gauges such as:

- `polyglot.executor.source.cache.size{language="python"}`
- `polyglot.executor.source.cache.size{language="js"}`

Values are sourced from executor `metadata()` (e.g., `sourceCacheSize`).

---

## Troubleshooting

### `polyglot.python.enabled=true` but runtime missing

If Python is enabled but GraalPy runtime is not on the classpath, startup fails (when fail-fast is enabled):

- add `org.graalvm.python:python-embedding`
- add `org.graalvm.python:python-launcher`

Same concept applies to JS runtime.

---
## 📜 License
Licensed under the **Apache License 2.0**.  
See [LICENSE](../LICENSE) for details.