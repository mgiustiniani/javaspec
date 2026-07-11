package io.github.jvmspec.formatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic registry for run result formatters.
 */
public final class RunFormatterRegistry {
    public static final String FORMATTER_PROGRESS = "progress";
    public static final String FORMATTER_PRETTY = "pretty";
    public static final String FORMATTER_JSON = "json";

    private static final List<String> BUILT_IN_FORMATTER_NAMES = builtInFormatterNamesList();

    private final Map<String, RunFormatter> formatters;

    public RunFormatterRegistry() {
        this.formatters = new LinkedHashMap<String, RunFormatter>();
    }

    public static RunFormatterRegistry builtIn() {
        RunFormatterRegistry registry = new RunFormatterRegistry();
        registry.register(new ProgressRunFormatter());
        registry.register(new PrettyRunFormatter());
        registry.register(new JsonRunFormatter());
        return registry;
    }

    public static RunFormatterRegistry builtIns() {
        return builtIn();
    }

    public static RunFormatterRegistry withBuiltIns() {
        return builtIn();
    }

    public static List<String> builtInFormatterNames() {
        return BUILT_IN_FORMATTER_NAMES;
    }

    public static List<String> builtInNames() {
        return builtInFormatterNames();
    }

    public void register(RunFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter must not be null");
        register(formatter.name(), formatter);
    }

    public void register(String name, RunFormatter formatter) {
        Objects.requireNonNull(formatter, "formatter must not be null");
        String normalizedName = validateName(name);
        formatters.put(normalizedName, formatter);
    }

    public RunFormatter lookup(String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return null;
        }
        return formatters.get(normalizedName);
    }

    public RunFormatter get(String name) {
        return lookup(name);
    }

    public boolean contains(String name) {
        return lookup(name) != null;
    }

    public List<String> formatterNames() {
        return Collections.unmodifiableList(new ArrayList<String>(formatters.keySet()));
    }

    public List<String> names() {
        return formatterNames();
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() == 0) {
            return null;
        }
        return normalized;
    }

    private static String validateName(String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            throw new IllegalArgumentException("formatter name must not be blank");
        }
        return normalizedName;
    }

    private static List<String> builtInFormatterNamesList() {
        List<String> names = new ArrayList<String>();
        names.add(FORMATTER_PROGRESS);
        names.add(FORMATTER_PRETTY);
        names.add(FORMATTER_JSON);
        return Collections.unmodifiableList(names);
    }
}
