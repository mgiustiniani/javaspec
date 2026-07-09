package io.github.jvmspec.generation.parser;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class CommentStrippingSourceParserTest {

    private final CommentStrippingSourceParser parser = new CommentStrippingSourceParser();

    // -------------------------------------------------------------------------
    // name() and isAvailable()

    @Test
    public void nameIsCommentStripping() {
        assertEquals("comment-stripping", parser.name());
    }

    @Test
    public void isAlwaysAvailable() {
        assertTrue(parser.isAvailable());
    }

    // -------------------------------------------------------------------------
    // stripCommentsAndLiterals()

    @Test
    public void stripsLineComment() {
        String source = "int x = 1; // this is a comment\nint y = 2;\n";
        String stripped = CommentStrippingSourceParser.stripCommentsAndLiterals(source);
        assertFalse(stripped.contains("this is a comment"));
        assertTrue(stripped.contains("int x = 1;"));
        assertTrue(stripped.contains("int y = 2;"));
        // Line count must be preserved
        assertEquals(lineCount(source), lineCount(stripped));
    }

    @Test
    public void stripsBlockComment() {
        String source = "/* block\ncomment */\nint x;\n";
        String stripped = CommentStrippingSourceParser.stripCommentsAndLiterals(source);
        assertFalse(stripped.contains("block"));
        assertFalse(stripped.contains("comment"));
        assertTrue(stripped.contains("int x;"));
        assertEquals(lineCount(source), lineCount(stripped));
    }

    @Test
    public void stripsStringLiteral() {
        String source = "String s = \"hello world\";\n";
        String stripped = CommentStrippingSourceParser.stripCommentsAndLiterals(source);
        assertFalse(stripped.contains("hello world"));
        assertTrue(stripped.contains("String s ="));
    }

    @Test
    public void stripsCharLiteral() {
        String source = "char c = 'A';\n";
        String stripped = CommentStrippingSourceParser.stripCommentsAndLiterals(source);
        assertFalse(stripped.contains("'A'"));
        assertTrue(stripped.contains("char c ="));
    }

    @Test
    public void preservesLengthAndNewlines() {
        String source = "/* comment */\nclass Foo {\n    // inline\n    void bar() {}\n}\n";
        String stripped = CommentStrippingSourceParser.stripCommentsAndLiterals(source);
        assertEquals(source.length(), stripped.length());
        assertEquals(lineCount(source), lineCount(stripped));
    }

    @Test
    public void handlesEscapedCharsInStringLiteral() {
        String source = "String s = \"a\\\"b\";\n";
        String stripped = CommentStrippingSourceParser.stripCommentsAndLiterals(source);
        assertFalse("escaped quote content must be stripped", stripped.contains("a\\\"b"));
    }

    // -------------------------------------------------------------------------
    // ParsedSource.hasMethod() — positive cases

    @Test
    public void detectsSimpleNoArgMethod() {
        String source = "class Foo {\n    public void bar() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertTrue(parsed.hasMethod("bar", noParams()));
    }

    @Test
    public void detectsMethodWithOneParam() {
        String source = "class Foo {\n    public void send(String message) {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertTrue(parsed.hasMethod("send", params("String")));
    }

    @Test
    public void detectsMethodWithMultipleParams() {
        String source = "class Foo {\n    int add(int a, int b) { return 0; }\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertTrue(parsed.hasMethod("add", params("int", "int")));
    }

    @Test
    public void detectsMethodNameInCommentIsFalsePositiveAvoided() {
        // The word "bar" appears in a comment but NOT as a real method declaration.
        String source = "class Foo {\n    // void bar() would go here\n    void baz() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertFalse("method name in comment must not be detected as existing",
                parsed.hasMethod("bar", noParams()));
        assertTrue(parsed.hasMethod("baz", noParams()));
    }

    @Test
    public void detectsMethodNameInStringIsFalsePositiveAvoided() {
        String source = "class Foo {\n    String x = \"void bar() {}\";\n    void baz() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertFalse("method name in string literal must not be detected as existing",
                parsed.hasMethod("bar", noParams()));
        assertTrue(parsed.hasMethod("baz", noParams()));
    }

    @Test
    public void detectsGenericMethodWithNestedTypeParameterBoundAndThrowsClause() {
        String source = "class Foo {\n" +
                "    public <T extends Comparable<T>> T max(T left, T right) throws Exception {\n" +
                "        return left.compareTo(right) >= 0 ? left : right;\n" +
                "    }\n" +
                "}\n";
        ParsedSource parsed = parser.parse(source);

        assertTrue(parsed.hasMethod("max", params("T", "T")));
    }

    @Test
    public void detectsAnnotatedVarargsAsArrayParameter() {
        String source = "class Foo {\n" +
                "    public void send(@Deprecated final String... messages) {\n" +
                "    }\n" +
                "}\n";
        ParsedSource parsed = parser.parse(source);

        assertTrue(parsed.hasMethod("send", params("String[]")));
    }

    // -------------------------------------------------------------------------
    // ParsedSource.hasMethod() — negative cases

    @Test
    public void methodAbsentReturnsFalse() {
        String source = "class Foo {\n    void baz() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertFalse(parsed.hasMethod("missing", noParams()));
    }

    @Test
    public void methodWithWrongParamCountReturnsFalse() {
        String source = "class Foo {\n    void foo(int a) {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertFalse(parsed.hasMethod("foo", noParams()));
        assertFalse(parsed.hasMethod("foo", params("int", "int")));
    }

    // -------------------------------------------------------------------------
    // ParsedSource.typeClosingBraceOffset()

    @Test
    public void findsClosingBraceOfSimpleClass() {
        String source = "class Foo {\n    void bar() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        int offset = parsed.typeClosingBraceOffset("Foo");
        assertTrue("closing brace offset must be non-negative", offset >= 0);
        assertEquals('}', source.charAt(offset));
        // The only '}' at top level should be the class closing brace.
        assertEquals(-1, source.indexOf('}', offset + 1));
    }

    @Test
    public void closingBraceOffsetNegativeWhenTypeAbsent() {
        String source = "class Foo {\n    void bar() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertEquals(-1, parsed.typeClosingBraceOffset("Bar"));
    }

    @Test
    public void closingBraceIgnoresBracesInComments() {
        // If we don't strip comments, the extra '}' in the comment would throw off brace counting.
        String source = "class Foo {\n    // fake }\n    void bar() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        int offset = parsed.typeClosingBraceOffset("Foo");
        assertTrue(offset >= 0);
        // Must be the LAST '}' in the file (the class closing brace)
        assertEquals(offset, source.lastIndexOf('}'));
    }

    @Test
    public void closingBraceIgnoresBracesInStrings() {
        String source = "class Foo {\n    String x = \"}\";\n    void bar() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        int offset = parsed.typeClosingBraceOffset("Foo");
        assertTrue(offset >= 0);
        assertEquals(offset, source.lastIndexOf('}'));
    }

    // -------------------------------------------------------------------------
    // ParsedSource.nestedTypeClosingBraceOffset()

    @Test
    public void findsClosingBraceOfNestedClass() {
        String source = "class Outer {\n    class Inner {\n        void foo() {}\n    }\n}\n";
        ParsedSource parsed = parser.parse(source);
        int innerClose = parsed.nestedTypeClosingBraceOffset("Outer", "Inner");
        assertTrue(innerClose >= 0);
        assertEquals('}', source.charAt(innerClose));
        // Inner close must come before outer close
        int outerClose = parsed.typeClosingBraceOffset("Outer");
        assertTrue(innerClose < outerClose);
    }

    @Test
    public void nestedTypeClosingBraceNegativeWhenAbsent() {
        String source = "class Outer {\n    void foo() {}\n}\n";
        ParsedSource parsed = parser.parse(source);
        assertEquals(-1, parsed.nestedTypeClosingBraceOffset("Outer", "Inner"));
    }

    // -------------------------------------------------------------------------
    // JavaSourceParserLoader

    @Test
    public void loaderDefaultParserIsCommentStripping() {
        JavaSourceParser p = JavaSourceParserLoader.defaultParser();
        assertEquals("comment-stripping", p.name());
        assertTrue(p.isAvailable());
    }

    @Test
    public void loaderSelectUsesServiceLoaderProviderWhenPresent() {
        JavaSourceParser p = JavaSourceParserLoader.select(
                Thread.currentThread().getContextClassLoader());
        assertNotNull(p);
        assertTrue(p.isAvailable());
        assertEquals("delegating-test-parser", p.name());
    }

    // -------------------------------------------------------------------------
    // Helpers

    private static List<String> noParams() {
        return Collections.emptyList();
    }

    private static List<String> params(String... types) {
        return Arrays.asList(types);
    }

    private static int lineCount(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
