package io.github.ih0rd.adapter.api.executors;

import static io.github.ih0rd.adapter.utils.CommonUtils.*;
import static io.github.ih0rd.adapter.utils.Constants.PYTHON;
import static io.github.ih0rd.adapter.utils.StringCaseConverter.camelToSnake;

import io.github.ih0rd.adapter.api.context.EvalResult;
import io.github.ih0rd.adapter.api.context.Language;
import io.github.ih0rd.adapter.api.context.PolyglotContextFactory;
import io.github.ih0rd.adapter.exceptions.EvaluationException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.*;

/**
 * Python executor implementation for GraalPy.
 * Loads Python classes and invokes their methods via polyglot API.
 */
public record PyExecutor(Context context, Path resourcesPath) implements BaseExecutor {

    @Override
    public String languageId() {
        return PYTHON;
    }

    @Override
    public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType, Object... args) {
        var instance = mapValue(memberTargetType, methodName);
        return invokeMethod(memberTargetType, instance, methodName, args);
    }

    @Override
    public <T> EvalResult<?> evaluate(String methodName, Class<T> memberTargetType) {
        var instance = mapValue(memberTargetType, methodName);
        return invokeMethod(memberTargetType, instance, methodName);
    }

    @Override
    public <T> EvalResult<?> evaluate(String code) {
        return evalInline(code);
    }

    public static PyExecutor createDefault() {
        return BaseExecutor.createDefault(Language.PYTHON, PyExecutor::new);
    }

    public static PyExecutor create(PolyglotContextFactory.Builder builder) {
        return BaseExecutor.create(Language.PYTHON, builder, PyExecutor::new);
    }

    private <T> T mapValue(Class<T> memberTargetType, String methodName) {
        var source = getFileSource(memberTargetType);
        context.eval(source);

        var bindings = context.getPolyglotBindings();
        var pyClass = getFirstElement(bindings.getMemberKeys());
        validate(pyClass, memberTargetType);

        var member = bindings.getMember(pyClass);
        if (!checkIfMethodExists(memberTargetType, methodName)
                || member.getMember(methodName) == null) {
            throw new EvaluationException(
                    "Method " + methodName + " is not supported for " + memberTargetType.getSimpleName());
        }

        return member.newInstance().as(memberTargetType);
    }

    private <T> void validate(String pyClassName, Class<T> memberTargetType) {
        if (pyClassName == null || pyClassName.isEmpty()) {
            throw new EvaluationException("Invalid Python class name: " + pyClassName);
        }

        var interfaceName = memberTargetType.getSimpleName();
        if (!interfaceName.equals(pyClassName)) {
            throw new EvaluationException(
                    "Interface name '" + interfaceName + "' must equal Python class name '" + pyClassName + "'");
        }
    }

    private <T> Source getFileSource(Class<T> memberTargetType) {
        return SOURCE_CACHE.computeIfAbsent(memberTargetType, this::loadSource);
    }

    private <T> Source loadSource(Class<T> memberTargetType) {
        var interfaceName = memberTargetType.getSimpleName();
        var pyFileName = camelToSnake(interfaceName);
        var resourcePath = "python/" + pyFileName + ".py";
        var cl = Thread.currentThread().getContextClassLoader();

        try (InputStream is = cl.getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return Source.newBuilder(PYTHON, reader, resourcePath).build();
                }
            }
        } catch (Exception e) {
            throw new EvaluationException("Failed to read Python script from classpath: " + resourcePath, e);
        }

        var fsPath = resourcesPath.resolve(pyFileName + ".py");
        if (!Files.exists(fsPath)) {
            throw new EvaluationException(
                    "Cannot find Python file: " + pyFileName +
                            " (classpath '" + resourcePath + "', filesystem '" + fsPath + "')");
        }

        try {
            return Source.newBuilder(PYTHON, fsPath.toFile()).build();
        } catch (Exception e) {
            throw new EvaluationException("Could not load Python file: " + pyFileName, e);
        }
    }

    @Override
    public void close() {
        context.close();
    }
}
