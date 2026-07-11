package io.github.jvmspec.discovery;

import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Behaviors that require language-aware (AST) spec parsing: comments, string literals and
 * multi-line expressions must be understood the way the Java language defines them, not the
 * way a regular expression approximates them.
 */
public class SpecDiscoveryAstTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private List<DiscoveredSpec> discover(String specSimpleName, String specSource, String folder) throws Exception {
        File specRoot = temporaryFolder.newFolder(folder);
        File specDir = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example");
        assertTrue(specDir.mkdirs());
        File specFile = new File(specDir, specSimpleName + ".java");
        Files.write(specFile.toPath(), specSource.getBytes(StandardCharsets.UTF_8));
        return SpecDiscovery.discover(specRoot);
    }

    @Test
    public void ignoresCommentedOutConstructionMarkersAndCalls() throws Exception {
        String specSource =
                "package spec.com.example;\n\n" +
                "import com.example.Writer;\n" +
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
                "public class MarkdownSpec extends ObjectBehavior<Markdown> {\n" +
                "    public void it_is_initializable() {\n" +
                "        // beConstructedWith(writer);\n" +
                "        /* beConstructedWith(reader); */\n" +
                "        // subject().obsoleteCall();\n" +
                "        shouldHaveType(Markdown.class);\n" +
                "    }\n" +
                "}\n";

        List<DiscoveredSpec> specs = discover("MarkdownSpec", specSource, "ast-comments-root");

        assertEquals(1, specs.size());
        DescribedType type = specs.get(0).describedType();
        assertFalse("commented-out beConstructedWith must not produce a constructor",
                type.hasConstructors());
        for (int i = 0; i < type.methods().size(); i++) {
            assertFalse("commented-out subject call must not produce a method descriptor",
                    "obsoleteCall".equals(type.methods().get(i).methodName()));
        }
    }

    @Test
    public void ignoresConstructionMarkersInsideStringLiterals() throws Exception {
        String specSource =
                "package spec.com.example;\n\n" +
                "import io.github.jvmspec.api.ObjectBehavior;\n\n" +
                "public class DocSpec extends ObjectBehavior<Doc> {\n" +
                "    public void it_documents_the_api() {\n" +
                "        String example = \"beConstructedWith(writer); subject().fake();\";\n" +
                "        render(example).shouldContain(\"beConstructedWith\");\n" +
                "    }\n" +
                "}\n";

        List<DiscoveredSpec> specs = discover("DocSpec", specSource, "ast-string-root");

        assertEquals(1, specs.size());
        DescribedType type = specs.get(0).describedType();
        assertFalse("construction marker inside a string literal must not produce a constructor",
                type.hasConstructors());
        boolean fakeDiscovered = false;
        boolean renderDiscovered = false;
        for (int i = 0; i < type.methods().size(); i++) {
            String name = type.methods().get(i).methodName();
            if ("fake".equals(name)) {
                fakeDiscovered = true;
            }
            if ("render".equals(name)) {
                renderDiscovered = true;
            }
        }
        assertFalse("subject call inside a string literal must not produce a method", fakeDiscovered);
        assertTrue("real proxy expectation must still be discovered", renderDiscovered);
    }

    @Test
    public void discoversProxyExpectationSpanningMultipleLinesWithInlineComment() throws Exception {
        String specSource =
                "package spec.com.example;\n\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "    public void it_totals() {\n" +
                "        total(10.0,\n" +
                "              2.5) // running total\n" +
                "            .shouldReturn(12.5);\n" +
                "    }\n" +
                "}\n";

        List<DiscoveredSpec> specs = discover("CalculatorSpec", specSource, "ast-multiline-root");

        assertEquals(1, specs.size());
        assertEquals(Arrays.asList(
                MethodDescriptor.of("total", "double", Arrays.asList("double", "double"), Arrays.asList("arg0", "arg1"))
        ), specs.get(0).describedType().methods());
    }
}
