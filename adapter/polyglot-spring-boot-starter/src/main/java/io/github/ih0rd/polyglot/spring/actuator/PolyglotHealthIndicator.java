package io.github.ih0rd.polyglot.spring.actuator;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import io.github.ih0rd.polyglot.spring.PolyglotExecutors;
import io.github.ih0rd.polyglot.spring.properties.PolyglotProperties;

/// Simple health check for polyglot executors.
///
/// Status mapping:
/// - UNKNOWN → polyglot.core.enabled = false
/// - UP      → at least one executor is available
/// - DOWN    → core enabled, but no executors available
public final class PolyglotHealthIndicator implements HealthIndicator {

  private final PolyglotExecutors executors;
  private final PolyglotProperties properties;

  public PolyglotHealthIndicator(PolyglotExecutors executors, PolyglotProperties properties) {
    this.executors = executors;
    this.properties = properties;
  }

  @Override
  public Health health() {
    if (!properties.core().enabled()) {
      return Health.unknown().build();
    }

    boolean pythonAvailable = executors.isPythonEnabled();
    boolean jsAvailable = executors.isJsEnabled();

    if (pythonAvailable || jsAvailable) {
      return Health.up()
          .withDetail("pythonEnabled", pythonAvailable)
          .withDetail("jsEnabled", jsAvailable)
          .build();
    }

    return Health.down()
        .withDetail("pythonEnabled", false)
        .withDetail("jsEnabled", false)
        .withDetail("reason", "No polyglot executors available")
        .build();
  }
}
