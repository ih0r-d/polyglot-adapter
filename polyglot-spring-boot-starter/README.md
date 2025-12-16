/// # Polyglot Adapter: Spring Boot Starter

This module provides autoconfiguration for the `polyglot-core` library, making it seamless to embed and interact with Python and JavaScript code within a Spring Boot application.

/// ## 1. Features

- **Auto-Configuration**: Automatically configures `PyExecutor` and `JsExecutor` beans.
- **Externalized Configuration**: A rich set of properties under the `polyglot.*` prefix for fine-grained control.
- **Dependency Injection**: A convenient `PolyglotExecutors` facade to inject executors into your services.
- **Lifecycle Management**: Handles executor creation, warmup, and script preloading on startup.
- **Validation**: Can validate Java-to-guest bindings on startup to fail-fast.
- **Actuator Integration**: Exposes executor status and metadata via `/actuator/health` and `/actuator/info`.
- **Micrometer Metrics**: (Optional) Registers basic metrics for monitoring executor state.

/// ## 2. Getting Started

Add the starter as a dependency to your project's `pom.xml`.

```xml
<dependency>
    <groupId>io.github.ih0rd</groupId>
    <artifactId>polyglot-spring-boot-starter</artifactId>
    <version>${polyglot-adapter.version}</version>
</dependency>
```

The starter will be active by default. You can disable it by setting `polyglot.core.enabled=false` in your `application.properties`.

/// ## 3. Core Usage

### 3.1. Accessing Executors

The easiest way to use the polyglot executors is to inject the `PolyglotExecutors` facade into your Spring components.

```java
import io.github.ih0rd.polyglot.spring.PolyglotExecutors;
import org.springframework.stereotype.Service;

@Service
public class MyService {

    private final PolyglotExecutors polyglot;

    public MyService(PolyglotExecutors polyglot) {
        this.polyglot = polyglot;
    }

    public void runScripts() {
        // Safely access the executor
        polyglot.python().ifPresent(py -> {
            Long result = py.evaluate("1 + 1", Long.class);
            System.out.println("Python says: 1 + 1 = " + result);
        });

        // Or require it, which throws an exception if it's not configured
        var js = polyglot.requireJs();
        String greeting = js.evaluate("'Hello from ' + 'JS!'", String.class);
        System.out.println(greeting);
    }
}
```

### 3.2. Binding Java Interfaces

The primary power of the adapter is its ability to bind guest language functions to Java interfaces.

**1. Define a Java Interface:**

```java
public interface Greeter {
    String hello(String name);
}
```

**2. Create a Python Implementation:**

Place the script in a location accessible to the `ResourcesProvider` (e.g., `src/main/resources/python/my_api.py`).

```python
# my_api.py
def hello(name):
  return f"Hello, {name}!"
```

**3. Bind and Use in Java:**

```java
import io.github.ih0rd.polyglot.spring.PolyglotExecutors;
import org.springframework.stereotype.Service;

@Service
public class GreeterService {

    private final Greeter pythonGreeter;

    public GreeterService(PolyglotExecutors polyglot) {
        // Bind the Python implementation to the Java interface
        this.pythonGreeter = polyglot.requirePython().bind(Greeter.class);
    }

    public String sayHello(String name) {
        return pythonGreeter.hello(name); // Executes the Python function
    }
}
```

/// ## 4. Configuration Properties

All properties are prefixed with `polyglot`.

| Property                         | Description                                                                                             | Default            |
|----------------------------------|---------------------------------------------------------------------------------------------------------|--------------------|
| `core.enabled`                   | Global on/off switch for the starter.                                                                   | `true`             |
| `core.fail-fast`                 | If `true`, fails application startup on critical errors (e.g., script preload failure).                 | `true`             |
| `core.log-metadata-on-startup`   | If `true`, logs a snapshot of polyglot configuration and detected runtimes on startup.                  | `true`             |
| `core.log-level`                 | Log level used for starter debug messages.                                                              | `debug`            |
| `python.enabled`                 | Enables the `PyExecutor` bean.                                                                          | `false`            |
| `python.resources-path`          | Path to the root directory for Python scripts (e.g., `classpath:/python/`).                               | `classpath:python` |
| `python.vfs-enabled`             | Enables GraalPy VFS integration in the executor configuration.                                          | `true`             |
| `python.safe-defaults`           | If `true`, applies core “safe defaults” for Python.                                                     | `true`             |
| `python.warmup-on-startup`       | Executes a simple expression to warm up the Python context on startup.                                  | `false`            |
| `python.preload-scripts[]`       | A list of script files to evaluate on startup.                                                          | `[]`               |
| `js.enabled`                     | Enables the `JsExecutor` bean.                                                                          | `false`            |
| `js.node-support`                | Enables Node.js compatibility mode for the JS executor.                                                 | `false`            |
| `js.resources-path`              | Path to the root directory for JavaScript scripts.                                                      | `classpath:js`     |
| `js.warmup-on-startup`           | Executes a simple expression to warm up the JS context on startup.                                      | `false`            |
| `js.preload-scripts[]`           | A list of script files to evaluate on startup.                                                          | `[]`               |
| `actuator.info.enabled`          | If `true`, adds polyglot metadata to the `/actuator/info` endpoint.                                       | `true`             |
| `actuator.health.enabled`        | If `true`, adds a polyglot health check to the `/actuator/health` endpoint.                               | `true`             |
| `metrics.enabled`                | If `true` and Micrometer is on the classpath, registers polyglot metrics.                               | `true`             |

**Example `application.properties`:**

```properties
# Enable python
polyglot.python.enabled=true

# Set the path for Python scripts
polyglot.python.resources-path=classpath:/scripts/py/

# Preload a utility script on startup
polyglot.python.preload-scripts[0]=utils.py
```

/// ## 5. Actuator & Metrics

### 5.1. Health Endpoint

The starter contributes to the `/actuator/health` endpoint. It will report `UP` if at least one executor is available and healthy, and show the status of each language.

```json
{
  "status": "UP",
  "components": {
    "polyglot": {
      "status": "UP",
      "details": {
        "pythonEnabled": true,
        "jsEnabled": false
      }
    },
    "...": "..."
  }
}
```

### 5.2. Info Endpoint

If enabled, the `/actuator/info` endpoint will display metadata about each active executor, such as the GraalVM engine version.

```json
"polyglot": {
  "core": {
    "enabled": true,
    "failFast": true
  },
  "python": {
    "enabled": true,
    "resourcesPath": "classpath:python",
    "vfsEnabled": true,
    "warmupOnStartup": false
  },
  "js": {
    "enabled": false
  }
}
```

### 5.3. Micrometer Metrics

When `polyglot.metrics.enabled=true` and Micrometer is present, the starter registers gauges that can be scraped by a monitoring system like Prometheus. This can be used to monitor the state of the polyglot runtime.
 - `polyglot.python.enabled`: 1 if python is enabled, 0 otherwise
 - `polyglot.js.enabled`: 1 if javascript is enabled, 0 otherwise
 - `polyglot.executor.source.cache.size`: The size of the source cache for each language
