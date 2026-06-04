package org.javaspec.config;

import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.profile.TargetProfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable top-level javaspec configuration.
 */
public final class JavaspecConfiguration {
    public static final String DEFAULT_FORMATTER = "progress";

    private static final List<String> EMPTY_BOOTSTRAP_HOOKS = Collections.unmodifiableList(new ArrayList<String>());
    private static final JavaspecConfiguration DEFAULT_CONFIGURATION = createDefaultConfiguration();

    private final TargetProfile profile;
    private final String formatter;
    private final ConstructorPolicy constructorPolicy;
    private final String defaultSuiteName;
    private final String jsonReportFile;
    private final String junitXmlReportFile;
    private final List<String> bootstrapHooks;
    private final Map<String, JavaspecSuiteConfiguration> suites;

    private JavaspecConfiguration(
            TargetProfile profile,
            String formatter,
            ConstructorPolicy constructorPolicy,
            String defaultSuiteName,
            String jsonReportFile,
            String junitXmlReportFile,
            List<String> bootstrapHooks,
            List<JavaspecSuiteConfiguration> suites
    ) {
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
        this.formatter = validateRequiredValue("formatter", formatter);
        this.constructorPolicy = Objects.requireNonNull(constructorPolicy, "constructorPolicy must not be null");
        this.defaultSuiteName = validateRequiredValue("defaultSuite", defaultSuiteName);
        this.jsonReportFile = validateOptionalValue("jsonReportFile", jsonReportFile);
        this.junitXmlReportFile = validateOptionalValue("junitXmlReportFile", junitXmlReportFile);
        this.bootstrapHooks = immutableBootstrapHooks(bootstrapHooks);
        this.suites = immutableSuites(suites, this.defaultSuiteName);
    }

    public static JavaspecConfiguration defaults() {
        return DEFAULT_CONFIGURATION;
    }

    public static JavaspecConfiguration of(
            TargetProfile profile,
            String formatter,
            ConstructorPolicy constructorPolicy,
            String defaultSuiteName,
            List<String> bootstrapHooks,
            List<JavaspecSuiteConfiguration> suites
    ) {
        return of(
                profile,
                formatter,
                constructorPolicy,
                defaultSuiteName,
                bootstrapHooks,
                suites,
                null,
                null
        );
    }

    public static JavaspecConfiguration of(
            TargetProfile profile,
            String formatter,
            ConstructorPolicy constructorPolicy,
            String defaultSuiteName,
            List<String> bootstrapHooks,
            List<JavaspecSuiteConfiguration> suites,
            String jsonReportFile,
            String junitXmlReportFile
    ) {
        return new JavaspecConfiguration(
                profile,
                formatter,
                constructorPolicy,
                defaultSuiteName,
                jsonReportFile,
                junitXmlReportFile,
                bootstrapHooks,
                suites
        );
    }

    public static JavaspecConfiguration load(File file) throws IOException {
        return JavaspecConfigurationParser.load(file);
    }

    public static JavaspecConfiguration parse(String content) {
        return JavaspecConfigurationParser.parse(content);
    }

    public TargetProfile profile() {
        return profile;
    }

    public TargetProfile getProfile() {
        return profile;
    }

    public String formatter() {
        return formatter;
    }

    public String getFormatter() {
        return formatter;
    }

    public ConstructorPolicy constructorPolicy() {
        return constructorPolicy;
    }

    public ConstructorPolicy getConstructorPolicy() {
        return constructorPolicy;
    }

    public String defaultSuiteName() {
        return defaultSuiteName;
    }

    public String getDefaultSuiteName() {
        return defaultSuiteName;
    }

    public String report() {
        return jsonReportFile;
    }

    public String getReport() {
        return jsonReportFile;
    }

    public String reportFile() {
        return jsonReportFile;
    }

    public String getReportFile() {
        return jsonReportFile;
    }

    public String jsonReport() {
        return jsonReportFile;
    }

    public String getJsonReport() {
        return jsonReportFile;
    }

    public String jsonReportFile() {
        return jsonReportFile;
    }

    public String getJsonReportFile() {
        return jsonReportFile;
    }

    public String junitXml() {
        return junitXmlReportFile;
    }

    public String getJunitXml() {
        return junitXmlReportFile;
    }

    public String getJUnitXml() {
        return junitXmlReportFile;
    }

    public String junitXmlFile() {
        return junitXmlReportFile;
    }

    public String getJunitXmlFile() {
        return junitXmlReportFile;
    }

    public String getJUnitXmlFile() {
        return junitXmlReportFile;
    }

    public String junitXmlReportFile() {
        return junitXmlReportFile;
    }

    public String getJunitXmlReportFile() {
        return junitXmlReportFile;
    }

    public String getJUnitXmlReportFile() {
        return junitXmlReportFile;
    }

    public JavaspecSuiteConfiguration defaultSuite() {
        return suite(defaultSuiteName);
    }

    public JavaspecSuiteConfiguration getDefaultSuite() {
        return defaultSuite();
    }

    public JavaspecSuiteConfiguration suite(String name) {
        String suiteName = validateRequiredValue("suite name", name);
        JavaspecSuiteConfiguration suite = suites.get(suiteName);
        if (suite == null) {
            throw new ConfigurationException("Suite '" + suiteName + "' is not configured. Available suites: " + availableSuiteNames() + ".");
        }
        return suite;
    }

    public boolean hasSuite(String name) {
        String suiteName = validateRequiredValue("suite name", name);
        return suites.containsKey(suiteName);
    }

    public List<String> bootstrapHooks() {
        return bootstrapHooks;
    }

    public List<String> bootstrap() {
        return bootstrapHooks;
    }

    public List<String> getBootstrapHooks() {
        return bootstrapHooks;
    }

    public List<String> getBootstrap() {
        return bootstrapHooks;
    }

    public Map<String, JavaspecSuiteConfiguration> suites() {
        return suites;
    }

    public Map<String, JavaspecSuiteConfiguration> getSuites() {
        return suites;
    }

    public List<String> suiteNames() {
        return Collections.unmodifiableList(new ArrayList<String>(suites.keySet()));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JavaspecConfiguration)) {
            return false;
        }
        JavaspecConfiguration that = (JavaspecConfiguration) other;
        return profile == that.profile
                && formatter.equals(that.formatter)
                && constructorPolicy == that.constructorPolicy
                && defaultSuiteName.equals(that.defaultSuiteName)
                && Objects.equals(jsonReportFile, that.jsonReportFile)
                && Objects.equals(junitXmlReportFile, that.junitXmlReportFile)
                && bootstrapHooks.equals(that.bootstrapHooks)
                && suites.equals(that.suites);
    }

    @Override
    public int hashCode() {
        int result = profile.hashCode();
        result = 31 * result + formatter.hashCode();
        result = 31 * result + constructorPolicy.hashCode();
        result = 31 * result + defaultSuiteName.hashCode();
        result = 31 * result + (jsonReportFile == null ? 0 : jsonReportFile.hashCode());
        result = 31 * result + (junitXmlReportFile == null ? 0 : junitXmlReportFile.hashCode());
        result = 31 * result + bootstrapHooks.hashCode();
        result = 31 * result + suites.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JavaspecConfiguration{"
                + "profile=" + profile
                + ", formatter='" + formatter + '\''
                + ", constructorPolicy=" + constructorPolicy
                + ", defaultSuiteName='" + defaultSuiteName + '\''
                + ", jsonReportFile='" + jsonReportFile + '\''
                + ", junitXmlReportFile='" + junitXmlReportFile + '\''
                + ", bootstrapHooks=" + bootstrapHooks
                + ", suites=" + suites
                + '}';
    }

    private static JavaspecConfiguration createDefaultConfiguration() {
        List<JavaspecSuiteConfiguration> suites = new ArrayList<JavaspecSuiteConfiguration>();
        suites.add(JavaspecSuiteConfiguration.defaults());
        return new JavaspecConfiguration(
                TargetProfile.JAVA8,
                DEFAULT_FORMATTER,
                ConstructorPolicy.defaultPolicy(),
                JavaspecSuiteConfiguration.DEFAULT_SUITE_NAME,
                null,
                null,
                EMPTY_BOOTSTRAP_HOOKS,
                suites
        );
    }

    private static String validateRequiredValue(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new ConfigurationException(fieldName + " must not be blank.");
        }
        return trimmed;
    }

    private static String validateOptionalValue(String fieldName, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new ConfigurationException(fieldName + " must not be blank.");
        }
        return trimmed;
    }

    private static List<String> immutableBootstrapHooks(List<String> bootstrapHooks) {
        Objects.requireNonNull(bootstrapHooks, "bootstrapHooks must not be null");
        if (bootstrapHooks.size() == 0) {
            return EMPTY_BOOTSTRAP_HOOKS;
        }
        List<String> hooks = new ArrayList<String>();
        for (int i = 0; i < bootstrapHooks.size(); i++) {
            String hook = bootstrapHooks.get(i);
            Objects.requireNonNull(hook, "bootstrap hook must not be null");
            String trimmed = hook.trim();
            if (trimmed.length() == 0) {
                throw new ConfigurationException("bootstrap hook must not be blank.");
            }
            hooks.add(trimmed);
        }
        return Collections.unmodifiableList(hooks);
    }

    private static Map<String, JavaspecSuiteConfiguration> immutableSuites(
            List<JavaspecSuiteConfiguration> suites,
            String defaultSuiteName
    ) {
        Objects.requireNonNull(suites, "suites must not be null");
        if (suites.size() == 0) {
            throw new ConfigurationException("At least one suite must be configured.");
        }
        Map<String, JavaspecSuiteConfiguration> result = new LinkedHashMap<String, JavaspecSuiteConfiguration>();
        for (int i = 0; i < suites.size(); i++) {
            JavaspecSuiteConfiguration suite = suites.get(i);
            Objects.requireNonNull(suite, "suite must not be null");
            if (result.containsKey(suite.name())) {
                throw new ConfigurationException("Duplicate suite name: " + suite.name() + ".");
            }
            result.put(suite.name(), suite);
        }
        if (!result.containsKey(defaultSuiteName)) {
            throw new ConfigurationException("Selected default suite '" + defaultSuiteName
                    + "' is not configured. Available suites: " + availableSuiteNames(result) + ".");
        }
        return Collections.unmodifiableMap(result);
    }

    private String availableSuiteNames() {
        return availableSuiteNames(suites);
    }

    private static String availableSuiteNames(Map<String, JavaspecSuiteConfiguration> suites) {
        if (suites.size() == 0) {
            return "<none>";
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String suiteName : suites.keySet()) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(suiteName);
            index++;
        }
        return builder.toString();
    }
}
