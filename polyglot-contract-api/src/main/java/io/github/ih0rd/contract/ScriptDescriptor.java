package io.github.ih0rd.contract;

public record ScriptDescriptor(
        SupportedLanguage language,
        String source,
        String fileName // optional, may be null
) {}