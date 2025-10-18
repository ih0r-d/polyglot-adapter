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
 * JavaScript executor implementation for GraalJS.
 * Supports inline code evaluation via the unified polyglot adapter API.
 */
public record JsExecutor(Context context, Path resourcesPath) implements BaseExecutor {

    public static JsExecutor createDefault() {
        return BaseExecutor.createDefault(Language.JS, JsExecutor::new);
    }

    public static JsExecutor create(PolyglotContextFactory.Builder builder) {
        return BaseExecutor.create(Language.JS, builder, JsExecutor::new);
    }

    @Override
    public String languageId() {
        return Language.JS.id();
    }

    @Override
    public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType, Object... args) {
        throw new UnsupportedOperationException(
                "Method-based evaluation is not yet implemented for JavaScript executor. " +
                        "Use evaluate(String code) instead.");
    }

    @Override
    public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType) {
        throw new UnsupportedOperationException(
                "Method-based evaluation is not yet implemented for JavaScript executor. " +
                        "Use evaluate(String code) instead.");
    }

    @Override
    public <T> EvalResult<?> evaluate(String code) {
        return evalInline(code);
    }

    @Override
    public void close() {
        context.close();
    }
}
