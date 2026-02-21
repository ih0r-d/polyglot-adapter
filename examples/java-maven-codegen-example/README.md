# java-maven-codegen-example

Example project demonstrating:

- Java interface generation from Python contracts (`polyglot-codegen`)
- GraalVM Python embedding
- Maven profile–based build control

---

## Requirements

- Java 21+
- Maven 3.9+
- GraalVM 25+

---

## Project Structure

```
src/main/resources/python        # Python scripts (contracts)
target/generated-sources/polyglot # Generated Java interfaces
```

---

## Generate Interfaces

Runs contract code generation only:

```bash
mvn clean install -Pcodegen
```

Generated interfaces will appear in:

```
target/generated-sources/polyglot
```

---

## Enable Graal Python Integration

Locks Python dependencies and prepares runtime resources:

```bash
mvn clean install -Pgraal
```

---

## Full Build (Codegen + Graal)

```bash
mvn clean install -Pcodegen,graal
```

---

## Notes

- Generated files are overwritten on each `-Pcodegen` build.
- Do not edit files under `target/generated-sources`.
- Codegen is currently integrated via `exec-maven-plugin`.
- In future versions this will be replaced by a dedicated Maven plugin.

---