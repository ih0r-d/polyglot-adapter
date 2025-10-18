package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import io.github.ih0rd.adapter.utils.ValueUnwrapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiFunction;

/**
 * Common executor interface for all polyglot language bindings.
 * Supports synchronous and asynchronous evaluation with shared virtual-thread executor.
 */
public interface BaseExecutor extends AutoCloseable {

    Map<Class<?>, Source> SOURCE_CACHE = new ConcurrentHashMap<>();
    ExecutorService VIRTUAL_EXECUTOR =
            Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType, Object... args);
    <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType);
    <T> EvalResult<?> evaluate(String code);

    String languageId();
    Context context();

    static <E extends BaseExecutor> E createDefault(Language lang, BiFunction<Context, Path, E> constructor) {
        var builder = new PolyglotContextFactory.Builder(lang);
        return constructor.apply(builder.build(), builder.getResourcesPath());
    }

    static <E extends BaseExecutor> E create(Language lang, PolyglotContextFactory.Builder builder, BiFunction<Context, Path, E> constructor) {
        return constructor.apply(builder.build(), builder.getResourcesPath());
    }

    default <T> EvalResult<?> evalInline(String code) {
        try (var ctx = context()) {
            Value value = ctx.eval(Source.newBuilder(languageId(), code, "inline." + languageId()).buildLiteral());
            if (value == null || value.isNull()) return EvalResult.of(null);
            T unwrapped = ValueUnwrapper.unwrap(value);
            return EvalResult.of(unwrapped);
        } catch (Exception e) {
            throw new EvaluationException("Error during " + languageId() + " code execution", e);
        }
    }

    default CompletableFuture<EvalResult<?>> evaluateAsync(String code) {
        return CompletableFuture.supplyAsync(() -> evalInline(code), VIRTUAL_EXECUTOR);
    }

    default <T> CompletableFuture<EvalResult<?>> evaluateAsync(
            String methodName, Class<T> memberTargetType, Object... args) {
        return CompletableFuture.supplyAsync(
                () -> evaluate(methodName, memberTargetType, args),
                VIRTUAL_EXECUTOR
        );
    }

    default <T> CompletableFuture<EvalResult<?>> evaluateAsync(
            String methodName, Class<T> memberTargetType) {
        return CompletableFuture.supplyAsync(
                () -> evaluate(methodName, memberTargetType),
                VIRTUAL_EXECUTOR
        );
    }

    @Override
    void close();
}
