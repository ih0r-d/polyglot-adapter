# Java Example for Polyglot Adapter

This module demonstrates how to use **polyglot-adapter-core** with Python code (via GraalPy).

---

## Project structure
```
examples/java-example
├── README.md                # this file
├── pom.xml                  # Maven configuration for the example
├── mvnw*, .mvn              # Maven wrapper
├── src
│   ├── main
│   │   ├── java/io/github/ih0rd/examples
│   │   │   ├── MyApi.java         # Java interface definition
│   │   │   └── PolyglotDemo.java  # Demo application
│   │   ├── python/my_api.py       # Python sources (dev)
│   │   └── resources/python/      # Packaged Python resources
│   │       └── my_api.py
│   └── test/java/io/github/ih0rd/examples
│       └── HelloPolyglotTest.java # Example JUnit test
└── python/my_api.py         # Extra Python copy for convenience
```


---

## Quick start

### Default Python adapter
```java
import io.github.ih0rd.adapter.api.PolyglotAdapter;
import java.util.Map;

public class DemoDefault {
    void main() {
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            Map<String, Object> result = adapter.evaluate("add", MyApi.class, 1, 2);
            System.out.println(result);
        }
    }
}
```

### Custom context configuration
```java
import io.github.ih0rd.adapter.api.PolyglotAdapter;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;

public class DemoCustom {
    void main() {
        var builder = new PolyglotContextFactory.Builder(Language.PYTHON)
                .allowExperimentalOptions(true)
                .allowAllAccess(true);

        try (PolyglotAdapter adapter = PolyglotAdapter.python(builder)) {
            adapter.evaluate("ping", MyApi.class);
        }
    }
}
```

### Generic executor (future-proof)
```java
import io.github.ih0rd.adapter.api.PolyglotAdapter;
import io.github.ih0rd.adapter.api.executors.BaseExecutor;

public class DemoGeneric {
    void main() {
        BaseExecutor exec = /* provide your executor instance */ null;
        try (PolyglotAdapter adapter = PolyglotAdapter.of(exec)) {
            adapter.evaluate("my_method", MyApi.class);
        }
    }
}
```

---

## Defining your Python API

### Java interface
```java
public interface MyApi {
    int add(int a, int b);
    void ping();
}
```

### Python implementation
File: `src/main/resources/python/my_api.py`
```python
class MyApi:
    def add(self, a: int, b: int) -> int:
        return a + b

    def ping(self) -> None:
        pass
```


---

## Running the demo

Build and run from this directory:

```bash
./mvnw clean package
./mvnw exec:java -Dexec.mainClass="io.github.ih0rd.examples.PolyglotDemo"
```

Or use the provided **Taskfile** in the project root.

---

## Key parts

- **`MyApi.java`**  
  Java interface that defines methods (`add`, `ping`) expected to be implemented in Python.

- **`my_api.py`**  
  Python class with the same name implementing those methods.

- **`PolyglotDemo.java`**  
  Shows how to load and run the Python class via `PolyglotAdapter`.

- **`PolyglotDemoTest.java`**  
  Simple JUnit test validating the adapter usage.

---

## Notes

- Requires **GraalVM 25+** with Python installed:
  ```bash
  gu install python
  ```
- Uses `polyglot-adapter-core` from the parent project.  
