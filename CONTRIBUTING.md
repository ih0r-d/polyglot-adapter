# Contributing to polyglot-adapter-core

We welcome all contributions — bug fixes, improvements, and new features.  
Please follow these simple guidelines to keep the process consistent and efficient.

---

## 🧩 Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/ih0r-d/polyglot-adapter-core.git
   cd polyglot-adapter-core
   ```
3. **Set up Maven Wrapper** (already included):
   ```bash
   ./mvnw clean package
   ```
4. Verify everything builds correctly before making changes.

---

## 🧱 Development Workflow

The project uses [Taskfile](https://taskfile.dev) for unified commands.

| Command                      | Description                  |
|------------------------------|------------------------------|
| `task clean`                 | Clean Maven & temp resources |
| `task build`                 | Build JARs                   |
| `task test`                  | Run all tests                |
| `task bump`                  | Increment version            |
| `task release VERSION=X.Y.Z` | Tag release                  |
| `task format`                | Format code (Spotless)       |

---

## 🧪 Tests

- Unit tests use **JUnit 5**.
- Run all tests before opening a PR:
  ```bash
  task test
  ```
- Add tests for any new functionality or bug fix.

---

## 🧭 Commit & Branch Rules

- Branch naming: `feature/...`, `fix/...`, `chore/...`
- Use clear commit messages (imperative style):
  ```text
  feat(core): add PolyglotAdapterFactory
  fix(python): correct resource path resolution
  chore(deps): update GraalVM SDK version
  ```
- Keep commits atomic and logically grouped.

---

## 🧰 Pull Requests

1. Make sure the build and tests pass locally.
2. Update docs or README if relevant.
3. Open a PR to the `main` branch.
4. Describe the intent of your change clearly.
5. Maintainers will review and provide feedback quickly.

---

## 🧑‍⚖️ Code Style

- Java 25+ syntax, Maven 3.9+
- Linting: `./mvnw spotless:apply`
- Follow existing conventions for naming and structure.

---

## 🔒 License

By contributing, you agree that your contributions are licensed under the **Apache License 2.0**, the same as the main project.

---

## 💬 Questions?

For general questions, open a [GitHub Discussion](https://github.com/ih0rd/polyglot-adapter-core/discussions).  
For issues or bugs, create a [GitHub Issue](https://github.com/ih0rd/polyglot-adapter-core/issues).

