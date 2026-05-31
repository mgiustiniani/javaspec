package org.javaspec.config;

import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.profile.TargetProfile;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaspecConfigurationTest {
    @Test
    public void defaultsUseJava8ProgressCommentAndDefaultSuite() {
        JavaspecConfiguration configuration = JavaspecConfiguration.defaults();

        assertSame(TargetProfile.JAVA8, configuration.profile());
        assertEquals("progress", configuration.formatter());
        assertSame(ConstructorPolicy.COMMENT, configuration.constructorPolicy());
        assertEquals("default", configuration.defaultSuiteName());
        assertTrue(configuration.bootstrapHooks().isEmpty());
        assertEquals(1, configuration.suites().size());
        assertEquals(1, configuration.suiteNames().size());
        assertEquals("default", configuration.suiteNames().get(0));

        JavaspecSuiteConfiguration suite = configuration.defaultSuite();
        assertSame(suite, configuration.suite("default"));
        assertEquals("default", suite.name());
        assertEquals("src/test/java", suite.specDirectory());
        assertEquals("src/main/java", suite.sourceDirectory());
        assertEquals("spec", suite.specPackagePrefix());
        assertEquals("", suite.packagePrefix());
        assertTrue(suite.bootstrapHooks().isEmpty());
    }

    @Test
    public void returnedCollectionsAreImmutableAndDefensivelyCopied() {
        List<String> bootstrapHooks = new ArrayList<String>();
        bootstrapHooks.add("global.Bootstrap");
        List<String> suiteBootstrapHooks = new ArrayList<String>();
        suiteBootstrapHooks.add("suite.Bootstrap");
        JavaspecSuiteConfiguration suite = JavaspecSuiteConfiguration.of(
                "custom",
                "spec/custom",
                "src/custom",
                "spec",
                "com.example",
                suiteBootstrapHooks
        );
        List<JavaspecSuiteConfiguration> suites = new ArrayList<JavaspecSuiteConfiguration>();
        suites.add(suite);

        JavaspecConfiguration configuration = JavaspecConfiguration.of(
                TargetProfile.JAVA8,
                "progress",
                ConstructorPolicy.COMMENT,
                "custom",
                bootstrapHooks,
                suites
        );
        bootstrapHooks.add("global.Mutated");
        suiteBootstrapHooks.add("suite.Mutated");
        suites.clear();

        assertEquals(1, configuration.bootstrapHooks().size());
        assertEquals("global.Bootstrap", configuration.bootstrapHooks().get(0));
        assertEquals(1, configuration.suites().size());
        assertEquals(1, configuration.suite("custom").bootstrapHooks().size());
        assertEquals("suite.Bootstrap", configuration.suite("custom").bootstrapHooks().get(0));

        assertUnmodifiableList(configuration.bootstrapHooks());
        assertUnmodifiableList(configuration.suiteNames());
        assertUnmodifiableMap(configuration.suites());
        assertUnmodifiableList(configuration.suite("custom").bootstrapHooks());
    }

    @Test
    public void missingSuiteExceptionListsAvailableSuites() {
        JavaspecConfiguration configuration = JavaspecConfiguration.defaults();

        try {
            configuration.suite("missing");
            fail("Expected missing suite to throw");
        } catch (ConfigurationException expected) {
            assertTrue(expected.getMessage().contains("Suite 'missing'"));
            assertTrue(expected.getMessage().contains("Available suites"));
            assertTrue(expected.getMessage().contains("default"));
        }
    }

    private static void assertUnmodifiableList(List<String> values) {
        try {
            values.add("mutated");
            fail("Expected list to be immutable");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }

    private static void assertUnmodifiableMap(Map<String, JavaspecSuiteConfiguration> suites) {
        try {
            suites.put("mutated", JavaspecSuiteConfiguration.defaults());
            fail("Expected map to be immutable");
        } catch (UnsupportedOperationException expected) {
            assertTrue(true);
        }
    }
}
