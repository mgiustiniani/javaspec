package org.javaspec.config;

import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.profile.TargetProfile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaspecConfigurationParserTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void parsesSeparatorsCommentsAndTopLevelKeys() {
        String content =
                "# javaspec configuration\n" +
                "\n" +
                " profile : java 11 \n" +
                "formatter = dots\n" +
                "constructorPolicy = preserve\n" +
                "defaultSuite : acceptance\n" +
                "bootstrap = org.example.GlobalHook , org.example.OtherHook\n" +
                "suite.acceptance.specDir = specs/acceptance\n" +
                "suite.acceptance.sourceDir: src/acceptance\n" +
                "suite.acceptance.specPackagePrefix = acceptance.spec\n" +
                "suite.acceptance.packagePrefix: com.example\n" +
                "suite.acceptance.bootstrap = org.example.AcceptanceHook\n";

        JavaspecConfiguration configuration = JavaspecConfigurationParser.parse(content);

        assertSame(TargetProfile.JAVA11, configuration.profile());
        assertEquals("dots", configuration.formatter());
        assertSame(ConstructorPolicy.PRESERVE, configuration.constructorPolicy());
        assertEquals("acceptance", configuration.defaultSuiteName());
        assertEquals(2, configuration.bootstrapHooks().size());
        assertEquals("org.example.GlobalHook", configuration.bootstrapHooks().get(0));
        assertEquals("org.example.OtherHook", configuration.bootstrapHooks().get(1));

        JavaspecSuiteConfiguration suite = configuration.defaultSuite();
        assertEquals("acceptance", suite.name());
        assertEquals("specs/acceptance", suite.specDirectory());
        assertEquals("src/acceptance", suite.sourceDirectory());
        assertEquals("acceptance.spec", suite.specPackagePrefix());
        assertEquals("com.example", suite.packagePrefix());
        assertEquals(1, suite.bootstrapHooks().size());
        assertEquals("org.example.AcceptanceHook", suite.bootstrapHooks().get(0));
    }

    @Test
    public void parsesKebabAliasesMultipleSuitesAndSelectedDefaultSuite() {
        String content =
                "profile=java17\n" +
                "constructor-policy: comment\n" +
                "default-suite: integration\n" +
                "suite.unit.spec-dir=tests/unit\n" +
                "suite.unit.source-dir=src/unit\n" +
                "suite.unit.spec-package-prefix=spec.unit\n" +
                "suite.unit.package-prefix=com.example.unit\n" +
                "suite.unit.bootstrap=unit.One, unit.Two\n" +
                "suite.integration.spec-dir=tests/integration\n" +
                "suite.integration.source-dir=src/integration\n" +
                "suite.integration.spec-package-prefix=spec.integration\n" +
                "suite.integration.package-prefix=   \n" +
                "suite.integration.bootstrap: integration.One\n";

        JavaspecConfiguration configuration = JavaspecConfigurationParser.parse(content);

        assertSame(TargetProfile.JAVA17, configuration.profile());
        assertSame(ConstructorPolicy.COMMENT, configuration.constructorPolicy());
        assertEquals("integration", configuration.defaultSuiteName());
        assertTrue(configuration.hasSuite("unit"));
        assertTrue(configuration.hasSuite("integration"));
        assertSame(configuration.suite("integration"), configuration.defaultSuite());

        JavaspecSuiteConfiguration unit = configuration.suite("unit");
        assertEquals("tests/unit", unit.specDirectory());
        assertEquals("src/unit", unit.sourceDirectory());
        assertEquals("spec.unit", unit.specPackagePrefix());
        assertEquals("com.example.unit", unit.packagePrefix());
        assertEquals(2, unit.bootstrapHooks().size());
        assertEquals("unit.One", unit.bootstrapHooks().get(0));
        assertEquals("unit.Two", unit.bootstrapHooks().get(1));

        JavaspecSuiteConfiguration integration = configuration.suite("integration");
        assertEquals("tests/integration", integration.specDirectory());
        assertEquals("src/integration", integration.sourceDirectory());
        assertEquals("spec.integration", integration.specPackagePrefix());
        assertEquals("", integration.packagePrefix());
        assertEquals(1, integration.bootstrapHooks().size());
        assertEquals("integration.One", integration.bootstrapHooks().get(0));
    }

    @Test
    public void allowsEmptyPackagePrefixButRejectsBlankRequiredSuiteValues() {
        JavaspecConfiguration configuration = JavaspecConfigurationParser.parse("suite.default.packagePrefix=   \n");
        assertEquals("", configuration.suite("default").packagePrefix());

        assertRejected("suite.default.specDir=   \n", "Line 1", "specDir", "blank");
        assertRejected("suite.default.sourceDir: \t\n", "Line 1", "sourceDir", "blank");
        assertRejected("suite.default.specPackagePrefix= \n", "Line 1", "specPackagePrefix", "blank");
        assertRejected("formatter= \n", "Line 1", "formatter", "blank");
        assertRejected("defaultSuite= \n", "Line 1", "defaultSuite", "blank");
    }

    @Test
    public void rejectsInvalidInputWithLineAndFieldDetails() {
        assertRejected(
                "constructorPolicy=comment\n" +
                "constructor-policy=delete\n",
                "Line 2", "Duplicate", "constructor-policy", "line 1"
        );
        assertRejected(
                "suite.default.specDir=specs\n" +
                "suite.default.spec-dir=other\n",
                "Line 2", "Duplicate", "spec-dir", "line 1"
        );
        assertRejected("unknown=value\n", "Line 1", "Unknown", "unknown");
        assertRejected("profile java8\n", "Line 1", "Malformed", "separator");
        assertRejected("suite..specDir=specs\n", "Line 1", "Suite name", "blank");
        assertRejected("profile=java99\n", "Line 1", "Invalid profile", "java99", "Valid profiles");
        assertRejected("constructor-policy=keep\n", "Line 1", "Invalid constructor policy", "keep", "Valid values");
        assertRejected("bootstrap=one,,two\n", "Line 1", "bootstrap", "blank entry", "position 2");
        assertRejected("suite.default.bootstrap=one,\n", "Line 1", "suite 'default' bootstrap", "blank entry");
        assertRejected("defaultSuite=missing\n", "default suite", "missing", "Available suites", "default");
    }

    @Test
    public void loadReadsUtf8ConfigurationFiles() throws Exception {
        File configFile = temporaryFolder.newFile("javaspec.conf");
        String content =
                "formatter=prógress\n" +
                "suite.default.specDir=spécifications\n";
        Files.write(configFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        JavaspecConfiguration configuration = JavaspecConfiguration.load(configFile);

        assertEquals("prógress", configuration.formatter());
        assertEquals("spécifications", configuration.suite("default").specDirectory());
    }

    private static void assertRejected(String content, String firstFragment, String secondFragment) {
        assertRejected(content, new String[] {firstFragment, secondFragment});
    }

    private static void assertRejected(String content, String firstFragment, String secondFragment, String thirdFragment) {
        assertRejected(content, new String[] {firstFragment, secondFragment, thirdFragment});
    }

    private static void assertRejected(
            String content,
            String firstFragment,
            String secondFragment,
            String thirdFragment,
            String fourthFragment
    ) {
        assertRejected(content, new String[] {firstFragment, secondFragment, thirdFragment, fourthFragment});
    }

    private static void assertRejected(
            String content,
            String firstFragment,
            String secondFragment,
            String thirdFragment,
            String fourthFragment,
            String fifthFragment
    ) {
        assertRejected(content, new String[] {firstFragment, secondFragment, thirdFragment, fourthFragment, fifthFragment});
    }

    private static void assertRejected(String content, String[] expectedFragments) {
        try {
            JavaspecConfigurationParser.parse(content);
            fail("Expected configuration to be rejected");
        } catch (ConfigurationException expected) {
            String message = expected.getMessage();
            for (int i = 0; i < expectedFragments.length; i++) {
                assertTrue("Expected message to contain '" + expectedFragments[i] + "' but was: " + message,
                        message.contains(expectedFragments[i]));
            }
        }
    }
}
