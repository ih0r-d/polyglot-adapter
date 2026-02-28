package io.github.ih0rd.polyglot.spring.client;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import io.github.ih0rd.polyglot.spring.client.exceptions.InvalidPolyglotClientTypeException;
import io.github.ih0rd.polyglot.spring.client.exceptions.PolyglotClientClassNotFoundException;
import io.github.ih0rd.polyglot.spring.properties.PolyglotProperties;

/// Scans and registers {@link PolyglotClient} interfaces.
///
/// Resolution priority:
/// 1. {@link EnablePolyglotClients} annotation
/// 2. {@code polyglot.clients.base-packages} properties
/// 3. No scanning if neither is present
///
/// Validation:
/// - only Java interfaces are allowed as polyglot clients
public final class PolyglotClientRegistrar
    implements ImportBeanDefinitionRegistrar, EnvironmentAware {

  private static final Logger log = LoggerFactory.getLogger(PolyglotClientRegistrar.class);

  private Environment environment;

  /// Receives Spring {@link Environment} at the registrar lifecycle stage.
  ///
  /// @param environment Spring environment
  @Override
  public void setEnvironment(@NonNull Environment environment) {
    this.environment = environment;
  }

  /// Registers {@link PolyglotClientFactoryBean} definitions for each discovered client interface.
  ///
  /// @param metadata importing class metadata
  /// @param registry bean definition registry
  @Override
  public void registerBeanDefinitions(
      @NonNull AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry) {
    String[] basePackages = resolveBasePackages(metadata);

    if (basePackages.length == 0) {
      return;
    }

    var scanner =
        new ClassPathScanningCandidateComponentProvider(false) {
          @Override
          protected boolean isCandidateComponent(AnnotatedBeanDefinition bd) {
            return bd.getMetadata().isInterface();
          }
        };
    scanner.addIncludeFilter(new AnnotationTypeFilter(PolyglotClient.class));

    var classLoader = resolveClassLoader();

    for (String basePackage : basePackages) {
      for (var candidate : scanner.findCandidateComponents(basePackage)) {
        String className = candidate.getBeanClassName();
        if (className == null) {
          continue;
        }

        validateClientType(className, classLoader);

        var definition =
            BeanDefinitionBuilder.genericBeanDefinition(PolyglotClientFactoryBean.class)
                .addConstructorArgValue(className)
                .getBeanDefinition();

        registry.registerBeanDefinition(className, definition);
      }
    }
  }

  /// Resolves base packages from @EnablePolyglotClients annotation.
  ///
  /// @param metadata importing class metadata
  /// @return array of base packages, or empty if annotation not present
  private String[] resolveBasePackages(AnnotationMetadata metadata) {

    String annotationName = EnablePolyglotClients.class.getName();

    if (!metadata.hasAnnotation(annotationName)) {
      return new String[0];
    }

    Map<String, Object> attrs =
        Objects.requireNonNull(metadata.getAnnotationAttributes(annotationName));

    String[] basePackages = (String[]) attrs.get("basePackages");
    return (basePackages != null) ? basePackages : new String[0];
  }

  /// Binds {@link PolyglotProperties} from Spring Boot configuration sources.
  ///
  /// @return bound properties or {@code null} if not present
  private PolyglotProperties bindPolyglotProperties() {
    var binder = Binder.get(environment);
    return binder.bind("polyglot", PolyglotProperties.class).orElse(null);
  }

  /// Resolves the most appropriate {@link ClassLoader} for scanning/validation.
  ///
  /// @return class loader
  private ClassLoader resolveClassLoader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl != null) {
      return cl;
    }
    return getClass().getClassLoader();
  }

  /// Validates that the discovered type is a valid polyglot client.
  ///
  /// Rules:
  /// - must be a Java interface
  /// - must not be an annotation type
  ///
  /// @param className   fully qualified class name
  /// @param classLoader class loader to load the class
  /// @throws IllegalStateException if the type is not a valid client interface
  private void validateClientType(String className, ClassLoader classLoader) {
    Class<?> type;
    try {
      type = ClassUtils.forName(className, classLoader);
    } catch (ClassNotFoundException e) {
      throw new PolyglotClientClassNotFoundException(
          "Polyglot client type not found: " + className, e);
    }

    if (!type.isInterface()) {
      throw new InvalidPolyglotClientTypeException(className);
    }

    if (type.isAnnotation()) {
      throw new InvalidPolyglotClientTypeException(className);
    }
  }
}
