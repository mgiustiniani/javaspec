package org.javaspec.generation.parser;

/**
 * SPI for parsing Java source text and answering structural queries.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}; the
 * built-in {@link CommentStrippingSourceParser} is always available as the
 * default.  An external implementation registered in
 * {@code META-INF/services/org.javaspec.generation.parser.JavaSourceParser}
 * is consulted first if it reports {@link #isAvailable()}.</p>
 *
 * <p>All methods receive the original, unmodified source text.
 * Implementations are responsible for handling comments, string literals, and
 * other lexical noise before applying structural queries.</p>
 */
public interface JavaSourceParser {

    /** Human-readable name for diagnostic output. */
    String name();

    /**
     * Returns {@code true} when this parser can be used in the current JVM
     * environment (e.g. the required tools are on the classpath).
     */
    boolean isAvailable();

    /**
     * Returns a view of the parsed source that can answer structural queries.
     *
     * @param source the full Java source text
     * @return a parsed view; never {@code null}
     */
    ParsedSource parse(String source);
}
