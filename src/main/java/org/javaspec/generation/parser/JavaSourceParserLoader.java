package org.javaspec.generation.parser;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Discovers and selects the best available {@link JavaSourceParser}.
 *
 * <p>External providers registered via
 * {@code META-INF/services/org.javaspec.generation.parser.JavaSourceParser}
 * are checked first (in registration order).  The built-in
 * {@link CommentStrippingSourceParser} is used if no external provider is
 * available or reports {@link JavaSourceParser#isAvailable() isAvailable()} as
 * {@code false}.</p>
 */
public final class JavaSourceParserLoader {

    private JavaSourceParserLoader() {
    }

    /**
     * Returns the best available parser for use in the current JVM.
     *
     * @param classLoader the class loader used for ServiceLoader discovery
     * @return the selected parser; never {@code null}
     */
    public static JavaSourceParser select(ClassLoader classLoader) {
        ServiceLoader<JavaSourceParser> loader =
                ServiceLoader.load(JavaSourceParser.class, classLoader);
        Iterator<JavaSourceParser> it = loader.iterator();
        while (it.hasNext()) {
            JavaSourceParser parser;
            try {
                parser = it.next();
            } catch (Exception ex) {
                // Skip broken providers.
                continue;
            }
            if (parser.isAvailable()) {
                return parser;
            }
        }
        return new CommentStrippingSourceParser();
    }

    /**
     * Returns the default built-in parser without any ServiceLoader discovery.
     * Useful for tests and contexts where the class loader is not readily available.
     *
     * @return the built-in {@link CommentStrippingSourceParser}
     */
    public static JavaSourceParser defaultParser() {
        return new CommentStrippingSourceParser();
    }
}
