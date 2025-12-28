
# Polyglot Java Example (GraalPy + Polyglot Adapter)

This repository demonstrates how to use **GraalPy Maven Plugin (v25.0.0)** together with **polyglot-adapter-core** to execute Python code from Java through GraalVM.

---

## 1. Overview

The example shows how to:
- Install and manage Python packages via the `graalpy-maven-plugin`.
- Create a GraalPy virtual environment and lock dependencies.
- Use the `PolyglotAdapter` SDK to call Python code from Java.

---

## 2. Project Structure

```
polyglot-java-example/
├── pom.xml
├── graalpy.lock
├── python-resources/
│   ├── venv/
│   └── requirements.lock
├── src/
│   ├── main/
│   │   ├── java/io/github/ih0rd/examples/
│   │   │   ├── PolyglotDemo.java
│   │   │   └── contracts/
│   │   │       ├── LibrariesApi.java
│   │   │       └── MyApi.java
│   │   ├── python/my_api.py
│   │   └── resources/python/
│   │       ├── libraries_api.py
│   │       └── my_api.py
│   └── test/java/io/github/ih0rd/examples/
│       └── PolyglotDemoTest.java
└── README.md
```

---

## 3. GraalPy Maven Plugin Configuration

```xml
<properties>
  <python.package.1>requests==2.32.3</python.package.1>
  <python.package.2>rich==13.9.2</python.package.2>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>org.graalvm.python</groupId>
      <artifactId>graalpy-maven-plugin</artifactId>
      <version>25.0.0</version>
      <configuration>
        <packages>
          <package>${python.package.1}</package>
          <package>${python.package.2}</package>
        </packages>
      </configuration>
      <executions>
        <execution>
          <id>generate-python-resources</id>
          <phase>generate-resources</phase>
          <goals>
            <goal>process-graalpy-resources</goal>
          </goals>
        </execution>
        <execution>
          <id>lock-python-packages</id>
          <goals>
            <goal>lock-packages</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

---

## 4. Building and Installing Python Packages

```bash
mvn clean package   -Dpython.package.1=requests==2.32.3   -Dpython.package.2=rich==13.9.2
```

**Result:**
- Virtual environment created in ` target/classes/org.graalvm.python.vfs/venv`  
- Packages installed: `requests`, `rich`  
- Lock file generated: `graalpy.lock`

---

## 5. Verifying Installed Packages

```bash
target/classes/org.graalvm.python.vfs/venv/bin/graalpy -m pip list
```
**Expected output:**
```
Package         Version
--------------- -----------
Faker           26.0.0
pip             24.3.1
python-dateutil 2.9.0.post0
six             1.17.0
tabulate        0.9.0
```

---

## 6. Running the Polyglot Demo

### Example Java Code
```java
import io.github.ih0rd.adapter.api.PolyglotAdapter;
import io.github.ih0rd.examples.contracts.MyApi;

public class PolyglotDemo {
    void main() {
        try (PolyglotAdapter adapter = PolyglotAdapter.python()) {
            var result = adapter.evaluate("add", MyApi.class, 2, 3);
            System.out.println("Result: " + result);
        }
    }
}
```

### Build and Run
```bash
./mvnw clean package
./mvnw exec:java -Dexec.mainClass="io.github.ih0rd.examples.PolyglotDemo"
```

---

## 7. Python API Definition

**Java interface:**
```java
public interface MyApi {
    int add(int a, int b);
    void ping();
}
```

**Python implementation:** (`src/main/resources/python/my_api.py`)
```python
class MyApi:
    def add(self, a: int, b: int) -> int:
        return a + b

    def ping(self) -> None:
        print("pong")
```

---

## 8. Notes

- Requires **GraalVM 25+** with Python installed:  
  ```bash
  gu install python
  ```

- No need for `pythonHome` configuration — GraalPy 25.0.0 handles venv automatically.  
- The project uses **polyglot-adapter-core** SDK to execute Python seamlessly via GraalVM.

---

## 9. Troubleshooting

If you see warnings like:
```
WARNING: sun.misc.Unsafe::objectFieldOffset has been called ...
```
They can be safely ignored, or you can suppress them with JVM flags:
```bash
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED --enable-native-access=ALL-UNNAMED
```

## 🧩 Common GraalPy / GraalVM Issues

| **Error / Warning**                                             | **Cause**                                                                                   | **How to Fix / Notes**                                                                                       |
|-----------------------------------------------------------------|---------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `NameError: name '__file__' is not defined`                     | GraalPy doesn’t define `__file__` when evaluating inline sources.                           | Before `context.eval(...)`, set: `context.getBindings("python").putMember("__file__", "path/to/script.py");` |
| `ModuleNotFoundError: No module named 'requests'`               | The GraalPy venv hasn’t been created or packages weren’t installed.                         | Run:<br>`mvn graalpy:process-graalpy-resources`<br>or simply<br>`mvn clean package`                          |
| `python.Executable not found`                                   | Plugin couldn’t locate the GraalPy binary in venv.                                          | Check `python-resources/venv/bin/graalpy` exists or adjust `python.external.dir` property.                   |
| `EvaluationException: Method <name> is not supported`           | The Java interface method doesn’t match Python implementation (missing or wrong signature). | Ensure both names and argument counts align between Java and Python.                                         |
| `WARNING: sun.misc.Unsafe::objectFieldOffset`                   | GraalVM’s Truffle runtime uses deprecated JDK internals.                                    | Safe to ignore.<br>To silence: `--add-opens=java.base/sun.misc=ALL-UNNAMED`                                  |
| `WARNING: java.lang.System::load has been called`               | Restricted native access warning under GraalVM 25+.                                         | Safe to ignore.<br>Add JVM flag: `--enable-native-access=ALL-UNNAMED`                                        |
| `IOAccess denied`                                               | Polyglot context lacks permission for file or I/O access.                                   | In `PolyglotContextFactory`, enable: `.allowIO(IOAccess.ALL)`                                                |
| `Lock file not created`                                         | The `graalpy:lock-packages` goal wasn’t executed.                                           | Run:<br>`mvn graalpy:lock-packages@lock-python-packages`                                                     |
| `venv missing`                                                  | Plugin didn’t generate Python virtual environment.                                          | Ensure `generate-resources` phase ran, or run:<br>`mvn process-resources`                                    |
| `UnsupportedLanguageException: No language for id python found` | GraalVM Python not installed.                                                               | Install with:<br>`gu install python`                                                                         |
| `ClassCastException` during evaluation                          | Returned Python value doesn’t match Java interface type.                                    | Adjust Java method signature or wrap return value in compatible type.                                        |

---

✅ **Tips**
- Always ensure **GraalVM ≥ 25.0.0** is used.
- Use `-Dpython.external.dir=<abs-path>` to override venv location.
- For debugging: enable verbose plugin logs via `-X`.
- Warnings starting with `WARNING:` are *harmless* unless they block execution.


---

## 10. License

Apache License 2.0  
© 2025 ih0rd
