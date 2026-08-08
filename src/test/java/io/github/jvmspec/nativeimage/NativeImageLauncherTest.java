package io.github.jvmspec.nativeimage;

import io.github.jvmspec.api.ObjectBehavior;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecExample;
import io.github.jvmspec.model.DescribedClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NativeImageLauncherTest {
    @Test
    public void runsBuildLinkedSpecsWithPrettyOutput() throws Exception {
        Invocation invocation = invoke(new String[0], spec(PassingSpec.class, "it_returns_the_value"));

        assertEquals(0, invocation.exitCode);
        assertTrue(invocation.out, invocation.out.contains("PASSED " + PassingSpec.class.getName()
                + "#it_returns_the_value"));
        assertTrue(invocation.out, invocation.out.contains(
                "Examples: 1 total, 1 passed, 0 failed, 0 broken, 0 skipped, 0 pending."));
        assertEquals("", invocation.err);
    }

    @Test
    public void supportsProgressAndJsonFormattersWithoutRuntimeExtensionDiscovery() throws Exception {
        Invocation progress = invoke(
                new String[] {"--formatter", "progress"},
                spec(PassingSpec.class, "it_returns_the_value")
        );
        Invocation json = invoke(
                new String[] {"--formatter=json"},
                spec(PassingSpec.class, "it_returns_the_value")
        );

        assertEquals(0, progress.exitCode);
        assertTrue(progress.out, progress.out.startsWith("Examples: 1 total"));
        assertEquals(0, json.exitCode);
        assertTrue(json.out, json.out.contains("\"schemaVersion\": 1"));
        assertTrue(json.out, json.out.contains("\"status\": \"passed\""));
    }

    @Test
    public void returnsFailureWhenAnExampleFails() throws Exception {
        Invocation invocation = invoke(new String[0], spec(FailingSpec.class, "it_fails"));

        assertEquals(1, invocation.exitCode);
        assertTrue(invocation.out, invocation.out.contains("FAILED " + FailingSpec.class.getName() + "#it_fails"));
        assertTrue(invocation.out, invocation.out.contains("1 failed"));
    }

    @Test
    public void stopsAfterTheFirstFailureWhenRequested() throws Exception {
        Invocation invocation = invoke(
                new String[] {"--stop-on-failure"},
                spec(FailingSpec.class, "it_fails", "it_would_pass")
        );

        assertEquals(1, invocation.exitCode);
        assertTrue(invocation.out, invocation.out.contains("Examples: 1 total"));
    }

    @Test
    public void documentsAndEnforcesTheNativePreviewBoundary() throws Exception {
        Invocation help = invoke(new String[] {"--help"}, Collections.<DiscoveredSpec>emptyList());
        Invocation unsupported = invoke(
                new String[] {"--compile"},
                spec(PassingSpec.class, "it_returns_the_value")
        );

        assertEquals(0, help.exitCode);
        assertTrue(help.out, help.out.contains("linked into this executable at native-image build time"));
        assertTrue(help.out, help.out.contains("No runtime source discovery, compilation, generation"));
        assertEquals(64, unsupported.exitCode);
        assertTrue(unsupported.err, unsupported.err.contains("Unsupported native option: --compile"));
    }

    private Invocation invoke(String[] args, List<DiscoveredSpec> specs) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = NativeImageLauncher.run(
                args,
                specs,
                getClass().getClassLoader(),
                new PrintStream(out, true, "UTF-8"),
                new PrintStream(err, true, "UTF-8")
        );
        return new Invocation(
                exitCode,
                new String(out.toByteArray(), StandardCharsets.UTF_8),
                new String(err.toByteArray(), StandardCharsets.UTF_8)
        );
    }

    private static List<DiscoveredSpec> spec(Class<?> specClass, String... methods) {
        List<SpecExample> examples = new ArrayList<SpecExample>();
        for (int i = 0; i < methods.length; i++) {
            examples.add(SpecExample.of(methods[i], i, 10 + i));
        }
        return Collections.singletonList(DiscoveredSpec.of(
                new File("src/test/java/" + specClass.getName().replace('.', '/') + ".java"),
                specClass.getName(),
                DescribedClass.of(SampleSubject.class.getName()),
                examples
        ));
    }

    private static final class Invocation {
        private final int exitCode;
        private final String out;
        private final String err;

        private Invocation(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }
    }

    public static final class SampleSubject {
        public String value() {
            return "native";
        }
    }

    public static final class PassingSpec extends ObjectBehavior<SampleSubject> {
        public PassingSpec() {
            super(SampleSubject.class);
        }

        public void it_returns_the_value() {
            match(subject().value()).shouldReturn("native");
        }
    }

    public static final class FailingSpec extends ObjectBehavior<SampleSubject> {
        public FailingSpec() {
            super(SampleSubject.class);
        }

        public void it_fails() {
            throw new AssertionError("native red");
        }

        public void it_would_pass() {
            match(subject().value()).shouldReturn("native");
        }
    }
}
