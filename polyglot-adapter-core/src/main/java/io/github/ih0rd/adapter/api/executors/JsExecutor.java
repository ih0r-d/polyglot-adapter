package io.github.ih0rd.adapter.api.executors;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;
import io.github.ih0rd.adapter.utils.ValueUnwrapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.nio.file.Path;

/**
 * Executor for executing JavaScript code via GraalJS (part of GraalVM Polyglot API).
 *
 * <p>This executor provides the ability to evaluate JavaScript source code dynamically
 * using the same unified interface as other language executors (e.g. {@link PyExecutor}).
 * It can execute either:
 * <ul>
 *     <li>Inline JS expressions (e.g. {@code "1 + 2"})</li>
 *     <li>JS modules or functions loaded from the classpath or filesystem</li>
 * </ul>
 *
 * <p>By default, GraalJS runs in the same polyglot {@link Context} created
 * by {@link PolyglotContextFactory.Builder}, which defines host access,
 * experimental options, and other capabilities.
 *
 * <p>Typical usage:
 * <pre>{@code
 * var builder = new PolyglotContextFactory.Builder(Language.JS);
 * try (var exec = new JsExecutor(builder.build(), builder.getResourcesPath())) {
 *     EvalResult<?> result = exec.evaluate("Math.max(10, 42)");
 *     System.out.println("Result: " + result.value());
 * }
 * }</pre>
 *
 * @see io.github.ih0rd.adapter.api.context.Language
 * @see io.github.ih0rd.adapter.api.context.PolyglotContextFactory
 * @see org.graalvm.polyglot.Context
 */
public record JsExecutor(Context context, Path resourcesPath) implements BaseExecutor {

    /** Create executor with default context configuration. */
    public static JsExecutor createDefault() {
        var builder = new PolyglotContextFactory.Builder(Language.JS);
        return new JsExecutor(builder.build(), builder.getResourcesPath());
    }

    /** Create executor with custom context configuration. */
    public static JsExecutor create(PolyglotContextFactory.Builder builder) {
        return new JsExecutor(builder.build(), builder.getResourcesPath());
    }


    /**
     * Evaluates a JavaScript method with arguments defined inside a JS module.
     *
     * @param methodName the JavaScript function name
     * @param memberTargetType Java interface mapping the JS module/class
     * @param args arguments passed to the JS function
     * @param <T> the interface type bound to the JS object
     * @return evaluation result containing return type and value
     */
    @Override
    public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType, Object... args) {
        throw new UnsupportedOperationException(
                "Method-based evaluation is not yet implemented for JavaScript executor. " +
                        "Use evaluate(String code) instead."
        );
    }

    /**
     * Evaluates a parameterless JavaScript method.
     *
     * @param methodName the JavaScript function name
     * @param memberTargetType Java interface mapping the JS module/class
     * @param <T> the interface type bound to the JS object
     * @return evaluation result containing return type and value
     */
    @Override
    public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType) {
        throw new UnsupportedOperationException(
                "Method-based evaluation is not yet implemented for JavaScript executor. " +
                        "Use evaluate(String code) instead."
        );
    }

    /**
     * Evaluates arbitrary inline JavaScript code within the current GraalVM context.
     *
     * <p>This is the low-level entry point for evaluating one-liner JS expressions or blocks of code.
     * Example:
     * <pre>{@code
     * EvalResult<?> res = executor.evaluate("let x = [1,2,3]; x.reduce((a,b) => a+b)");
     * System.out.println(res); // EvalResult[type=Integer, value=6]
     * }</pre>
     *
     * @param code the raw JavaScript code to evaluate
     * @param <T> the expected type of the result (auto-inferred)
     * @return evaluation result with inferred Java type and value
     */
    @Override
    public <T> EvalResult<?> evaluate(String code) {
        try {
            var value = context.eval(
                    Source.newBuilder(Language.JS.id(), code, "inline.js").buildLiteral()
            );

            if (value == null || value.isNull()) {
                return EvalResult.of(null);
            }

            T unwrapped = ValueUnwrapper.unwrap(value);

            return EvalResult.of(unwrapped);
        } catch (Exception e) {
            throw new EvaluationException(
                    "Error during JavaScript code execution", e
            );
        }
    }
    /**
     * Closes the underlying GraalVM context and releases all associated resources.
     */
    @Override
    public void close() {
        context.close();
    }
}
