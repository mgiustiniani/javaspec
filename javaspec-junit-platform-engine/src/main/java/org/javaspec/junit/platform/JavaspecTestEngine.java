package org.javaspec.junit.platform;

import org.javaspec.config.ConfigurationException;
import org.javaspec.config.JavaspecConfiguration;
import org.javaspec.config.JavaspecSuiteConfiguration;
import org.javaspec.discovery.DiscoveredSpec;
import org.javaspec.discovery.SpecDiscovery;
import org.javaspec.discovery.SpecDiscoveryRequest;
import org.javaspec.discovery.SpecExample;
import org.javaspec.discovery.SpecNamingConvention;
import org.javaspec.invocation.JavaspecInvocation;
import org.javaspec.invocation.JavaspecInvocationResult;
import org.javaspec.invocation.JavaspecLauncher;
import org.javaspec.runner.ExampleResult;
import org.javaspec.runner.FailureDetail;
import org.javaspec.runner.RunResult;
import org.javaspec.runner.SpecResult;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional JUnit Platform adapter over canonical javaspec discovery and launching.
 */
public final class JavaspecTestEngine implements TestEngine {
    public static final String ENGINE_ID = "javaspec";

    private static final String ENGINE_DISPLAY_NAME = "javaspec";
    private static final String SPEC_SEGMENT_TYPE = "spec";
    private static final String EXAMPLE_SEGMENT_TYPE = "example";

    private static final String CONFIG_FILE_KEY = "javaspec.configFile";
    private static final String SUITE_KEY = "javaspec.suite";
    private static final String SPEC_DIR_KEY = "javaspec.specDir";
    private static final String SPEC_ROOT_KEY = "javaspec.specRoot";
    private static final String CLASS_FILTERS_KEY = "javaspec.classFilters";
    private static final String CLASS_FILTER_KEY = "javaspec.classFilter";
    private static final String CLASS_KEY = "javaspec.class";
    private static final String EXAMPLE_FILTERS_KEY = "javaspec.exampleFilters";
    private static final String EXAMPLE_FILTER_KEY = "javaspec.exampleFilter";
    private static final String EXAMPLE_KEY = "javaspec.example";
    private static final String STOP_ON_FAILURE_KEY = "javaspec.stopOnFailure";

    public String getId() {
        return ENGINE_ID;
    }

    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        EngineSettings settings = EngineSettings.from(discoveryRequest, uniqueId);
        JavaspecEngineDescriptor engineDescriptor = new JavaspecEngineDescriptor(uniqueId, settings);

        List<DiscoveredSpec> discoveredSpecs = SpecDiscovery.discover(settings.discoveryRequest());
        List<DiscoveredSpec> selectedSpecs = settings.selectorFilter().select(discoveredSpecs, uniqueId);
        for (int i = 0; i < selectedSpecs.size(); i++) {
            DiscoveredSpec spec = selectedSpecs.get(i);
            JavaspecSpecDescriptor specDescriptor = new JavaspecSpecDescriptor(
                    uniqueId.append(SPEC_SEGMENT_TYPE, spec.specQualifiedName()),
                    spec,
                    i
            );
            List<SpecExample> examples = spec.exampleMetadata();
            for (int j = 0; j < examples.size(); j++) {
                SpecExample example = examples.get(j);
                specDescriptor.addChild(new JavaspecExampleDescriptor(
                        specDescriptor.getUniqueId().append(EXAMPLE_SEGMENT_TYPE, example.methodName()),
                        spec.specQualifiedName(),
                        example,
                        j
                ));
            }
            engineDescriptor.addChild(specDescriptor);
        }
        return engineDescriptor;
    }

    public void execute(ExecutionRequest request) {
        EngineExecutionListener listener = request.getEngineExecutionListener();
        TestDescriptor rootDescriptor = request.getRootTestDescriptor();
        listener.executionStarted(rootDescriptor);
        try {
            if (!(rootDescriptor instanceof JavaspecEngineDescriptor)) {
                listener.executionFinished(rootDescriptor, TestExecutionResult.failed(
                        new JavaspecEngineException("Unsupported javaspec root descriptor: " + rootDescriptor.getClass().getName())));
                return;
            }

            JavaspecEngineDescriptor engineDescriptor = (JavaspecEngineDescriptor) rootDescriptor;
            List<JavaspecSpecDescriptor> specDescriptors = specDescriptorsOf(engineDescriptor);
            List<DiscoveredSpec> executionSpecs = executionSpecsOf(specDescriptors);

            JavaspecInvocation invocation = JavaspecInvocation
                    .forSpecs(executionSpecs, engineDescriptor.settings().classLoader())
                    .withStopOnFailure(engineDescriptor.settings().stopOnFailure());
            JavaspecInvocationResult invocationResult = JavaspecLauncher.run(invocation);

            publishResults(listener, specDescriptors, invocationResult.runResult());
            listener.executionFinished(rootDescriptor, TestExecutionResult.successful());
        } catch (Throwable throwable) {
            listener.executionFinished(rootDescriptor, TestExecutionResult.failed(throwable));
        }
    }

    private static List<JavaspecSpecDescriptor> specDescriptorsOf(TestDescriptor rootDescriptor) {
        List<JavaspecSpecDescriptor> descriptors = new ArrayList<JavaspecSpecDescriptor>();
        for (TestDescriptor child : rootDescriptor.getChildren()) {
            if (child instanceof JavaspecSpecDescriptor) {
                descriptors.add((JavaspecSpecDescriptor) child);
            }
        }
        Collections.sort(descriptors, new Comparator<JavaspecSpecDescriptor>() {
            public int compare(JavaspecSpecDescriptor left, JavaspecSpecDescriptor right) {
                return Integer.compare(left.order(), right.order());
            }
        });
        return descriptors;
    }

    private static List<DiscoveredSpec> executionSpecsOf(List<JavaspecSpecDescriptor> specDescriptors) {
        List<DiscoveredSpec> specs = new ArrayList<DiscoveredSpec>();
        for (int i = 0; i < specDescriptors.size(); i++) {
            specs.add(specDescriptors.get(i).toExecutionSpec());
        }
        return specs;
    }

    private static void publishResults(
            EngineExecutionListener listener,
            List<JavaspecSpecDescriptor> specDescriptors,
            RunResult runResult
    ) {
        Map<String, SpecResult> specResults = specResultsByName(runResult);
        Map<String, ExampleResult> exampleResults = exampleResultsByFullName(runResult);

        for (int i = 0; i < specDescriptors.size(); i++) {
            JavaspecSpecDescriptor specDescriptor = specDescriptors.get(i);
            SpecResult specResult = specResults.get(specDescriptor.specQualifiedName());
            List<JavaspecExampleDescriptor> exampleDescriptors = specDescriptor.exampleDescriptors();
            if (specResult != null && !specResult.isExecutable() && exampleDescriptors.isEmpty()) {
                listener.executionSkipped(specDescriptor, skipReason(specResult.notExecutableReason()));
                continue;
            }
            listener.executionStarted(specDescriptor);
            for (int j = 0; j < exampleDescriptors.size(); j++) {
                JavaspecExampleDescriptor exampleDescriptor = exampleDescriptors.get(j);
                ExampleResult exampleResult = exampleResults.get(exampleDescriptor.fullName());
                if (exampleResult == null) {
                    listener.executionSkipped(exampleDescriptor,
                            "Skipped because javaspec stop-on-failure halted execution before this example.");
                    continue;
                }
                publishExampleResult(listener, exampleDescriptor, exampleResult);
            }
            listener.executionFinished(specDescriptor, TestExecutionResult.successful());
        }
    }

    private static void publishExampleResult(
            EngineExecutionListener listener,
            JavaspecExampleDescriptor exampleDescriptor,
            ExampleResult exampleResult
    ) {
        if (exampleResult.isPending()) {
            listener.executionSkipped(exampleDescriptor, pendingReason(exampleResult.detail()));
            return;
        }
        if (exampleResult.isSkipped()) {
            listener.executionSkipped(exampleDescriptor, skipReason(exampleResult.detail()));
            return;
        }
        listener.executionStarted(exampleDescriptor);
        if (exampleResult.isPassed()) {
            listener.executionFinished(exampleDescriptor, TestExecutionResult.successful());
            return;
        }
        listener.executionFinished(exampleDescriptor, TestExecutionResult.failed(throwableFor(exampleResult)));
    }

    private static Map<String, SpecResult> specResultsByName(RunResult runResult) {
        Map<String, SpecResult> results = new LinkedHashMap<String, SpecResult>();
        List<SpecResult> specResults = runResult.specResults();
        for (int i = 0; i < specResults.size(); i++) {
            SpecResult result = specResults.get(i);
            results.put(result.specQualifiedName(), result);
        }
        return results;
    }

    private static Map<String, ExampleResult> exampleResultsByFullName(RunResult runResult) {
        Map<String, ExampleResult> results = new LinkedHashMap<String, ExampleResult>();
        List<ExampleResult> exampleResults = runResult.exampleResults();
        for (int i = 0; i < exampleResults.size(); i++) {
            ExampleResult result = exampleResults.get(i);
            results.put(result.stableId(), result);
        }
        return results;
    }

    private static Throwable throwableFor(ExampleResult result) {
        String message = failureMessage(result);
        Throwable throwable;
        if (result.isFailed()) {
            throwable = new JavaspecAssertionFailure(message);
        } else {
            throwable = new JavaspecBrokenExampleException(message);
        }
        StackTraceElement[] stackTrace = stackTraceFrom(result.failureDetail());
        if (stackTrace.length > 0) {
            throwable.setStackTrace(stackTrace);
        }
        return throwable;
    }

    private static String failureMessage(ExampleResult result) {
        FailureDetail failureDetail = result.failureDetail();
        String detail = result.detail();
        String summary = failureDetail == null ? "" : failureDetail.summary();
        if (isBlank(detail)) {
            return isBlank(summary) ? result.stableId() : summary;
        }
        if (isBlank(summary)) {
            return detail;
        }
        return detail + ": " + summary;
    }

    private static String skipReason(String reason) {
        if (isBlank(reason)) {
            return "Skipped by javaspec.";
        }
        return reason;
    }

    private static String pendingReason(String reason) {
        if (isBlank(reason) || "Pending by javaspec.".equals(reason)) {
            return "Pending by javaspec.";
        }
        return "Pending: " + reason;
    }

    private static StackTraceElement[] stackTraceFrom(FailureDetail failureDetail) {
        if (failureDetail == null || failureDetail.stackTrace().isEmpty()) {
            return new StackTraceElement[0];
        }
        List<StackTraceElement> elements = new ArrayList<StackTraceElement>();
        List<String> lines = failureDetail.stackTrace();
        for (int i = 0; i < lines.size(); i++) {
            StackTraceElement element = stackTraceElementFrom(lines.get(i));
            if (element != null) {
                elements.add(element);
            }
        }
        return elements.toArray(new StackTraceElement[elements.size()]);
    }

    private static StackTraceElement stackTraceElementFrom(String line) {
        if (line == null) {
            return null;
        }
        int open = line.indexOf('(');
        int close = line.lastIndexOf(')');
        if (open <= 0 || close <= open) {
            return null;
        }
        String classAndMethod = line.substring(0, open);
        int methodSeparator = classAndMethod.lastIndexOf('.');
        if (methodSeparator <= 0 || methodSeparator == classAndMethod.length() - 1) {
            return null;
        }
        String className = classAndMethod.substring(0, methodSeparator);
        String methodName = classAndMethod.substring(methodSeparator + 1);
        String location = line.substring(open + 1, close);
        if ("Native Method".equals(location)) {
            return new StackTraceElement(className, methodName, null, -2);
        }
        if ("Unknown Source".equals(location)) {
            return new StackTraceElement(className, methodName, null, -1);
        }
        String fileName = location;
        int lineNumber = -1;
        int lineSeparator = location.lastIndexOf(':');
        if (lineSeparator >= 0 && lineSeparator < location.length() - 1) {
            Integer parsedLineNumber = parseInteger(location.substring(lineSeparator + 1));
            if (parsedLineNumber != null) {
                fileName = location.substring(0, lineSeparator);
                lineNumber = parsedLineNumber.intValue();
            }
        }
        return new StackTraceElement(className, methodName, fileName, lineNumber);
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static final class EngineSettings {
        private final SpecDiscoveryRequest discoveryRequest;
        private final boolean stopOnFailure;
        private final ClassLoader classLoader;
        private final SelectorFilter selectorFilter;

        private EngineSettings(
                SpecDiscoveryRequest discoveryRequest,
                boolean stopOnFailure,
                ClassLoader classLoader,
                SelectorFilter selectorFilter
        ) {
            this.discoveryRequest = discoveryRequest;
            this.stopOnFailure = stopOnFailure;
            this.classLoader = classLoader;
            this.selectorFilter = selectorFilter;
        }

        static EngineSettings from(EngineDiscoveryRequest request, UniqueId engineUniqueId) {
            ConfigurationParameters parameters = request.getConfigurationParameters();
            String configFile = optionalParameter(parameters, CONFIG_FILE_KEY);
            JavaspecConfiguration configuration = configurationFrom(configFile);
            String requestedSuite = optionalParameter(parameters, SUITE_KEY);
            String selectedSuiteName = requestedSuite == null ? configuration.defaultSuiteName() : requestedSuite;
            JavaspecSuiteConfiguration selectedSuite = selectedSuite(
                    configuration,
                    selectedSuiteName,
                    requestedSuite,
                    configFile != null
            );
            SpecNamingConvention namingConvention;
            try {
                namingConvention = SpecNamingConvention.from(selectedSuite);
            } catch (IllegalArgumentException ex) {
                throw new JUnitException("Invalid javaspec naming metadata for suite '" + selectedSuiteName + "'.", ex);
            }

            String specRoot = firstParameter(parameters, SPEC_ROOT_KEY, SPEC_DIR_KEY);
            if (specRoot == null) {
                specRoot = selectedSuite.specDirectory();
            }

            SpecDiscoveryRequest discoveryRequest = SpecDiscoveryRequest
                    .forSuite(selectedSuite.name(), new File(specRoot), namingConvention);
            discoveryRequest = addFilters(discoveryRequest, classFiltersFrom(parameters), exampleFiltersFrom(parameters));

            return new EngineSettings(
                    discoveryRequest,
                    stopOnFailureFrom(parameters),
                    classLoaderFrom(request),
                    SelectorFilter.from(request, engineUniqueId)
            );
        }

        SpecDiscoveryRequest discoveryRequest() {
            return discoveryRequest;
        }

        boolean stopOnFailure() {
            return stopOnFailure;
        }

        ClassLoader classLoader() {
            return classLoader;
        }

        SelectorFilter selectorFilter() {
            return selectorFilter;
        }

        private static JavaspecConfiguration configurationFrom(String configFile) {
            if (configFile == null) {
                return JavaspecConfiguration.defaults();
            }
            try {
                return JavaspecConfiguration.load(new File(configFile));
            } catch (ConfigurationException ex) {
                throw new JUnitException("Invalid javaspec configuration file: " + configFile, ex);
            } catch (IOException ex) {
                throw new JUnitException("Could not read javaspec configuration file: " + configFile, ex);
            } catch (SecurityException ex) {
                throw new JUnitException("Could not read javaspec configuration file: " + configFile, ex);
            }
        }

        private static JavaspecSuiteConfiguration selectedSuite(
                JavaspecConfiguration configuration,
                String selectedSuiteName,
                String requestedSuite,
                boolean configurationFileConfigured
        ) {
            if (configuration.hasSuite(selectedSuiteName)) {
                return configuration.suite(selectedSuiteName);
            }
            if (requestedSuite != null && !configurationFileConfigured) {
                JavaspecSuiteConfiguration defaults = JavaspecSuiteConfiguration.defaults();
                return JavaspecSuiteConfiguration.of(
                        selectedSuiteName,
                        defaults.specDirectory(),
                        defaults.sourceDirectory(),
                        defaults.specPackagePrefix(),
                        defaults.packagePrefix(),
                        defaults.bootstrapHooks()
                );
            }
            if (requestedSuite != null) {
                throw new JUnitException("javaspec suite '" + selectedSuiteName + "' is not configured. Available suites: "
                        + configuration.suiteNames() + ".");
            }
            return configuration.defaultSuite();
        }

        private static SpecDiscoveryRequest addFilters(
                SpecDiscoveryRequest discoveryRequest,
                List<String> classFilters,
                List<String> exampleFilters
        ) {
            SpecDiscoveryRequest result = discoveryRequest;
            for (int i = 0; i < classFilters.size(); i++) {
                result = result.withClassFilter(classFilters.get(i));
            }
            for (int i = 0; i < exampleFilters.size(); i++) {
                result = result.withExampleFilter(exampleFilters.get(i));
            }
            return result;
        }

        private static List<String> classFiltersFrom(ConfigurationParameters parameters) {
            return configuredValues(parameters, CLASS_FILTERS_KEY, CLASS_FILTER_KEY, CLASS_KEY);
        }

        private static List<String> exampleFiltersFrom(ConfigurationParameters parameters) {
            return configuredValues(parameters, EXAMPLE_FILTERS_KEY, EXAMPLE_FILTER_KEY, EXAMPLE_KEY);
        }

        private static boolean stopOnFailureFrom(ConfigurationParameters parameters) {
            Optional<Boolean> value = parameters.getBoolean(STOP_ON_FAILURE_KEY);
            return value.isPresent() && value.get().booleanValue();
        }

        private static ClassLoader classLoaderFrom(EngineDiscoveryRequest request) {
            List<ClassSelector> classSelectors = request.getSelectorsByType(ClassSelector.class);
            for (int i = 0; i < classSelectors.size(); i++) {
                ClassLoader classLoader = classSelectors.get(i).getClassLoader();
                if (classLoader != null) {
                    return classLoader;
                }
            }
            List<MethodSelector> methodSelectors = request.getSelectorsByType(MethodSelector.class);
            for (int i = 0; i < methodSelectors.size(); i++) {
                ClassLoader classLoader = methodSelectors.get(i).getClassLoader();
                if (classLoader != null) {
                    return classLoader;
                }
            }
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                return contextClassLoader;
            }
            return JavaspecTestEngine.class.getClassLoader();
        }
    }

    private static final class SelectorFilter {
        private final List<String> selectedClassNames;
        private final List<String> selectedPackageNames;
        private final List<SelectedMethod> selectedMethods;
        private final List<UniqueId> selectedUniqueIds;
        private final boolean uniqueIdSelectorsPresent;

        private SelectorFilter(
                List<String> selectedClassNames,
                List<String> selectedPackageNames,
                List<SelectedMethod> selectedMethods,
                List<UniqueId> selectedUniqueIds,
                boolean uniqueIdSelectorsPresent
        ) {
            this.selectedClassNames = selectedClassNames;
            this.selectedPackageNames = selectedPackageNames;
            this.selectedMethods = selectedMethods;
            this.selectedUniqueIds = selectedUniqueIds;
            this.uniqueIdSelectorsPresent = uniqueIdSelectorsPresent;
        }

        static SelectorFilter from(EngineDiscoveryRequest request, UniqueId engineUniqueId) {
            List<String> classNames = new ArrayList<String>();
            List<ClassSelector> classSelectors = request.getSelectorsByType(ClassSelector.class);
            for (int i = 0; i < classSelectors.size(); i++) {
                classNames.add(classSelectors.get(i).getClassName());
            }

            List<String> packageNames = new ArrayList<String>();
            List<PackageSelector> packageSelectors = request.getSelectorsByType(PackageSelector.class);
            for (int i = 0; i < packageSelectors.size(); i++) {
                packageNames.add(packageSelectors.get(i).getPackageName());
            }

            List<SelectedMethod> methods = new ArrayList<SelectedMethod>();
            List<MethodSelector> methodSelectors = request.getSelectorsByType(MethodSelector.class);
            for (int i = 0; i < methodSelectors.size(); i++) {
                MethodSelector selector = methodSelectors.get(i);
                methods.add(new SelectedMethod(selector.getClassName(), selector.getMethodName()));
            }

            List<UniqueId> uniqueIds = new ArrayList<UniqueId>();
            List<UniqueIdSelector> uniqueIdSelectors = request.getSelectorsByType(UniqueIdSelector.class);
            for (int i = 0; i < uniqueIdSelectors.size(); i++) {
                UniqueId uniqueId = uniqueIdSelectors.get(i).getUniqueId();
                if (isEngineUniqueId(uniqueId, engineUniqueId)) {
                    uniqueIds.add(uniqueId);
                }
            }

            return new SelectorFilter(
                    immutableStrings(classNames),
                    immutableStrings(packageNames),
                    Collections.unmodifiableList(methods),
                    Collections.unmodifiableList(uniqueIds),
                    !uniqueIdSelectors.isEmpty()
            );
        }

        List<DiscoveredSpec> select(List<DiscoveredSpec> specs, UniqueId engineUniqueId) {
            if (!hasSelectors()) {
                return specs;
            }
            List<DiscoveredSpec> selected = new ArrayList<DiscoveredSpec>();
            for (int i = 0; i < specs.size(); i++) {
                DiscoveredSpec spec = specs.get(i);
                DiscoveredSpec filtered = filteredSpec(spec, engineUniqueId);
                if (filtered != null) {
                    selected.add(filtered);
                }
            }
            return Collections.unmodifiableList(selected);
        }

        private DiscoveredSpec filteredSpec(DiscoveredSpec spec, UniqueId engineUniqueId) {
            UniqueId specUniqueId = engineUniqueId.append(SPEC_SEGMENT_TYPE, spec.specQualifiedName());
            boolean includeAllExamples = matchesSpec(spec, specUniqueId);
            List<SpecExample> examples = new ArrayList<SpecExample>();
            List<SpecExample> originalExamples = spec.exampleMetadata();
            for (int i = 0; i < originalExamples.size(); i++) {
                SpecExample example = originalExamples.get(i);
                UniqueId exampleUniqueId = specUniqueId.append(EXAMPLE_SEGMENT_TYPE, example.methodName());
                if (includeAllExamples || matchesExample(spec, example, exampleUniqueId)) {
                    examples.add(example);
                }
            }
            if (!includeAllExamples && examples.isEmpty()) {
                return null;
            }
            return DiscoveredSpec.of(spec.specFile(), spec.specQualifiedName(), spec.describedType(), examples);
        }

        private boolean matchesSpec(DiscoveredSpec spec, UniqueId specUniqueId) {
            for (int i = 0; i < selectedClassNames.size(); i++) {
                if (matchesClassName(spec, selectedClassNames.get(i))) {
                    return true;
                }
            }
            for (int i = 0; i < selectedPackageNames.size(); i++) {
                if (matchesPackageName(spec, selectedPackageNames.get(i))) {
                    return true;
                }
            }
            for (int i = 0; i < selectedUniqueIds.size(); i++) {
                if (specUniqueId.hasPrefix(selectedUniqueIds.get(i))) {
                    return true;
                }
            }
            return false;
        }

        private boolean matchesExample(DiscoveredSpec spec, SpecExample example, UniqueId exampleUniqueId) {
            for (int i = 0; i < selectedMethods.size(); i++) {
                SelectedMethod method = selectedMethods.get(i);
                if (matchesClassName(spec, method.className()) && method.methodName().equals(example.methodName())) {
                    return true;
                }
            }
            for (int i = 0; i < selectedUniqueIds.size(); i++) {
                if (exampleUniqueId.hasPrefix(selectedUniqueIds.get(i))) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasSelectors() {
            return !selectedClassNames.isEmpty()
                    || !selectedPackageNames.isEmpty()
                    || !selectedMethods.isEmpty()
                    || uniqueIdSelectorsPresent;
        }

        private static boolean matchesClassName(DiscoveredSpec spec, String className) {
            String specQualifiedName = spec.specQualifiedName();
            String specSimpleName = simpleName(specQualifiedName);
            String describedQualifiedName = spec.describedClass().qualifiedName();
            String describedSimpleName = spec.describedClass().simpleName();
            return className.equals(specQualifiedName)
                    || className.equals(specSimpleName)
                    || className.equals(describedQualifiedName)
                    || className.equals(describedSimpleName);
        }

        private static boolean matchesPackageName(DiscoveredSpec spec, String packageName) {
            return isPackageOrChild(packageName, packageNameOf(spec.specQualifiedName()))
                    || isPackageOrChild(packageName, spec.describedClass().packageName());
        }

        private static boolean isPackageOrChild(String selectedPackage, String candidatePackage) {
            if (selectedPackage.equals(candidatePackage)) {
                return true;
            }
            return candidatePackage.startsWith(selectedPackage + ".");
        }

        private static String packageNameOf(String qualifiedName) {
            int lastDot = qualifiedName.lastIndexOf('.');
            if (lastDot < 0) {
                return "";
            }
            return qualifiedName.substring(0, lastDot);
        }

        private static boolean isEngineUniqueId(UniqueId uniqueId, UniqueId engineUniqueId) {
            Optional<String> selectedEngineId = uniqueId.getEngineId();
            if (!selectedEngineId.isPresent() || !ENGINE_ID.equals(selectedEngineId.get())) {
                return false;
            }
            return uniqueId.hasPrefix(engineUniqueId) || engineUniqueId.hasPrefix(uniqueId);
        }
    }

    private static final class SelectedMethod {
        private final String className;
        private final String methodName;

        private SelectedMethod(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        String className() {
            return className;
        }

        String methodName() {
            return methodName;
        }
    }

    private static final class JavaspecEngineDescriptor extends EngineDescriptor {
        private final EngineSettings settings;

        private JavaspecEngineDescriptor(UniqueId uniqueId, EngineSettings settings) {
            super(uniqueId, ENGINE_DISPLAY_NAME);
            this.settings = settings;
        }

        EngineSettings settings() {
            return settings;
        }
    }

    private static final class JavaspecSpecDescriptor extends AbstractTestDescriptor {
        private final DiscoveredSpec spec;
        private final int order;

        private JavaspecSpecDescriptor(UniqueId uniqueId, DiscoveredSpec spec, int order) {
            super(uniqueId, spec.specQualifiedName(), ClassSource.from(spec.specQualifiedName()));
            this.spec = spec;
            this.order = order;
        }

        public Type getType() {
            return Type.CONTAINER;
        }

        public String getLegacyReportingName() {
            return spec.specQualifiedName();
        }

        int order() {
            return order;
        }

        String specQualifiedName() {
            return spec.specQualifiedName();
        }

        DiscoveredSpec toExecutionSpec() {
            List<JavaspecExampleDescriptor> exampleDescriptors = exampleDescriptors();
            List<SpecExample> examples = new ArrayList<SpecExample>();
            for (int i = 0; i < exampleDescriptors.size(); i++) {
                examples.add(exampleDescriptors.get(i).example());
            }
            return DiscoveredSpec.of(spec.specFile(), spec.specQualifiedName(), spec.describedType(), examples);
        }

        List<JavaspecExampleDescriptor> exampleDescriptors() {
            List<JavaspecExampleDescriptor> descriptors = new ArrayList<JavaspecExampleDescriptor>();
            for (TestDescriptor child : getChildren()) {
                if (child instanceof JavaspecExampleDescriptor) {
                    descriptors.add((JavaspecExampleDescriptor) child);
                }
            }
            Collections.sort(descriptors, new Comparator<JavaspecExampleDescriptor>() {
                public int compare(JavaspecExampleDescriptor left, JavaspecExampleDescriptor right) {
                    return Integer.compare(left.order(), right.order());
                }
            });
            return descriptors;
        }
    }

    private static final class JavaspecExampleDescriptor extends AbstractTestDescriptor {
        private final String specQualifiedName;
        private final SpecExample example;
        private final int order;

        private JavaspecExampleDescriptor(UniqueId uniqueId, String specQualifiedName, SpecExample example, int order) {
            super(uniqueId, stableExampleId(specQualifiedName, example),
                    MethodSource.from(specQualifiedName, example.methodName()));
            this.specQualifiedName = specQualifiedName;
            this.example = example;
            this.order = order;
        }

        public Type getType() {
            return Type.TEST;
        }

        public String getLegacyReportingName() {
            return stableId();
        }

        int order() {
            return order;
        }

        SpecExample example() {
            return example;
        }

        String fullName() {
            return stableId();
        }

        String stableId() {
            return stableExampleId(specQualifiedName, example);
        }

        private static String stableExampleId(String specQualifiedName, SpecExample example) {
            return example.stableId(specQualifiedName);
        }
    }

    private static List<String> configuredValues(ConfigurationParameters parameters, String pluralKey, String firstAlias, String secondAlias) {
        List<String> values = new ArrayList<String>();
        addConfiguredValues(values, parameters, pluralKey);
        addConfiguredValues(values, parameters, firstAlias);
        addConfiguredValues(values, parameters, secondAlias);
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(values);
    }

    private static void addConfiguredValues(List<String> values, ConfigurationParameters parameters, String key) {
        String value = optionalParameter(parameters, key);
        if (value != null) {
            addDelimitedValues(values, value);
        }
    }

    private static void addDelimitedValues(List<String> values, String value) {
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == ',' || value.charAt(i) == ';') {
                addTrimmed(values, value.substring(start, i));
                start = i + 1;
            }
        }
    }

    private static void addTrimmed(List<String> values, String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 0) {
            values.add(trimmed);
        }
    }

    private static String firstParameter(ConfigurationParameters parameters, String preferredKey, String fallbackKey) {
        String preferred = optionalParameter(parameters, preferredKey);
        if (preferred != null) {
            return preferred;
        }
        return optionalParameter(parameters, fallbackKey);
    }

    private static String optionalParameter(ConfigurationParameters parameters, String key) {
        Optional<String> value = parameters.get(key);
        if (!value.isPresent()) {
            return null;
        }
        String trimmed = value.get().trim();
        if (trimmed.length() == 0) {
            throw new JUnitException("javaspec configuration parameter '" + key + "' must not be blank.");
        }
        return trimmed;
    }

    private static List<String> immutableStrings(List<String> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<String>();
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (!isBlank(value)) {
                copy.add(value.trim());
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static String simpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastDot + 1);
    }

    private static final class JavaspecAssertionFailure extends AssertionError {
        private static final long serialVersionUID = 1L;

        private JavaspecAssertionFailure(String message) {
            super(message);
        }
    }

    private static final class JavaspecBrokenExampleException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private JavaspecBrokenExampleException(String message) {
            super(message);
        }
    }

    private static final class JavaspecEngineException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private JavaspecEngineException(String message) {
            super(message);
        }
    }
}
