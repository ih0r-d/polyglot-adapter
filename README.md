# Polyglot Platform

![Build](https://img.shields.io/badge/build-maven-blue?logo=apache-maven)
![Java](https://img.shields.io/badge/JDK-21%20%7C%2025-007396?logo=openjdk)
![GraalVM](https://img.shields.io/badge/GraalVM-25.x-FF6F00?logo=oracle)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ih0r-d/polyglot-adapter.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.ih0r-d/polyglot-adapter)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)

A modular platform for running polyglot languages (Python, JavaScript)
inside JVM applications via GraalVM Polyglot, with optional
contract-based interface generation.

------------------------------------------------------------------------

## Architecture Overview

Polyglot Platform consists of two independent layers:

-   **Adapter** --- runtime execution layer (JDK 25)
-   **Tooling** --- contract and interface generation (JDK 21)

------------------------------------------------------------------------

## Repository Structure

    .
    ├── adapter/                (JDK 25 runtime)
    │   ├── polyglot-adapter/
    │   ├── polyglot-spring-boot-starter/
    │   └── polyglot-bom/
    │
    ├── tooling/                (JDK 21 build-time tools)
    │   ├── polyglot-contract-api/
    │   ├── polyglot-codegen/
    │   └── polyglot-codegen-maven-plugin/
    │
    ├── examples/
    ├── scripts/
    └── CHANGELOG.md

------------------------------------------------------------------------

## Components

### Adapter (Runtime -- JDK 25)

-   **polyglot-adapter** --- framework-agnostic execution layer
-   **polyglot-spring-boot-starter** --- Spring Boot 4
    autoconfiguration
-   **polyglot-bom** --- dependency management

------------------------------------------------------------------------

### Tooling (Build-time -- JDK 21)

-   **polyglot-contract-api** --- shared contract model
-   **polyglot-codegen** --- contract → Java interface generator
-   **polyglot-codegen-maven-plugin** --- Maven integration

------------------------------------------------------------------------

## Requirements

### Runtime (adapter)

-   JDK 25+
-   GraalVM 25.x+
-   Maven 3.9+

### Tooling

-   JDK 21+
-   Maven 3.9+

------------------------------------------------------------------------

## Quick Start (Runtime)

### 1. Import BOM

``` xml
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

### 2. Add Adapter

``` xml
<dependency>
  <groupId>io.github.ih0r-d</groupId>
  <artifactId>polyglot-adapter</artifactId>
</dependency>
```

### 3. Optional: Spring Boot Starter

``` xml
<dependency>
  <groupId>io.github.ih0r-d</groupId>
  <artifactId>polyglot-spring-boot-starter</artifactId>
</dependency>
```

------------------------------------------------------------------------

## Optional Language Runtimes

### GraalPy

``` xml
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-embedding</artifactId>
</dependency>
<dependency>
  <groupId>org.graalvm.python</groupId>
  <artifactId>python-launcher</artifactId>
</dependency>
```

### GraalJS

``` xml
<dependency>
  <groupId>org.graalvm.js</groupId>
  <artifactId>js</artifactId>
  <type>pom</type>
</dependency>
```

------------------------------------------------------------------------

## Development

Build everything:

    task build

Build only adapter:

    MODULE=polyglot-adapter task build

Build only tooling:

    MODULE=polyglot-codegen task build

------------------------------------------------------------------------

## License

Licensed under the Apache License 2.0.
