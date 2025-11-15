package io.github.ih0rd.adapter.api.executors;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;

import org.graalvm.polyglot.*;

import io.github.ih0rd.adapter.api.context.ResourcesProvider;
import io.github.ih0rd.adapter.api.context.SupportedLanguage;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

/// # BaseExecutor
///
/// Abstract superclass for all GraalVM language executors (Python, JS, etc.)
///
/// ---
/// ## Responsibilities
/// - Manage per-instance context and resource paths.
/// - Provide shared helpers for function invocation and script loading.
/// - Ensure clean context lifecycle handling (closeable).
///
/// ---
/// ## Design
/// - No static caches for sources (context-bound only).
/// - Thread-safe `ConcurrentHashMap` for per-instance script caching.
/// - Virtual-thread pool for async evaluations.
public abstract class BaseExecutor implements AutoCloseable {

  /// ### context
  /// GraalVM polyglot execution context.
  protected final Context context;

  /// ### resourcesPath
  /// Filesystem path to language-specific resources.
  protected final Path resourcesPath;

  /// ### sourceCache
  /// Cache of loaded `Source` objects per executor instance.
  protected final Map<String, Source> sourceCache = new ConcurrentHashMap<>();

  protected BaseExecutor(Context context, Path resourcesPath) {
    this.context = context;
    this.resourcesPath = resourcesPath;
  }

  /// ### languageId
  /// Returns the GraalVM language identifier (e.g. `"python"`, `"js"`).
  public abstract String languageId();

  /// ### callFunction
  /// Executes a named function within the current language bindings.
  /// Throws {@link EvaluationException} if function not found or cannot execute.
  protected Value callFunction(String methodName, Object... args) {
    try {
      Value bindings = context.getBindings(languageId());
      Value fn = bindings.getMember(methodName);
      if (fn == null || !fn.canExecute()) {
        throw new EvaluationException("Function not found: " + methodName);
      }
      return fn.execute(args);
    } catch (Exception e) {
      throw new EvaluationException("Error executing function: " + methodName, e);
    }
  }

  /// ### loadScript
  /// Loads a script file (from filesystem or classpath) for the given language.
  /// Returns a cached {@link Source} instance for performance.
  protected Source loadScript(SupportedLanguage lang, String name) {
    String key = lang.id() + ":" + name;
    return sourceCache.computeIfAbsent(
        key,
        k -> {
          try {
            return loadSource(lang, name);
          } catch (IOException e) {
            throw new EvaluationException("Failed to load source: " + name, e);
          }
        });
  }

  /// ### loadSource
  /// Internal helper that performs lookup of script file in:
  /// 1. Filesystem (via `resourcesPath`)
  /// 2. Classpath (under `/python/` or `/js/`)
  private Source loadSource(SupportedLanguage lang, String name) throws IOException {
    String fileName = name + lang.ext();
    Path fsPath = ResourcesProvider.get(lang).resolve(fileName);

    if (Files.exists(fsPath)) {
      return Source.newBuilder(lang.id(), fsPath.toFile()).build();
    }

    String cpPath = lang.name().toLowerCase() + "/" + fileName;
    try (InputStream is =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(cpPath)) {
      if (is != null) {
        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          return Source.newBuilder(lang.id(), reader, cpPath).build();
        }
      }
    }
    throw new EvaluationException("Script not found: " + fileName);
  }

  /// ### evaluate
  /// Evaluates a code snippet inline for the current language.
  /// Returns the result as {@link Value}.
  public Value evaluate(String code) {
    try {
      return context.eval(Source.newBuilder(languageId(), code, "inline." + languageId()).build());
    } catch (Exception e) {
      throw new EvaluationException("Error during " + languageId() + " evaluation", e);
    }
  }

  /// ### context
  /// Returns underlying GraalVM {@link Context}.
  protected Context context() {
    return context;
  }

  /// ### close
  /// Clears internal caches and closes GraalVM context safely.
  @Override
  public void close() {
    sourceCache.clear();
    context.close();
  }
}
