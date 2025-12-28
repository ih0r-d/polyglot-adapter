package io.github.ih0rd.polyglot.spring.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import io.github.ih0rd.adapter.context.JsExecutor;
import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.polyglot.spring.properties.PolyglotProperties;

/// # PolyglotStartupValidator
///
/// Internal Spring lifecycle component responsible for **polyglot startup warmup**
/// and **early failure detection**.
///
/// ## Responsibilities
/// - Perform safe GraalVM engine warmup (NOOP execution)
/// - Optionally fail application startup on initialization errors
/// - Respect user configuration (`enabled`, `failFast`, `warmupOnStartup`)
///
/// ## Design notes
/// - This class is **internal** and not part of public API
/// - No user scripts are executed
/// - Context lifecycle is managed by executors
/// - Uses Spring `SmartLifecycle` for deterministic startup ordering
///
public final class PolyglotStartupValidator implements SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(PolyglotStartupValidator.class);

    private final PolyglotProperties properties;
    private final PyExecutor pyExecutor;
    private final JsExecutor jsExecutor;

    private volatile boolean running;

    public PolyglotStartupValidator(
            PolyglotProperties properties,
            PyExecutor pyExecutor,
            JsExecutor jsExecutor) {

        this.properties = properties;
        this.pyExecutor = pyExecutor;
        this.jsExecutor = jsExecutor;
    }

    /// ## start
    ///
    /// Invoked automatically during Spring context startup.
    ///
    /// Performs:
    /// - Python warmup (if enabled)
    /// - JavaScript warmup (if enabled)
    ///
    /// Behavior on failure:
    /// - `failFast=true`  → abort application startup
    /// - `failFast=false` → log warning and continue
    ///
    @Override
    public void start() {
        if (!properties.core().enabled()) {
            return;
        }

        try {
            warmupPython();
            warmupJs();
            running = true;
        } catch (Exception e) {
            if (properties.core().failFast()) {
                throw new IllegalStateException(
                        "Polyglot startup warmup failed", e);
            }
            log.warn(
                    "Polyglot startup warmup failed (failFast=false): {}",
                    e.getMessage(),
                    e
            );
        }
    }

    /// ## warmupPython
    ///
    /// Executes a safe NOOP expression to warm up GraalPy engine.
    ///
    /// Notes:
    /// - Does NOT execute user code
    /// - Does NOT create or close Context
    /// - Uses existing executor-managed Context
    ///
    private void warmupPython() {
        if (pyExecutor == null
                || !properties.python().enabled()
                || !properties.python().warmupOnStartup()) {
            return;
        }

        log.debug("Warming up Python executor");
        pyExecutor.evaluate(PolyglotWarmupConstants.NOOP_EXPRESSION);
    }

    /// ## warmupJs
    ///
    /// Executes a safe NOOP expression to warm up GraalJS engine.
    ///
    /// Notes:
    /// - Does NOT execute user code
    /// - Does NOT create or close Context
    /// - Uses existing executor-managed Context
    ///
    @SuppressWarnings("resource")
    private void warmupJs() {
        if (jsExecutor == null
                || !properties.js().enabled()
                || !properties.js().warmupOnStartup()) {
            return;
        }

        log.debug("Warming up JavaScript executor");
        jsExecutor.evaluate(PolyglotWarmupConstants.NOOP_EXPRESSION);
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    /// Run as late as possible, after all beans are ready.
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
