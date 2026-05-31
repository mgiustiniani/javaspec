package org.javaspec.config;

import org.javaspec.generation.ConstructorPolicy;
import org.javaspec.profile.TargetProfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Restricted line-based parser for javaspec configuration files.
 */
public final class JavaspecConfigurationParser {
    private static final String SUITE_PREFIX = "suite.";

    private JavaspecConfigurationParser() {
    }

    public static JavaspecConfiguration load(File file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        try {
            return parse(reader);
        } finally {
            reader.close();
        }
    }

    public static JavaspecConfiguration parse(String content) {
        Objects.requireNonNull(content, "content must not be null");
        try {
            return parse(new StringReader(content));
        } catch (IOException ex) {
            throw new IllegalStateException("Unexpected I/O error while reading configuration text.", ex);
        }
    }

    public static JavaspecConfiguration parse(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader must not be null");
        ConfigurationDraft draft = new ConfigurationDraft();
        Map<String, Integer> seenKeys = new LinkedHashMap<String, Integer>();
        BufferedReader bufferedReader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);

        String line;
        int lineNumber = 0;
        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            parseLine(draft, seenKeys, line, lineNumber);
        }
        return draft.toConfiguration();
    }

    private static void parseLine(
            ConfigurationDraft draft,
            Map<String, Integer> seenKeys,
            String line,
            int lineNumber
    ) {
        String trimmedLine = line.trim();
        if (trimmedLine.length() == 0 || trimmedLine.startsWith("#")) {
            return;
        }

        int separatorIndex = separatorIndex(line);
        if (separatorIndex < 0) {
            throw ConfigurationException.atLine(lineNumber,
                    "Malformed configuration line. Expected key/value separator '=' or ':'.");
        }

        String key = line.substring(0, separatorIndex).trim();
        String value = line.substring(separatorIndex + 1).trim();
        if (key.length() == 0) {
            throw ConfigurationException.atLine(lineNumber, "Malformed configuration line. Key must not be blank.");
        }

        ParsedKey parsedKey = parseKey(key, lineNumber);
        Integer firstLineNumber = seenKeys.get(parsedKey.canonicalKey());
        if (firstLineNumber != null) {
            throw ConfigurationException.atLine(lineNumber, "Duplicate configuration key '" + parsedKey.displayKey()
                    + "'. First defined on line " + firstLineNumber + ".");
        }
        seenKeys.put(parsedKey.canonicalKey(), Integer.valueOf(lineNumber));

        apply(draft, parsedKey, value, lineNumber);
    }

    private static int separatorIndex(String line) {
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '=' || character == ':') {
                return i;
            }
        }
        return -1;
    }

    private static ParsedKey parseKey(String key, int lineNumber) {
        if ("profile".equals(key)) {
            return ParsedKey.topLevel(key, KeyKind.PROFILE, "profile");
        }
        if ("formatter".equals(key)) {
            return ParsedKey.topLevel(key, KeyKind.FORMATTER, "formatter");
        }
        if ("constructorPolicy".equals(key) || "constructor-policy".equals(key)) {
            return ParsedKey.topLevel(key, KeyKind.CONSTRUCTOR_POLICY, "constructorPolicy");
        }
        if ("defaultSuite".equals(key) || "default-suite".equals(key)) {
            return ParsedKey.topLevel(key, KeyKind.DEFAULT_SUITE, "defaultSuite");
        }
        if ("bootstrap".equals(key)) {
            return ParsedKey.topLevel(key, KeyKind.BOOTSTRAP, "bootstrap");
        }
        if (key.startsWith(SUITE_PREFIX)) {
            return parseSuiteKey(key, lineNumber);
        }
        throw ConfigurationException.atLine(lineNumber, "Unknown configuration key: " + key + ".");
    }

    private static ParsedKey parseSuiteKey(String key, int lineNumber) {
        String remainder = key.substring(SUITE_PREFIX.length());
        if (remainder.length() == 0) {
            throw ConfigurationException.atLine(lineNumber,
                    "Suite configuration key is missing suite name and property: " + key + ".");
        }

        ParsedKey parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".specDir", KeyKind.SUITE_SPEC_DIRECTORY, "specDir", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".spec-dir", KeyKind.SUITE_SPEC_DIRECTORY, "specDir", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".sourceDir", KeyKind.SUITE_SOURCE_DIRECTORY, "sourceDir", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".source-dir", KeyKind.SUITE_SOURCE_DIRECTORY, "sourceDir", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".specPackagePrefix", KeyKind.SUITE_SPEC_PACKAGE_PREFIX, "specPackagePrefix", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".spec-package-prefix", KeyKind.SUITE_SPEC_PACKAGE_PREFIX, "specPackagePrefix", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".packagePrefix", KeyKind.SUITE_PACKAGE_PREFIX, "packagePrefix", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".package-prefix", KeyKind.SUITE_PACKAGE_PREFIX, "packagePrefix", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }
        parsedKey = parseSuiteKeyWithSuffix(key, remainder, ".bootstrap", KeyKind.SUITE_BOOTSTRAP, "bootstrap", lineNumber);
        if (parsedKey != null) {
            return parsedKey;
        }

        throw ConfigurationException.atLine(lineNumber, "Unknown suite configuration key: " + key + ".");
    }

    private static ParsedKey parseSuiteKeyWithSuffix(
            String key,
            String remainder,
            String suffix,
            KeyKind kind,
            String canonicalProperty,
            int lineNumber
    ) {
        if (!remainder.endsWith(suffix)) {
            return null;
        }
        String suiteName = remainder.substring(0, remainder.length() - suffix.length());
        validateSuiteNameInKey(key, suiteName, lineNumber);
        String canonicalKey = SUITE_PREFIX + suiteName + "." + canonicalProperty;
        return ParsedKey.suite(key, kind, canonicalKey, suiteName);
    }

    private static void validateSuiteNameInKey(String key, String suiteName, int lineNumber) {
        if (suiteName.trim().length() == 0) {
            throw ConfigurationException.atLine(lineNumber, "Suite name in key '" + key + "' must not be blank.");
        }
        if (!suiteName.equals(suiteName.trim())) {
            throw ConfigurationException.atLine(lineNumber,
                    "Suite name in key '" + key + "' must not contain leading or trailing whitespace.");
        }
    }

    private static void apply(ConfigurationDraft draft, ParsedKey parsedKey, String value, int lineNumber) {
        if (parsedKey.kind() == KeyKind.PROFILE) {
            draft.profile = parseProfile(value, lineNumber);
            return;
        }
        if (parsedKey.kind() == KeyKind.FORMATTER) {
            draft.formatter = requiredValue("formatter", value, lineNumber);
            return;
        }
        if (parsedKey.kind() == KeyKind.CONSTRUCTOR_POLICY) {
            draft.constructorPolicy = parseConstructorPolicy(value, lineNumber);
            return;
        }
        if (parsedKey.kind() == KeyKind.DEFAULT_SUITE) {
            draft.defaultSuiteName = requiredValue("defaultSuite", value, lineNumber);
            return;
        }
        if (parsedKey.kind() == KeyKind.BOOTSTRAP) {
            draft.bootstrapHooks = parseBootstrapHooks("bootstrap", value, lineNumber);
            return;
        }

        SuiteDraft suite = draft.suite(parsedKey.suiteName());
        if (parsedKey.kind() == KeyKind.SUITE_SPEC_DIRECTORY) {
            suite.specDirectory = requiredValue("suite '" + suite.name + "' specDir", value, lineNumber);
            return;
        }
        if (parsedKey.kind() == KeyKind.SUITE_SOURCE_DIRECTORY) {
            suite.sourceDirectory = requiredValue("suite '" + suite.name + "' sourceDir", value, lineNumber);
            return;
        }
        if (parsedKey.kind() == KeyKind.SUITE_SPEC_PACKAGE_PREFIX) {
            suite.specPackagePrefix = requiredValue("suite '" + suite.name + "' specPackagePrefix", value, lineNumber);
            return;
        }
        if (parsedKey.kind() == KeyKind.SUITE_PACKAGE_PREFIX) {
            suite.packagePrefix = value.trim();
            return;
        }
        if (parsedKey.kind() == KeyKind.SUITE_BOOTSTRAP) {
            suite.bootstrapHooks = parseBootstrapHooks("suite '" + suite.name + "' bootstrap", value, lineNumber);
        }
    }

    private static TargetProfile parseProfile(String value, int lineNumber) {
        String trimmed = requiredValue("profile", value, lineNumber);
        try {
            return TargetProfile.parse(trimmed);
        } catch (IllegalArgumentException ex) {
            throw ConfigurationException.atLine(lineNumber, "Invalid profile: " + trimmed
                    + ". Valid profiles: " + validProfileKeys() + ".");
        }
    }

    private static ConstructorPolicy parseConstructorPolicy(String value, int lineNumber) {
        try {
            return ConstructorPolicyParser.parse(value);
        } catch (ConfigurationException ex) {
            throw ConfigurationException.atLine(lineNumber, ex.getMessage());
        }
    }

    private static String requiredValue(String fieldName, String value, int lineNumber) {
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw ConfigurationException.atLine(lineNumber, fieldName + " must not be blank.");
        }
        return trimmed;
    }

    private static List<String> parseBootstrapHooks(String fieldName, String value, int lineNumber) {
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw ConfigurationException.atLine(lineNumber, fieldName + " must not be blank.");
        }

        List<String> hooks = new ArrayList<String>();
        int segmentStart = 0;
        int entryIndex = 1;
        while (segmentStart <= trimmed.length()) {
            int comma = trimmed.indexOf(',', segmentStart);
            int segmentEnd = comma < 0 ? trimmed.length() : comma;
            String hook = trimmed.substring(segmentStart, segmentEnd).trim();
            if (hook.length() == 0) {
                throw ConfigurationException.atLine(lineNumber, fieldName
                        + " contains a blank entry at position " + entryIndex + ".");
            }
            hooks.add(hook);
            if (comma < 0) {
                break;
            }
            segmentStart = comma + 1;
            entryIndex++;
        }
        return Collections.unmodifiableList(hooks);
    }

    private static String validProfileKeys() {
        StringBuilder builder = new StringBuilder();
        List<TargetProfile> profiles = TargetProfile.orderedProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(profiles.get(i).key());
        }
        return builder.toString();
    }

    private enum KeyKind {
        PROFILE,
        FORMATTER,
        CONSTRUCTOR_POLICY,
        DEFAULT_SUITE,
        BOOTSTRAP,
        SUITE_SPEC_DIRECTORY,
        SUITE_SOURCE_DIRECTORY,
        SUITE_SPEC_PACKAGE_PREFIX,
        SUITE_PACKAGE_PREFIX,
        SUITE_BOOTSTRAP
    }

    private static final class ParsedKey {
        private final String displayKey;
        private final KeyKind kind;
        private final String canonicalKey;
        private final String suiteName;

        private ParsedKey(String displayKey, KeyKind kind, String canonicalKey, String suiteName) {
            this.displayKey = displayKey;
            this.kind = kind;
            this.canonicalKey = canonicalKey;
            this.suiteName = suiteName;
        }

        static ParsedKey topLevel(String displayKey, KeyKind kind, String canonicalKey) {
            return new ParsedKey(displayKey, kind, canonicalKey, null);
        }

        static ParsedKey suite(String displayKey, KeyKind kind, String canonicalKey, String suiteName) {
            return new ParsedKey(displayKey, kind, canonicalKey, suiteName);
        }

        String displayKey() {
            return displayKey;
        }

        KeyKind kind() {
            return kind;
        }

        String canonicalKey() {
            return canonicalKey;
        }

        String suiteName() {
            return suiteName;
        }
    }

    private static final class ConfigurationDraft {
        private TargetProfile profile = TargetProfile.JAVA8;
        private String formatter = JavaspecConfiguration.DEFAULT_FORMATTER;
        private ConstructorPolicy constructorPolicy = ConstructorPolicy.defaultPolicy();
        private String defaultSuiteName = JavaspecSuiteConfiguration.DEFAULT_SUITE_NAME;
        private List<String> bootstrapHooks = Collections.unmodifiableList(new ArrayList<String>());
        private final Map<String, SuiteDraft> suites = new LinkedHashMap<String, SuiteDraft>();

        ConfigurationDraft() {
            suite(JavaspecSuiteConfiguration.DEFAULT_SUITE_NAME);
        }

        SuiteDraft suite(String name) {
            SuiteDraft suite = suites.get(name);
            if (suite == null) {
                suite = new SuiteDraft(name);
                suites.put(name, suite);
            }
            return suite;
        }

        JavaspecConfiguration toConfiguration() {
            List<JavaspecSuiteConfiguration> suiteConfigurations = new ArrayList<JavaspecSuiteConfiguration>();
            for (SuiteDraft suite : suites.values()) {
                suiteConfigurations.add(suite.toConfiguration());
            }
            return JavaspecConfiguration.of(
                    profile,
                    formatter,
                    constructorPolicy,
                    defaultSuiteName,
                    bootstrapHooks,
                    suiteConfigurations
            );
        }
    }

    private static final class SuiteDraft {
        private final String name;
        private String specDirectory = JavaspecSuiteConfiguration.DEFAULT_SPEC_DIRECTORY;
        private String sourceDirectory = JavaspecSuiteConfiguration.DEFAULT_SOURCE_DIRECTORY;
        private String specPackagePrefix = JavaspecSuiteConfiguration.DEFAULT_SPEC_PACKAGE_PREFIX;
        private String packagePrefix = JavaspecSuiteConfiguration.DEFAULT_PACKAGE_PREFIX;
        private List<String> bootstrapHooks = Collections.unmodifiableList(new ArrayList<String>());

        SuiteDraft(String name) {
            this.name = name;
        }

        JavaspecSuiteConfiguration toConfiguration() {
            return JavaspecSuiteConfiguration.of(
                    name,
                    specDirectory,
                    sourceDirectory,
                    specPackagePrefix,
                    packagePrefix,
                    bootstrapHooks
            );
        }
    }
}
