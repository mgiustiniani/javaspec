package io.github.jvmspec.config;

import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.profile.TargetProfile;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        assertNull(configuration.report());
        assertNull(configuration.getReport());
        assertNull(configuration.reportFile());
        assertNull(configuration.getReportFile());
        assertNull(configuration.jsonReport());
        assertNull(configuration.getJsonReport());
        assertNull(configuration.jsonReportFile());
        assertNull(configuration.getJsonReportFile());
        assertNull(configuration.junitXml());
        assertNull(configuration.getJunitXml());
        assertNull(configuration.getJUnitXml());
        assertNull(configuration.junitXmlFile());
        assertNull(configuration.getJunitXmlFile());
        assertNull(configuration.getJUnitXmlFile());
        assertNull(configuration.junitXmlReportFile());
        assertNull(configuration.getJunitXmlReportFile());
        assertNull(configuration.getJUnitXmlReportFile());
        assertTrue(configuration.bootstrapHooks().isEmpty());
        assertFalse(configuration.bootstrapDiscovery());
        assertFalse(configuration.isBootstrapDiscoveryEnabled());
        assertFalse(configuration.getBootstrapDiscovery());
        assertTrue(configuration.extensions().isEmpty());
        assertTrue(configuration.getExtensions().isEmpty());
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
        assertFalse(suite.bootstrapDiscovery());
        assertFalse(suite.isBootstrapDiscoveryEnabled());
        assertFalse(suite.getBootstrapDiscovery());
        assertTrue(suite.extensions().isEmpty());
        assertTrue(suite.getExtensions().isEmpty());
    }

    @Test
    public void bootstrapDiscoveryIsExposedEffectiveAndParticipatesInValueSemantics() {
        JavaspecSuiteConfiguration disabledSuite = JavaspecSuiteConfiguration.of(
                "disabled",
                "spec/disabled",
                "src/disabled",
                "spec",
                "com.example",
                new ArrayList<String>(),
                false
        );
        JavaspecSuiteConfiguration enabledSuite = JavaspecSuiteConfiguration.of(
                "enabled",
                "spec/enabled",
                "src/enabled",
                "spec",
                "com.example",
                new ArrayList<String>(),
                true
        );
        JavaspecConfiguration topLevelEnabled = JavaspecConfiguration.of(
                TargetProfile.JAVA8,
                "progress",
                ConstructorPolicy.COMMENT,
                "disabled",
                new ArrayList<String>(),
                Arrays.asList(disabledSuite, enabledSuite),
                null,
                null,
                true
        );
        JavaspecConfiguration same = JavaspecConfiguration.of(
                TargetProfile.JAVA8,
                "progress",
                ConstructorPolicy.COMMENT,
                "disabled",
                new ArrayList<String>(),
                Arrays.asList(disabledSuite, enabledSuite),
                null,
                null,
                true
        );
        JavaspecConfiguration topLevelDisabled = JavaspecConfiguration.of(
                TargetProfile.JAVA8,
                "progress",
                ConstructorPolicy.COMMENT,
                "disabled",
                new ArrayList<String>(),
                Arrays.asList(disabledSuite, enabledSuite),
                null,
                null,
                false
        );
        JavaspecSuiteConfiguration suiteDisabledCopy = JavaspecSuiteConfiguration.of(
                "enabled",
                "spec/enabled",
                "src/enabled",
                "spec",
                "com.example",
                new ArrayList<String>(),
                false
        );
        JavaspecConfiguration differentSuiteFlag = JavaspecConfiguration.of(
                TargetProfile.JAVA8,
                "progress",
                ConstructorPolicy.COMMENT,
                "disabled",
                new ArrayList<String>(),
                Arrays.asList(disabledSuite, suiteDisabledCopy),
                null,
                null,
                true
        );

        assertTrue(topLevelEnabled.bootstrapDiscovery());
        assertTrue(topLevelEnabled.isBootstrapDiscoveryEnabled());
        assertTrue(topLevelEnabled.getBootstrapDiscovery());
        assertTrue(enabledSuite.bootstrapDiscovery());
        assertTrue(enabledSuite.isBootstrapDiscoveryEnabled());
        assertTrue(enabledSuite.getBootstrapDiscovery());
        assertTrue(topLevelEnabled.effectiveBootstrapDiscovery(disabledSuite));
        assertTrue(topLevelEnabled.effectiveBootstrapDiscovery(enabledSuite));
        assertFalse(topLevelDisabled.effectiveBootstrapDiscovery(disabledSuite));
        assertTrue(topLevelDisabled.effectiveBootstrapDiscovery(enabledSuite));
        assertEquals(same, topLevelEnabled);
        assertEquals(same.hashCode(), topLevelEnabled.hashCode());
        assertFalse(topLevelEnabled.equals(topLevelDisabled));
        assertFalse(topLevelEnabled.equals(differentSuiteFlag));
        assertTrue(topLevelEnabled.toString().contains("bootstrapDiscovery=true"));
        assertTrue(enabledSuite.toString().contains("bootstrapDiscovery=true"));
    }

    @Test
    public void configuredExtensionsAreTrimmedDefensivelyCopiedImmutableAndValueSemantic() {
        List<String> extensions = new ArrayList<String>();
        extensions.add("  org.example.Top  ");
        extensions.add("org.example.Top");
        List<String> suiteExtensions = new ArrayList<String>();
        suiteExtensions.add("\torg.example.Suite\n");
        JavaspecSuiteConfiguration suite = JavaspecSuiteConfiguration.of(
                "custom",
                "spec/custom",
                "src/custom",
                "spec",
                "com.example",
                new ArrayList<String>(),
                suiteExtensions
        );
        List<JavaspecSuiteConfiguration> suites = new ArrayList<JavaspecSuiteConfiguration>();
        suites.add(suite);

        JavaspecConfiguration configuration = JavaspecConfiguration.of(
                TargetProfile.JAVA11,
                "pretty",
                ConstructorPolicy.PRESERVE,
                "custom",
                new ArrayList<String>(),
                extensions,
                suites,
                null,
                null
        );
        JavaspecConfiguration same = JavaspecConfiguration.of(
                TargetProfile.JAVA11,
                "pretty",
                ConstructorPolicy.PRESERVE,
                "custom",
                new ArrayList<String>(),
                Arrays.asList("org.example.Top", "org.example.Top"),
                suites,
                null,
                null
        );
        JavaspecConfiguration differentExtensions = JavaspecConfiguration.of(
                TargetProfile.JAVA11,
                "pretty",
                ConstructorPolicy.PRESERVE,
                "custom",
                new ArrayList<String>(),
                Arrays.asList("org.example.Other"),
                suites,
                null,
                null
        );
        extensions.add("org.example.Mutated");
        suiteExtensions.add("org.example.SuiteMutated");

        assertEquals(Arrays.asList("org.example.Top", "org.example.Top"), configuration.extensions());
        assertSame(configuration.extensions(), configuration.getExtensions());
        assertEquals(Arrays.asList("org.example.Suite"), configuration.suite("custom").extensions());
        assertSame(configuration.suite("custom").extensions(), configuration.suite("custom").getExtensions());
        assertUnmodifiableList(configuration.extensions());
        assertUnmodifiableList(configuration.suite("custom").extensions());
        assertEquals(same, configuration);
        assertEquals(same.hashCode(), configuration.hashCode());
        assertFalse(configuration.equals(differentExtensions));
        assertTrue(configuration.toString().contains("extensions=[org.example.Top, org.example.Top]"));
        assertTrue(configuration.suite("custom").toString().contains("extensions=[org.example.Suite]"));
    }

    @Test
    public void blankConfiguredExtensionsAreRejectedByConfigurationModel() {
        try {
            JavaspecSuiteConfiguration.of("custom", "spec", "src", "spec", "", new ArrayList<String>(),
                    Arrays.asList("org.example.Valid", "  "));
            fail("Expected blank suite extension to be rejected");
        } catch (ConfigurationException expected) {
            assertTrue(expected.getMessage().contains("extension"));
            assertTrue(expected.getMessage().contains("blank"));
        }

        try {
            JavaspecConfiguration.of(
                    TargetProfile.JAVA8,
                    "progress",
                    ConstructorPolicy.COMMENT,
                    "default",
                    new ArrayList<String>(),
                    Arrays.asList("org.example.Valid", "\t"),
                    Arrays.asList(JavaspecSuiteConfiguration.defaults()),
                    null,
                    null
            );
            fail("Expected blank top-level extension to be rejected");
        } catch (ConfigurationException expected) {
            assertTrue(expected.getMessage().contains("extension"));
            assertTrue(expected.getMessage().contains("blank"));
        }
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
    public void reportDestinationsAreTrimmedExposedAndParticipateInValueSemantics() {
        List<JavaspecSuiteConfiguration> suites = new ArrayList<JavaspecSuiteConfiguration>();
        suites.add(JavaspecSuiteConfiguration.defaults());
        List<String> bootstrapHooks = new ArrayList<String>();

        JavaspecConfiguration configuration = JavaspecConfiguration.of(
                TargetProfile.JAVA17,
                "pretty",
                ConstructorPolicy.PRESERVE,
                "default",
                bootstrapHooks,
                suites,
                "  reports/run.json  ",
                "  reports/junit.xml  "
        );
        JavaspecConfiguration same = JavaspecConfiguration.of(
                TargetProfile.JAVA17,
                "pretty",
                ConstructorPolicy.PRESERVE,
                "default",
                bootstrapHooks,
                suites,
                "reports/run.json",
                "reports/junit.xml"
        );
        JavaspecConfiguration differentJsonReport = JavaspecConfiguration.of(
                TargetProfile.JAVA17,
                "pretty",
                ConstructorPolicy.PRESERVE,
                "default",
                bootstrapHooks,
                suites,
                "reports/other.json",
                "reports/junit.xml"
        );
        JavaspecConfiguration differentJunitXmlReport = JavaspecConfiguration.of(
                TargetProfile.JAVA17,
                "pretty",
                ConstructorPolicy.PRESERVE,
                "default",
                bootstrapHooks,
                suites,
                "reports/run.json",
                "reports/other.xml"
        );

        assertEquals("reports/run.json", configuration.report());
        assertEquals("reports/run.json", configuration.getReport());
        assertEquals("reports/run.json", configuration.reportFile());
        assertEquals("reports/run.json", configuration.getReportFile());
        assertEquals("reports/run.json", configuration.jsonReport());
        assertEquals("reports/run.json", configuration.getJsonReport());
        assertEquals("reports/run.json", configuration.jsonReportFile());
        assertEquals("reports/run.json", configuration.getJsonReportFile());
        assertEquals("reports/junit.xml", configuration.junitXml());
        assertEquals("reports/junit.xml", configuration.getJunitXml());
        assertEquals("reports/junit.xml", configuration.getJUnitXml());
        assertEquals("reports/junit.xml", configuration.junitXmlFile());
        assertEquals("reports/junit.xml", configuration.getJunitXmlFile());
        assertEquals("reports/junit.xml", configuration.getJUnitXmlFile());
        assertEquals("reports/junit.xml", configuration.junitXmlReportFile());
        assertEquals("reports/junit.xml", configuration.getJunitXmlReportFile());
        assertEquals("reports/junit.xml", configuration.getJUnitXmlReportFile());
        assertEquals(same, configuration);
        assertEquals(same.hashCode(), configuration.hashCode());
        assertFalse(configuration.equals(differentJsonReport));
        assertFalse(configuration.equals(differentJunitXmlReport));
        assertTrue(configuration.toString().contains("jsonReportFile='reports/run.json'"));
        assertTrue(configuration.toString().contains("junitXmlReportFile='reports/junit.xml'"));
    }

    @Test
    public void blankReportDestinationsAreRejectedByConfigurationModel() {
        List<JavaspecSuiteConfiguration> suites = new ArrayList<JavaspecSuiteConfiguration>();
        suites.add(JavaspecSuiteConfiguration.defaults());
        List<String> bootstrapHooks = new ArrayList<String>();

        try {
            JavaspecConfiguration.of(
                    TargetProfile.JAVA8,
                    "progress",
                    ConstructorPolicy.COMMENT,
                    "default",
                    bootstrapHooks,
                    suites,
                    "   ",
                    null
            );
            fail("Expected blank JSON report destination to be rejected");
        } catch (ConfigurationException expected) {
            assertTrue(expected.getMessage().contains("jsonReportFile"));
            assertTrue(expected.getMessage().contains("blank"));
        }

        try {
            JavaspecConfiguration.of(
                    TargetProfile.JAVA8,
                    "progress",
                    ConstructorPolicy.COMMENT,
                    "default",
                    bootstrapHooks,
                    suites,
                    null,
                    "\t"
            );
            fail("Expected blank JUnit XML report destination to be rejected");
        } catch (ConfigurationException expected) {
            assertTrue(expected.getMessage().contains("junitXmlReportFile"));
            assertTrue(expected.getMessage().contains("blank"));
        }
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
