package io.github.ih0rd.polyglot.spring.client;

/// Defines the binding strategy between a Java interface
/// and its guest-language implementation.
public enum Convention {

  /// Default adapter convention.
  ///
  /// Resolution rules:
  /// - script name is derived from interface name
  /// - guest class or export must match interface name
  DEFAULT
}
