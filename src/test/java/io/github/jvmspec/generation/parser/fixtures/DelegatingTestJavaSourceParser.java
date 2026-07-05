package io.github.jvmspec.generation.parser.fixtures;

import io.github.jvmspec.generation.parser.CommentStrippingSourceParser;
import io.github.jvmspec.generation.parser.JavaSourceParser;
import io.github.jvmspec.generation.parser.ParsedSource;

/** ServiceLoader-visible parser provider used to prove external parser discovery works. */
public final class DelegatingTestJavaSourceParser implements JavaSourceParser {
    private final CommentStrippingSourceParser delegate = new CommentStrippingSourceParser();

    public String name() {
        return "delegating-test-parser";
    }

    public boolean isAvailable() {
        return true;
    }

    public ParsedSource parse(String source) {
        return delegate.parse(source);
    }
}
