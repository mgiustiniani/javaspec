package io.github.jvmspec.compatibility.language;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.cli.run.GenerationOrchestrator;
import io.github.jvmspec.cli.run.GenerationOrchestratorResult;
import io.github.jvmspec.generation.ClassConstructorUpdater;
import io.github.jvmspec.generation.ClassMethodUpdater;
import io.github.jvmspec.generation.ConstructorPolicy;
import io.github.jvmspec.model.ConstructorDescriptor;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.model.MethodDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JavaLanguageCoverageManifestTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void manifestIsCompleteAndHostSupportedCoveredFixturesPass() throws Exception {
        List<LanguageCoverageEntry> entries = LanguageCoverageManifest.load();
        Map<String, LanguageCoverageEntry> byConstruct = validateManifest(entries);
        int hostRelease = javaSpecificationVersion();
        int manifestPlannedCount = plannedCount(entries);
        List<String> reportLines = new ArrayList<String>();
        reportLines.add("javaspec Java language coverage");
        reportLines.add("host-release=" + hostRelease);
        reportLines.add("strict=" + strictCoverageRequired());

        int passCount = 0;
        int plannedCount = 0;
        int notApplicableCount = 0;
        for (int i = 0; i < entries.size(); i++) {
            LanguageCoverageEntry entry = entries.get(i);
            int requiredRelease = releaseOf(entry.profile());
            if (requiredRelease > hostRelease) {
                notApplicableCount++;
                reportLines.add(reportLine("NOT_APPLICABLE", entry, "requires Java " + requiredRelease));
            } else if (LanguageCoverageEntry.EvidenceStatus.PLANNED.equals(entry.evidenceStatus())) {
                plannedCount++;
                reportLines.add(reportLine("PLANNED", entry, entry.description()));
            } else {
                runCoveredFixture(entry);
                passCount++;
                reportLines.add(reportLine("PASS", entry, entry.fixturePath()));
            }
        }
        reportLines.add("summary pass=" + passCount + " planned=" + plannedCount
                + " not-applicable=" + notApplicableCount + " manifest-planned=" + manifestPlannedCount);
        writeReport(reportLines);

        assertEquals(JavaLanguageConstruct.values().length, byConstruct.size());
        if (strictCoverageRequired()) {
            assertEquals("Strict stable coverage does not allow PLANNED manifest entries", 0, manifestPlannedCount);
        }
    }

    private Map<String, LanguageCoverageEntry> validateManifest(List<LanguageCoverageEntry> entries) {
        assertFalse("Language coverage manifest must not be empty", entries.isEmpty());
        assertEquals("Manifest must contain one row per declared construct",
                JavaLanguageConstruct.values().length, entries.size());

        Map<String, LanguageCoverageEntry> byConstruct = new LinkedHashMap<String, LanguageCoverageEntry>();
        for (int i = 0; i < entries.size(); i++) {
            LanguageCoverageEntry entry = entries.get(i);
            assertFalse("Duplicate construct in manifest: " + entry.constructId(),
                    byConstruct.containsKey(entry.constructId()));
            byConstruct.put(entry.constructId(), entry);
            if (LanguageCoverageEntry.EvidenceStatus.COVERED.equals(entry.evidenceStatus())) {
                assertTrue("Covered entry must name a fixture: " + entry.constructId(), entry.hasFixture());
                assertFalse("Covered entry must name an executable scenario: " + entry.constructId(),
                        "PENDING".equals(entry.scenario()));
                assertNotNull("Missing fixture root: " + entry.fixturePath(),
                        getClass().getResource("/" + entry.fixturePath()));
            }
        }

        JavaLanguageConstruct[] required = JavaLanguageConstruct.values();
        for (int i = 0; i < required.length; i++) {
            JavaLanguageConstruct construct = required[i];
            LanguageCoverageEntry entry = byConstruct.get(construct.id());
            assertNotNull("Manifest is missing construct " + construct.id(), entry);
            assertEquals("Wrong profile for " + construct.id(), construct.profile(), entry.profile());
            assertEquals("Manifest order must be deterministic", construct.id(), entries.get(i).constructId());
        }
        return byConstruct;
    }

    private void runCoveredFixture(LanguageCoverageEntry entry) throws Exception {
        if ("SPEC_LAMBDA_TARGETS".equals(entry.scenario())) {
            runSpecLambdaTargetFixture(entry);
            return;
        }
        if ("UPDATE_METHOD".equals(entry.scenario())) {
            UpdatedFixture fixture = updateFixture(entry);
            compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId());
            return;
        }
        if ("UPDATE_NO_COMPILE".equals(entry.scenario())) {
            updateFixture(entry);
            return;
        }
        if ("MODULE_INFO".equals(entry.scenario())) {
            runModuleInfoFixture(entry);
            return;
        }
        if ("JAVA11_APIS".equals(entry.scenario())) {
            runJava11ApiFixture(entry);
            return;
        }
        if ("SEALED_MULTI_FILE".equals(entry.scenario())) {
            runSealedMultiFileFixture(entry);
            return;
        }
        if ("JAVA17_APIS".equals(entry.scenario())) {
            runJava17ApiFixture(entry);
            return;
        }
        if ("JAVA21_APIS".equals(entry.scenario())) {
            runJava21ApiFixture(entry);
            return;
        }
        if ("JAVA21_VIRTUAL_THREADS".equals(entry.scenario())) {
            runJava21VirtualThreadFixture(entry);
            return;
        }
        if ("FLEXIBLE_CONSTRUCTOR".equals(entry.scenario())) {
            runFlexibleConstructorFixture(entry);
            return;
        }
        if ("COMPACT_SOURCE".equals(entry.scenario())) {
            runCompactSourceFixture(entry);
            return;
        }
        if ("JAVA25_GATHERERS".equals(entry.scenario())) {
            runJava25GathererFixture(entry);
            return;
        }
        if ("ATOMIC_FAILURE".equals(entry.scenario())) {
            assertTrue(resourceText(entry.fixturePath() + "/marker.txt").contains("AtomicFileWriterTest"));
            return;
        }
        if ("SOURCE_DIAGNOSTICS".equals(entry.scenario())) {
            runCompactSourceRefusal(entry, false);
            return;
        }
        throw new AssertionError("Unknown covered language fixture scenario: " + entry.scenario());
    }

    private UpdatedFixture updateFixture(LanguageCoverageEntry entry) throws Exception {
        File sourceRoot = temporaryFolder.newFolder(entry.constructId() + "-source");
        File specRoot = temporaryFolder.newFolder(entry.constructId() + "-spec");
        copyFixtureFile(entry, "spec", "spec/com/example/SubjectSpec.java", specRoot);

        List<DiscoveredSpec> discovered = SpecDiscovery.discover(specRoot);
        assertEquals(1, discovered.size());
        assertFalse("Fixture spec must expose one classic PHPSpec-style behavior",
                discovered.get(0).examples().isEmpty());
        assertTrue(discovered.get(0).examples().get(0).methodName().startsWith("it_"));
        assertEquals(1, discovered.get(0).describedType().methods().size());

        String sourceRelativePath = discovered.get(0).describedType().qualifiedName().replace('.', '/') + ".java";
        File sourceFile = copyFixtureFile(entry, "initial", sourceRelativePath, sourceRoot);
        String expected = resourceText(entry.fixturePath() + "/expected/" + sourceRelativePath);

        String initialHash = sha256(sourceFile);
        String initial = readUtf8(sourceFile);
        String planned = ClassMethodUpdater.updateSource(initial, discovered.get(0).describedType());
        assertEquals("Dry-run planning must not write the source file", initialHash, sha256(sourceFile));
        assertEquals(expected, planned);

        String updated = ClassMethodUpdater.updateFile(sourceFile, discovered.get(0).describedType());
        assertEquals(expected, updated);
        assertEquals(expected, readUtf8(sourceFile));
        if (expected.contains("// javaspec:stub")) {
            assertTrue("Generated production bodies must remain visibly incomplete",
                    updated.contains("// javaspec:stub"));
        }

        String appliedHash = sha256(sourceFile);
        String updatedAgain = ClassMethodUpdater.updateFile(sourceFile, discovered.get(0).describedType());
        assertEquals(updated, updatedAgain);
        assertEquals("Second update must be byte-idempotent", appliedHash, sha256(sourceFile));
        return new UpdatedFixture(sourceRoot, specRoot, sourceFile);
    }

    private void runSpecLambdaTargetFixture(LanguageCoverageEntry entry) throws Exception {
        LambdaFixtureRun typed = runLambdaFixture(entry, "typed");
        assertTrue(typed.result.shouldProceed());
        String generatedSubject = readUtf8(typed.sourceFile);
        assertTrue(generatedSubject.contains("java.util.function.Function<String, String>"));
        assertTrue(generatedSubject.contains("// javaspec:stub"));
        File typedSupport = new File(typed.generatedRoot, "spec/com/example/SubjectSpecSupport.java");
        assertTrue(typedSupport.isFile());
        String supportSource = readUtf8(typedSupport);
        assertTrue("Typed proxy must retain an explicit subject() equivalent", supportSource.contains("subject()"));
        assertTrue(supportSource.contains("transformCast"));
        File typedSpec = new File(typed.specRoot, "spec/com/example/SubjectSpec.java");
        compileSpecAndTypedProxy(typed.sourceFile, typedSupport, typedSpec,
                entry.constructId() + "-typed-proxy");
        String typedHash = sha256(typed.sourceFile);
        LambdaFixtureRun typedAgain = executeLambdaFixture(typed);
        assertTrue(typedAgain.result.shouldProceed());
        assertEquals("Lambda target generation must be idempotent", typedHash, sha256(typed.sourceFile));

        LambdaFixtureRun unique = runLambdaFixture(entry, "unique");
        assertTrue(unique.result.shouldProceed());
        assertEquals("A unique production SAM signature must remain production truth",
                unique.initialSourceHash, sha256(unique.sourceFile));
        assertTrue(new File(unique.generatedRoot, "spec/com/example/SubjectSpecSupport.java").isFile());

        LambdaFixtureRun untyped = runLambdaFixture(entry, "untyped");
        assertFalse(untyped.result.shouldProceed());
        assertEquals(1, untyped.result.exitCode());
        assertTrue(untyped.diagnostic.contains("Cannot infer functional-interface target"));
        assertTrue(untyped.diagnostic.contains("explicitly typed variable"));
        assertEquals(untyped.initialSourceHash, sha256(untyped.sourceFile));
        assertFalse("Untyped lambda refusal must happen before support writes", untyped.generatedRoot.exists());

        LambdaFixtureRun ambiguous = runLambdaFixture(entry, "ambiguous");
        assertFalse(ambiguous.result.shouldProceed());
        assertEquals(1, ambiguous.result.exitCode());
        assertTrue(ambiguous.diagnostic.contains("one unambiguous production signature"));
        assertEquals(ambiguous.initialSourceHash, sha256(ambiguous.sourceFile));
        assertFalse("Ambiguous lambda refusal must happen before support writes", ambiguous.generatedRoot.exists());
    }

    private LambdaFixtureRun runLambdaFixture(LanguageCoverageEntry entry, String caseName) throws Exception {
        File sourceRoot = temporaryFolder.newFolder(entry.constructId() + "-" + caseName + "-source");
        File specRoot = temporaryFolder.newFolder(entry.constructId() + "-" + caseName + "-spec");
        File generatedRoot = new File(temporaryFolder.getRoot(),
                entry.constructId() + "-" + caseName + "-generated");
        File sourceFile = copyResource(entry.fixturePath() + "/" + caseName
                + "/initial/com/example/Subject.java", new File(sourceRoot, "com/example/Subject.java"));
        copyResource(entry.fixturePath() + "/" + caseName
                + "/spec/spec/com/example/SubjectSpec.java",
                new File(specRoot, "spec/com/example/SubjectSpec.java"));
        LambdaFixtureRun run = new LambdaFixtureRun(
                sourceRoot, specRoot, generatedRoot, sourceFile, sha256(sourceFile));
        return executeLambdaFixture(run);
    }

    private LambdaFixtureRun executeLambdaFixture(LambdaFixtureRun run) throws Exception {
        List<DiscoveredSpec> specs = SpecDiscovery.discover(run.specRoot);
        assertEquals(1, specs.size());
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        run.result = GenerationOrchestrator.execute(
                specs,
                run.specRoot,
                run.sourceRoot,
                new BufferedReader(new StringReader("")),
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(errorOutput),
                true,
                false,
                SpecNamingConvention.defaults(),
                getClass().getClassLoader(),
                ConstructorPolicy.PRESERVE,
                run.generatedRoot
        );
        run.diagnostic = new String(errorOutput.toByteArray(), StandardCharsets.UTF_8);
        return run;
    }

    private void runModuleInfoFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File moduleInfo = copyFixtureFile(entry, "initial", "module-info.java", fixture.sourceRoot);
        String moduleHash = sha256(moduleInfo);
        copyFixtureFile(entry, "initial", "module-info.java", fixture.specRoot);
        assertEquals("module-info.java must not be discovered as a specification",
                1, SpecDiscovery.discover(fixture.specRoot).size());
        compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId(), moduleInfo);
        assertEquals("Module descriptor must remain byte-for-byte unchanged", moduleHash, sha256(moduleInfo));
    }

    private void runJava11ApiFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File output = compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId());
        URLClassLoader loader = fixtureClassLoader(output);
        try {
            Class<?> subjectClass = Class.forName("com.example.Subject", true, loader);
            Object subject = subjectClass.getDeclaredConstructor().newInstance();
            Object values = subjectClass.getMethod("values").invoke(subject);
            assertEquals("[value]", values.toString());
            assertNotNull(subjectClass.getMethod("client").invoke(subject));
            File text = temporaryFolder.newFile("java11-read-string.txt");
            Files.write(text.toPath(), "java11".getBytes(StandardCharsets.UTF_8));
            assertEquals("java11", subjectClass.getMethod("read", java.nio.file.Path.class)
                    .invoke(subject, text.toPath()));
        } finally {
            loader.close();
        }
    }

    private void runSealedMultiFileFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File companion = copyFixtureFile(
                entry, "additional", "com/example/Companion.java", fixture.sourceRoot);
        String companionHash = sha256(companion);
        compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId(), companion);
        assertEquals("Sealed hierarchy companion must remain unchanged", companionHash, sha256(companion));
    }

    private void runJava17ApiFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File output = compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId());
        URLClassLoader loader = fixtureClassLoader(output);
        try {
            Class<?> subjectClass = Class.forName("com.example.Subject", true, loader);
            Object subject = subjectClass.getDeclaredConstructor().newInstance();
            assertEquals("[value]", subjectClass.getMethod("values").invoke(subject).toString());
            assertEquals("0a0b", subjectClass.getMethod("hex").invoke(subject));
            assertEquals("1970-01-01T00:00:00Z", subjectClass.getMethod("instant").invoke(subject).toString());
            assertTrue(subjectClass.getMethod("randomValue").invoke(subject) instanceof Integer);
        } finally {
            loader.close();
        }
    }

    private void runJava21ApiFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File output = compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId());
        URLClassLoader loader = fixtureClassLoader(output);
        try {
            Class<?> subjectClass = Class.forName("com.example.Subject", true, loader);
            Object subject = subjectClass.getDeclaredConstructor().newInstance();
            assertEquals("first", subjectClass.getMethod("first").invoke(subject));
            assertEquals("last", subjectClass.getMethod("reversedFirst").invoke(subject));
            assertEquals("[first, last]", subjectClass.getMethod("values").invoke(subject).toString());
        } finally {
            loader.close();
        }
    }

    private void runJava21VirtualThreadFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File output = compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId());
        URLClassLoader loader = fixtureClassLoader(output);
        try {
            Class<?> subjectClass = Class.forName("com.example.Subject", true, loader);
            Object subject = subjectClass.getDeclaredConstructor().newInstance();
            assertEquals("done:true:false", subjectClass.getMethod("runOnVirtualThread").invoke(subject));
        } finally {
            loader.close();
        }
    }

    private void runFlexibleConstructorFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File companion = copyFixtureFile(
                entry, "additional", "com/example/Companion.java", fixture.sourceRoot);
        String companionHash = sha256(companion);
        String updated = readUtf8(fixture.sourceFile);
        DescribedType constructorContract = DescribedType.of("com.example.Subject").withConstructors(
                Collections.singletonList(ConstructorDescriptor.of(
                        Collections.singletonList("String"),
                        Collections.singletonList("value"),
                        ""
                ))
        );
        String constructorChecked = ClassConstructorUpdater.updateSource(
                updated, constructorContract, ConstructorPolicy.PRESERVE);
        assertEquals("Matching flexible constructor must not be rewritten", updated, constructorChecked);
        assertTrue(updated.indexOf("if (value == null)") < updated.indexOf("String normalized"));
        assertTrue(updated.indexOf("String normalized") < updated.indexOf("super(normalized)"));
        compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId(), companion);
        assertEquals("Flexible-constructor base must remain unchanged", companionHash, sha256(companion));
    }

    private void runCompactSourceFixture(LanguageCoverageEntry entry) throws Exception {
        runCompactSourceRefusal(entry, true);
    }

    private void runCompactSourceRefusal(LanguageCoverageEntry entry, boolean compileSource) throws Exception {
        File sourceRoot = temporaryFolder.newFolder(entry.constructId() + "-source");
        File specRoot = temporaryFolder.newFolder(entry.constructId() + "-spec");
        File generatedRoot = new File(temporaryFolder.getRoot(), entry.constructId() + "-generated");
        File compactSource = copyFixtureFile(entry, "initial", "CompactProgram.java", sourceRoot);
        copyFixtureFile(entry, "spec", "spec/CompactProgramSpec.java", specRoot);
        String initial = readUtf8(compactSource);
        DescribedType unsupportedTarget = DescribedType.of("CompactProgram").withMethods(
                Arrays.asList(MethodDescriptor.of("addedBehavior", "String")));
        assertEquals("Compact source must not be mutated as a class-like subject", initial,
                ClassMethodUpdater.updateSource(initial, unsupportedTarget));

        List<DiscoveredSpec> specs = SpecDiscovery.discover(specRoot);
        assertEquals(1, specs.size());
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        GenerationOrchestratorResult result = GenerationOrchestrator.execute(
                specs,
                specRoot,
                sourceRoot,
                new BufferedReader(new StringReader("")),
                new PrintStream(standardOutput),
                new PrintStream(errorOutput),
                true,
                false,
                SpecNamingConvention.defaults(),
                getClass().getClassLoader(),
                ConstructorPolicy.PRESERVE,
                generatedRoot
        );
        String diagnostic = new String(errorOutput.toByteArray(), StandardCharsets.UTF_8);
        assertFalse(result.shouldProceed());
        assertEquals(1, result.exitCode());
        assertTrue(diagnostic.contains("no named class-like declaration"));
        assertTrue(diagnostic.contains("use a named class, record, interface, enum, or annotation"));
        assertFalse("Fail-closed compact-source refusal must not generate support", generatedRoot.exists());
        assertEquals(initial, readUtf8(compactSource));
        if (compileSource) {
            compile(compactSource, 25, entry.constructId());
        }
    }

    private void runJava25GathererFixture(LanguageCoverageEntry entry) throws Exception {
        UpdatedFixture fixture = updateFixture(entry);
        File output = compile(fixture.sourceFile, releaseOf(entry.profile()), entry.constructId());
        URLClassLoader loader = fixtureClassLoader(output);
        try {
            Class<?> subjectClass = Class.forName("com.example.Subject", true, loader);
            Object subject = subjectClass.getDeclaredConstructor().newInstance();
            assertEquals("ab", subjectClass.getMethod("folded").invoke(subject));
        } finally {
            loader.close();
        }
    }

    private void compileSpecAndTypedProxy(
            File sourceFile,
            File supportFile,
            File specFile,
            String fixtureId
    ) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("A JDK compiler is required for typed-proxy coverage", compiler);
        File output = temporaryFolder.newFolder("compiled-" + fixtureId);
        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
        List<String> arguments = new ArrayList<String>();
        if (javaSpecificationVersion() >= 9) {
            arguments.add("--release");
            arguments.add("8");
        } else {
            arguments.add("-source");
            arguments.add("8");
            arguments.add("-target");
            arguments.add("8");
        }
        arguments.add("-classpath");
        arguments.add(new File("target/classes").getAbsolutePath());
        arguments.add("-d");
        arguments.add(output.getAbsolutePath());
        arguments.add(sourceFile.getAbsolutePath());
        arguments.add(supportFile.getAbsolutePath());
        arguments.add(specFile.getAbsolutePath());
        int exit = compiler.run(null, compilerOutput, compilerOutput,
                arguments.toArray(new String[arguments.size()]));
        assertEquals("Typed proxy and spec did not compile:\n"
                        + new String(compilerOutput.toByteArray(), StandardCharsets.UTF_8),
                0, exit);
    }

    private URLClassLoader fixtureClassLoader(File output) throws IOException {
        return new URLClassLoader(new URL[] {output.toURI().toURL()}, getClass().getClassLoader());
    }

    private File compile(File sourceFile, int release, String fixtureId, File... additionalSources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("A JDK compiler is required for language coverage fixtures", compiler);
        File output = temporaryFolder.newFolder("compiled-" + fixtureId);
        ByteArrayOutputStream compilerOutput = new ByteArrayOutputStream();
        List<String> arguments = new ArrayList<String>();
        if (javaSpecificationVersion() >= 9) {
            arguments.add("--release");
            arguments.add(Integer.toString(release));
        } else {
            arguments.add("-source");
            arguments.add(Integer.toString(release));
            arguments.add("-target");
            arguments.add(Integer.toString(release));
        }
        arguments.add("-d");
        arguments.add(output.getAbsolutePath());
        arguments.add(sourceFile.getAbsolutePath());
        for (int i = 0; i < additionalSources.length; i++) {
            arguments.add(additionalSources[i].getAbsolutePath());
        }
        int exit = compiler.run(null, compilerOutput, compilerOutput,
                arguments.toArray(new String[arguments.size()]));
        assertEquals("Fixture did not compile:\n" + new String(compilerOutput.toByteArray(), StandardCharsets.UTF_8),
                0, exit);
        String source = readUtf8(sourceFile);
        String packageName = packageNameOf(source);
        String simpleName = sourceFile.getName().substring(0, sourceFile.getName().length() - ".java".length());
        String classRelativePath = (packageName.length() == 0 ? "" : packageName.replace('.', '/') + "/")
                + simpleName + ".class";
        assertTrue(new File(output, classRelativePath).isFile());
        return output;
    }

    private static final class LambdaFixtureRun {
        private final File sourceRoot;
        private final File specRoot;
        private final File generatedRoot;
        private final File sourceFile;
        private final String initialSourceHash;
        private GenerationOrchestratorResult result;
        private String diagnostic;

        private LambdaFixtureRun(
                File sourceRoot,
                File specRoot,
                File generatedRoot,
                File sourceFile,
                String initialSourceHash
        ) {
            this.sourceRoot = sourceRoot;
            this.specRoot = specRoot;
            this.generatedRoot = generatedRoot;
            this.sourceFile = sourceFile;
            this.initialSourceHash = initialSourceHash;
        }
    }

    private static final class UpdatedFixture {
        private final File sourceRoot;
        private final File specRoot;
        private final File sourceFile;

        private UpdatedFixture(File sourceRoot, File specRoot, File sourceFile) {
            this.sourceRoot = sourceRoot;
            this.specRoot = specRoot;
            this.sourceFile = sourceFile;
        }
    }

    private File copyFixtureFile(
            LanguageCoverageEntry entry,
            String fixtureArea,
            String projectRelativePath,
            File targetRoot
    ) throws IOException {
        return copyResource(
                entry.fixturePath() + "/" + fixtureArea + "/" + projectRelativePath,
                new File(targetRoot, projectRelativePath)
        );
    }

    private File copyResource(String resourcePath, File target) throws IOException {
        byte[] bytes = resourceBytes(resourcePath);
        File parent = target.getParentFile();
        assertTrue(parent.mkdirs() || parent.isDirectory());
        Files.write(target.toPath(), bytes);
        return target;
    }

    private String resourceText(String resourcePath) throws IOException {
        return new String(resourceBytes(resourcePath), StandardCharsets.UTF_8);
    }

    private byte[] resourceBytes(String resourcePath) throws IOException {
        InputStream stream = getClass().getResourceAsStream("/" + resourcePath);
        if (stream == null) {
            throw new IOException("Missing fixture resource: " + resourcePath);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
        } finally {
            stream.close();
        }
        return output.toByteArray();
    }

    private static String readUtf8(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static String packageNameOf(String source) {
        String marker = "package ";
        int start = source.indexOf(marker);
        if (start < 0) {
            return "";
        }
        start += marker.length();
        int end = source.indexOf(';', start);
        if (end < 0) {
            return "";
        }
        return source.substring(start, end).trim();
    }

    private static String sha256(File file) throws IOException, NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            result.append(String.format("%02x", Integer.valueOf(digest[i] & 0xff)));
        }
        return result.toString();
    }

    private static int javaSpecificationVersion() {
        String value = System.getProperty("java.specification.version", "8");
        if (value.startsWith("1.")) {
            value = value.substring(2);
        }
        int dot = value.indexOf('.');
        if (dot >= 0) {
            value = value.substring(0, dot);
        }
        return Integer.parseInt(value);
    }

    private static int releaseOf(String profile) {
        if ("all".equals(profile) || "java8".equals(profile)) {
            return 8;
        }
        if (profile.startsWith("java")) {
            return Integer.parseInt(profile.substring(4));
        }
        throw new IllegalArgumentException("Unknown language coverage profile: " + profile);
    }

    private static int plannedCount(List<LanguageCoverageEntry> entries) {
        int count = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (LanguageCoverageEntry.EvidenceStatus.PLANNED.equals(entries.get(i).evidenceStatus())) {
                count++;
            }
        }
        return count;
    }

    private static boolean strictCoverageRequired() {
        return Boolean.parseBoolean(System.getProperty("javaspec.languageCoverage.strict", "false"));
    }

    private static String reportLine(String result, LanguageCoverageEntry entry, String detail) {
        return result + "\t" + entry.profile() + "\t" + entry.constructId() + "\t"
                + entry.disposition().name() + "\t" + detail;
    }

    private static void writeReport(List<String> lines) throws IOException {
        File target = new File("target/java-language-coverage-report.txt");
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create language coverage report directory: " + parent);
        }
        Files.write(target.toPath(), lines, StandardCharsets.UTF_8);
    }
}
