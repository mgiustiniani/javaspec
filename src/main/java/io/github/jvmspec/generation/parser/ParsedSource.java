package io.github.jvmspec.generation.parser;

import java.util.List;

/**
 * A parsed (or pre-processed) view of a Java source file that answers
 * structural queries needed by the code-generation pipeline.
 *
 * <p>All offsets are byte/character positions in the <em>original</em> source
 * string, so that insertion/replacement operations can be applied to the
 * original text.</p>
 */
public interface ParsedSource {

    /**
     * Returns {@code true} when the source contains a method (or element)
     * declaration whose simple name and parameter type tokens match the given
     * values, ignoring any leading/trailing whitespace and access modifiers.
     *
     * <p>The default implementation normalises whitespace but does not resolve
     * type names; callers are expected to pass the same paramType tokens that
     * they would use in generation.</p>
     *
     * @param methodName  simple method name (e.g. {@code "add"})
     * @param paramTypes  ordered list of parameter type tokens
     *                    (e.g. {@code ["int", "int"]}); may be empty for no-arg methods
     * @return {@code true} if the method is already present
     */
    boolean hasMethod(String methodName, List<String> paramTypes);

    /**
     * Returns the character offset of the closing {@code '}' } brace of the
     * <em>primary</em> type declaration (the outermost class/interface/enum)
     * whose simple name matches {@code typeName}.
     *
     * @param typeName the simple name of the type to locate
     * @return the offset in the original source, or {@code -1} if not found
     */
    int typeClosingBraceOffset(String typeName);

    /**
     * Returns the character offset of the closing {@code '}' } brace of a
     * <em>nested</em> type declaration inside the primary type.
     *
     * @param outerTypeName the simple name of the outer type
     * @param nestedTypeName the simple name of the nested type
     * @return the offset in the original source, or {@code -1} if not found
     */
    int nestedTypeClosingBraceOffset(String outerTypeName, String nestedTypeName);
}
