# polyglot-adapter

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-25%2B-007396?logo=openjdk)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

A modular toolkit for running **Python** and **JavaScript** inside JVM applications via **GraalVM Polyglot**.

This repository contains:

- **`polyglot-core`** — framework-agnostic Java SDK (executors, binding, context helper)
- **`polyglot-spring-boot-starter`** — Spring Boot 4 auto-configuration + client scanning + Actuator/Micrometer
- **`polyglot-bom`** — BOM for consistent dependency versions
- **`examples/`** — demo apps

---

## Repository layout

```
.
├── polyglot-bom/
├── polyglot-core/
├── polyglot-spring-boot-starter/
├── examples/
├── scripts/
├── pom.xml
├── CHANGELOG.md
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── LICENSE
└── Taskfile.yaml
```

---

## Requirements

- **JDK 25+**
- **GraalVM 25.x+**
- **Maven 3.9+**

> The **starter** targets **Spring Boot 4.x**.

---

## Quick start

### 1) Use the BOM (recommended)

Import the BOM and then add the module(s) you need without versions:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.github.ih0r-d</groupId>
      <artifactId>polyglot-bom</artifactId>
      <version>${polyglot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 2) Core SDK (framework-agnostic)

```xml
<dependency>
  <groupId>io.github.ih0r-d</groupId>
  <artifactId>polyglot-adapter</artifactId>
</dependency>
```

### 3) Spring Boot Starter

```xml
<dependency>
  <groupId>io.github.ih0r-d</groupId>
  <artifactId>polyglot-spring-boot-starter</artifactId>
</dependency>
```

---

## Optional language runtimes (add only what you use)

The project keeps language engines **optional** to avoid unwanted transitive pulls.

### 🐍 GraalPy

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

### 🕸 GraalJS

```xml
<dependency>
  <groupId>org.graalvm.js</groupId>
  <artifactId>js</artifactId>
  <version>25.0.1</version>
  <type>pom</type>
</dependency>
```

---

## Documentation

- [Core SDK](./polyglot-core/README.md)
- [Spring Boot starter](./polyglot-spring-boot-starter/README.md)

---


## 📜 License
Licensed under the **Apache License 2.0**.  
See [LICENSE](./LICENSE) for details.