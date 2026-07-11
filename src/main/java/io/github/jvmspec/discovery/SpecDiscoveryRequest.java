package io.github.jvmspec.discovery;

import io.github.jvmspec.config.JavaspecSuiteConfiguration;
import io.github.jvmspec.model.DescribedClass;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable input for specification discovery, including naming and optional exact-match filters.
 */
public final class SpecDiscoveryRequest {
    private static final List<String> EMPTY_FILTERS = Collections.unmodifiableList(new ArrayList<String>());

    private final File specRoot;
    private final SpecNamingConvention namingConvention;
    private final String suiteName;
    private final List<String> suiteFilters;
    private final List<String> classFilters;
    private final List<String> exampleFilters;

    private SpecDiscoveryRequest(
            File specRoot,
            SpecNamingConvention namingConvention,
            String suiteName,
            List<String> suiteFilters,
            List<String> classFilters,
            List<String> exampleFilters
    ) {
        this.specRoot = Objects.requireNonNull(specRoot, "specRoot must not be null");
        this.namingConvention = Objects.requireNonNull(namingConvention, "namingConvention must not be null");
        this.suiteName = validateOptionalValue("suiteName", suiteName);
        this.suiteFilters = immutableFilters("suiteFilters", suiteFilters);
        this.classFilters = immutableFilters("classFilters", classFilters);
        this.exampleFilters = immutableFilters("exampleFilters", exampleFilters);
    }

    public static SpecDiscoveryRequest of(File specRoot) {
        return of(specRoot, SpecNamingConvention.defaults());
    }

    public static SpecDiscoveryRequest from(JavaspecSuiteConfiguration suiteConfiguration) {
        Objects.requireNonNull(suiteConfiguration, "suiteConfiguration must not be null");
        return forSuite(
                suiteConfiguration.name(),
                new File(suiteConfiguration.specDirectory()),
                SpecNamingConvention.from(suiteConfiguration)
        );
    }

    public static SpecDiscoveryRequest forRoot(File specRoot) {
        return of(specRoot);
    }

    public static SpecDiscoveryRequest of(File specRoot, SpecNamingConvention namingConvention) {
        return new SpecDiscoveryRequest(
                specRoot,
                namingConvention,
                "",
                EMPTY_FILTERS,
                EMPTY_FILTERS,
                EMPTY_FILTERS
        );
    }

    public static SpecDiscoveryRequest forRoot(File specRoot, SpecNamingConvention namingConvention) {
        return of(specRoot, namingConvention);
    }

    public static SpecDiscoveryRequest forSuite(String suiteName, File specRoot) {
        return of(specRoot).withSuiteName(suiteName);
    }

    public static SpecDiscoveryRequest forSuite(String suiteName, File specRoot, SpecNamingConvention namingConvention) {
        return of(specRoot, namingConvention).withSuiteName(suiteName);
    }

    public File specRoot() {
        return specRoot;
    }

    public File getSpecRoot() {
        return specRoot;
    }

    public SpecNamingConvention namingConvention() {
        return namingConvention;
    }

    public SpecNamingConvention getNamingConvention() {
        return namingConvention;
    }

    public String suiteName() {
        return suiteName;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public List<String> suiteFilters() {
        return suiteFilters;
    }

    public List<String> getSuiteFilters() {
        return suiteFilters;
    }

    public List<String> classFilters() {
        return classFilters;
    }

    public List<String> describedClassFilters() {
        return classFilters;
    }

    public List<String> getClassFilters() {
        return classFilters;
    }

    public List<String> exampleFilters() {
        return exampleFilters;
    }

    public List<String> getExampleFilters() {
        return exampleFilters;
    }

    public boolean hasSuiteFilters() {
        return !suiteFilters.isEmpty();
    }

    public boolean hasClassFilters() {
        return !classFilters.isEmpty();
    }

    public boolean hasExampleFilters() {
        return !exampleFilters.isEmpty();
    }

    public SpecDiscoveryRequest withNamingConvention(SpecNamingConvention namingConvention) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, suiteFilters, classFilters, exampleFilters);
    }

    public SpecDiscoveryRequest withSuiteName(String suiteName) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, suiteFilters, classFilters, exampleFilters);
    }

    public SpecDiscoveryRequest withSuiteFilter(String suiteFilter) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, addFilter(suiteFilters, "suiteFilter", suiteFilter), classFilters, exampleFilters);
    }

    public SpecDiscoveryRequest addSuiteFilter(String suiteFilter) {
        return withSuiteFilter(suiteFilter);
    }

    public SpecDiscoveryRequest withSuiteFilters(List<String> suiteFilters) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, suiteFilters, classFilters, exampleFilters);
    }

    public SpecDiscoveryRequest withClassFilter(String classFilter) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, suiteFilters, addFilter(classFilters, "classFilter", classFilter), exampleFilters);
    }

    public SpecDiscoveryRequest addClassFilter(String classFilter) {
        return withClassFilter(classFilter);
    }

    public SpecDiscoveryRequest withDescribedClassFilter(String classFilter) {
        return withClassFilter(classFilter);
    }

    public SpecDiscoveryRequest withDescribedClassFilter(DescribedClass describedClass) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        return withClassFilter(describedClass.qualifiedName());
    }

    public SpecDiscoveryRequest withClassFilters(List<String> classFilters) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, suiteFilters, classFilters, exampleFilters);
    }

    public SpecDiscoveryRequest withDescribedClassFilters(List<String> classFilters) {
        return withClassFilters(classFilters);
    }

    public SpecDiscoveryRequest withExampleFilter(String exampleFilter) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, suiteFilters, classFilters, addFilter(exampleFilters, "exampleFilter", exampleFilter));
    }

    public SpecDiscoveryRequest addExampleFilter(String exampleFilter) {
        return withExampleFilter(exampleFilter);
    }

    public SpecDiscoveryRequest withExampleFilters(List<String> exampleFilters) {
        return new SpecDiscoveryRequest(specRoot, namingConvention, suiteName, suiteFilters, classFilters, exampleFilters);
    }

    public boolean matchesSuite() {
        if (!hasSuiteFilters()) {
            return true;
        }
        for (int i = 0; i < suiteFilters.size(); i++) {
            if (suiteFilters.get(i).equals(suiteName)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesClass(DescribedClass describedClass, String specQualifiedName) {
        Objects.requireNonNull(describedClass, "describedClass must not be null");
        Objects.requireNonNull(specQualifiedName, "specQualifiedName must not be null");
        if (!hasClassFilters()) {
            return true;
        }
        String specSimpleName = simpleName(specQualifiedName);
        for (int i = 0; i < classFilters.size(); i++) {
            String filter = classFilters.get(i);
            if (filter.equals(describedClass.qualifiedName())
                    || filter.equals(describedClass.simpleName())
                    || filter.equals(specQualifiedName)
                    || filter.equals(specSimpleName)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesExample(SpecExample example) {
        Objects.requireNonNull(example, "example must not be null");
        if (!hasExampleFilters()) {
            return true;
        }
        String orderIndex = Integer.toString(example.sourceOrderIndex());
        for (int i = 0; i < exampleFilters.size(); i++) {
            String filter = exampleFilters.get(i);
            if (filter.equals(example.methodName())
                    || filter.equals(example.displayName())
                    || filter.equals(orderIndex)) {
                return true;
            }
        }
        return false;
    }

    public List<SpecExample> filterExamples(List<SpecExample> examples) {
        Objects.requireNonNull(examples, "examples must not be null");
        List<SpecExample> result = new ArrayList<SpecExample>();
        for (int i = 0; i < examples.size(); i++) {
            SpecExample example = Objects.requireNonNull(examples.get(i), "examples[" + i + "] must not be null");
            if (matchesExample(example)) {
                result.add(example);
            }
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SpecDiscoveryRequest)) {
            return false;
        }
        SpecDiscoveryRequest that = (SpecDiscoveryRequest) other;
        return specRoot.equals(that.specRoot)
                && namingConvention.equals(that.namingConvention)
                && suiteName.equals(that.suiteName)
                && suiteFilters.equals(that.suiteFilters)
                && classFilters.equals(that.classFilters)
                && exampleFilters.equals(that.exampleFilters);
    }

    @Override
    public int hashCode() {
        int result = specRoot.hashCode();
        result = 31 * result + namingConvention.hashCode();
        result = 31 * result + suiteName.hashCode();
        result = 31 * result + suiteFilters.hashCode();
        result = 31 * result + classFilters.hashCode();
        result = 31 * result + exampleFilters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SpecDiscoveryRequest{" +
                "specRoot=" + specRoot +
                ", namingConvention=" + namingConvention +
                ", suiteName='" + suiteName + '\'' +
                ", suiteFilters=" + suiteFilters +
                ", classFilters=" + classFilters +
                ", exampleFilters=" + exampleFilters +
                '}';
    }

    private static List<String> addFilter(List<String> existingFilters, String fieldName, String filter) {
        List<String> filters = new ArrayList<String>(existingFilters);
        filters.add(validateFilter(fieldName, filter));
        return filters;
    }

    private static List<String> immutableFilters(String fieldName, List<String> filters) {
        Objects.requireNonNull(filters, fieldName + " must not be null");
        if (filters.isEmpty()) {
            return EMPTY_FILTERS;
        }
        List<String> copy = new ArrayList<String>();
        for (int i = 0; i < filters.size(); i++) {
            copy.add(validateFilter(fieldName + "[" + i + "]", filters.get(i)));
        }
        return Collections.unmodifiableList(copy);
    }

    private static String validateFilter(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private static String validateOptionalValue(String fieldName, String value) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        return value.trim();
    }

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastDot + 1);
    }
}
