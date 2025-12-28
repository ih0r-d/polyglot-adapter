package io.github.ih0rd.polyglot.spring.config;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import io.github.ih0rd.adapter.context.JsExecutor;
import io.github.ih0rd.adapter.context.PyExecutor;
import io.github.ih0rd.polyglot.spring.PolyglotExecutors;
import io.github.ih0rd.polyglot.spring.internal.PolyglotStartupLifecycle;
import io.github.ih0rd.polyglot.spring.metrics.PolyglotMetricsBinder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PolyglotAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(PolyglotAutoConfiguration.class)
                    );


    @Test
    void coreDisabled_createsNoExecutors() {
        contextRunner
                .withPropertyValues("polyglot.core.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PyExecutor.class);
                    assertThat(context).doesNotHaveBean(JsExecutor.class);
                    assertThat(context).doesNotHaveBean(PolyglotExecutors.class);
                });
    }

    @Test
    void pythonEnabled_registersExecutorsInfrastructure() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        PolyglotAutoConfiguration.class,
                        PolyglotPythonAutoConfiguration.class
                ))
                .withPropertyValues(
                        "polyglot.core.enabled=true",
                        "polyglot.python.enabled=true",
                        "polyglot.python.resources-path=classpath:python"
                )
                .withBean(PyExecutor.class, Mockito::mock)
                .run(context -> {
                    assertThat(context).hasSingleBean(PolyglotExecutors.class);
                });
    }


    @Test
    void jsDisabled_doesNotCreateJsExecutor() {
        contextRunner
                .withPropertyValues(
                        "polyglot.core.enabled=true",
                        "polyglot.js.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(JsExecutor.class);
                });
    }


    @Test
    void metricsEnabled_registersMetricsBinder() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(
                        PolyglotAutoConfiguration.class,
                        PolyglotMetricsAutoConfiguration.class
                ))
                .withPropertyValues(
                        "polyglot.core.enabled=true",
                        "polyglot.python.enabled=true",
                        "polyglot.python.resources-path=classpath:python",
                        "polyglot.metrics.enabled=true"
                )
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(PolyglotMetricsBinder.class);
                });
    }

    @Test
    void startupLifecycleRegistered() {
        contextRunner
                .withPropertyValues(
                        "polyglot.core.enabled=true",
                        "polyglot.python.enabled=true",
                        "polyglot.python.resources-path=classpath:python"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PolyglotStartupLifecycle.class);
                });
    }

}
