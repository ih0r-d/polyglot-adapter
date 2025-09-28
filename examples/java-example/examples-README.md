# Polyglot Adapter Examples

This module contains runnable examples that demonstrate how to use **polyglot-adapter-core** with GraalVM and GraalPy.

---

## Examples

### 1. PolyglotDemo
**File:** `src/main/java/io/github/ih0rd/examples/PolyglotDemo.java`

Shows two use cases of the adapter:
1. **Default resources** — loads Python scripts from the default path (`src/main/python`).
2. **Custom resources** — overrides the Python resources path with a system property.

Run:
```bash
./mvnw clean compile exec:java -Dexec.mainClass="io.github.ih0rd.examples.PolyglotDemo"
```

Expected output (simplified):
```
=== Running with DEFAULT resources ===
Result (default): {add=3}

=== Running with CUSTOM resources ===
Result (custom path): {add=30}
```

---

### 2. Integration with external libraries *(placeholder)*
**Planned example.**  
Demonstrates how to extend the adapter with third‑party Java/Python libraries.

---

### 3. Advanced configuration *(placeholder)*
**Planned example.**  
Shows how to configure the adapter with `PolyglotContextFactory.Builder`, enabling fine‑grained GraalVM options.

---

## Notes
- All examples are **self‑contained** and live under `examples/java-example`.
- Python scripts are located under:
  - `src/main/resources/python/`
  - `src/main/python/`
- To run examples, you need GraalVM with Python installed:
  ```bash
  gu install python
  ```
