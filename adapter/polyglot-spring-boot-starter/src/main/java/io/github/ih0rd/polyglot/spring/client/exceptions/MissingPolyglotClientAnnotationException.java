package io.github.ih0rd.polyglot.spring.client.exceptions;

import io.github.ih0rd.polyglot.spring.client.PolyglotClient;

/// Thrown when a polyglot client bean is being created,
/// but the target type does not have {@link PolyglotClient}.
public final class MissingPolyglotClientAnnotationException extends PolyglotClientException {

  public MissingPolyglotClientAnnotationException(String className) {
    super("Missing @PolyglotClient on " + className);
  }
}
