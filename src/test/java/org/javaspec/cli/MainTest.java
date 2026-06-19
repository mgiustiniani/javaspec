package org.javaspec.cli;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;

public class MainTest {

    public interface Mailer {
        void send(String recipient, String body);
    }

    @org.junit.Before
    public void cleanGeneratedSources() throws Exception {
        File dir = new File(TEST_GENERATED_SOURCES);
        if (dir.exists()) {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dirPath, IOException exc) throws IOException {
                    if (!dirPath.equals(dir.toPath())) {
                        Files.deleteIfExists(dirPath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String TEST_GENERATED_SOURCES = "target/generated-sources/javaspec";

    @Test
    public void describeCreatesPhpspecStyleSpecSkeletonOnly() throws Exception {
        File specRoot = temporaryFolder.newFolder("spec-root");
        File specFile = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CalculatorSpec.java");
        File supportFile = new File(TEST_GENERATED_SOURCES, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CalculatorSpecSupport.java");

        CommandResult result = run("describe", "com.example.Calculator", "--spec-dir", specRoot.getAbsolutePath());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Generated specification support: " + supportFile.getPath()));
        assertTrue(result.out.contains("Generated specification: " + specFile.getPath()));
        assertTrue(result.out.contains("Specification class: spec.com.example.CalculatorSpec"));
        assertTrue(result.out.contains("Described class: com.example.Calculator"));
        assertTrue(result.out.contains("No production class was generated"));
        assertEquals("", result.err);
        assertTrue(specFile.isFile());
        assertTrue(supportFile.isFile());
        assertEquals("package spec.com.example;\n\n" +
                "import com.example.Calculator;\n\n" +
                "public class CalculatorSpec extends CalculatorSpecSupport {\n" +
                "    public void it_is_initializable() {\n" +
                "        shouldHaveType(Calculator.class);\n" +
                "    }\n" +
                "}\n", readFile(specFile));
        assertEquals("package spec.com.example;\n\n" +
                "import com.example.Calculator;\n\n" +
                "public class CalculatorSpecSupport extends org.javaspec.api.ObjectBehavior<Calculator> {\n" +
                "    public CalculatorSpecSupport() {\n" +
                "        super(Calculator.class);\n" +
                "    }\n" +
                "}\n", readFile(supportFile));
        assertEquals(1, countFiles(specRoot));
    }

    // -------------------------------------------------------------------------
    // --resolve-pom CLI integration tests

    // -------------------------------------------------------------------------
    // list-extensions command

    @Test
    public void listExtensionsCommandPrintsFormatters() {
        CommandResult result = run("list-extensions");
        assertEquals(0, result.exitCode);
        assertEquals("", result.err);
        assertTrue(result.out.contains("Formatters:"));
        assertTrue(result.out.contains("progress"));
        assertTrue(result.out.contains("pretty"));
    }

    @Test
    public void listExtensionsCommandPrintsExtensionSection() {
        CommandResult result = run("list-extensions");
        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Extensions:"));
    }

    @Test
    public void listExtensionsCommandPrintsClasspathHints() {
        CommandResult result = run("list-extensions");
        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("--classpath"));
        assertTrue(result.out.contains("--resolve-pom"));
    }

    // -------------------------------------------------------------------------
    // --release CLI integration tests

    @Test
    public void releaseWithInvalidVersionExitsWithUsageError() {
        CommandResult result = run("run", "--release", "abc");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error:"));
        assertTrue(result.err.contains("--release") || result.err.contains("release"));
    }

    @Test
    public void releaseWithEmptyValueExitsWithUsageError() {
        CommandResult result = run("run", "--release", "");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error:"));
    }

    @Test
    public void releaseOnDescribeCommandExitsWithUsageError() {
        CommandResult result = run("describe", "--release", "11", "com.example.Foo");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error:"));
        assertTrue(result.err.contains("--release"));
    }

    @Test
    public void releaseOnProphesizeCommandExitsWithUsageError() {
        CommandResult result = run("prophesize", "--release", "11", "java.util.List");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error:"));
        assertTrue(result.err.contains("--release"));
    }

    // -------------------------------------------------------------------------
    // --resolve-pom CLI integration tests

    @Test
    public void resolvePomWithMissingFileExitsWithUsageError() {
        CommandResult result = run("run", "--resolve-pom", "no-such-file.xml");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error: POM file not found:"));
    }

    @Test
    public void resolvePomWithEmptyValueExitsWithUsageError() {
        CommandResult result = run("run", "--resolve-pom", "");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error:"));
    }

    @Test
    public void resolvePomOnDescribeCommandExitsWithUsageError() {
        CommandResult result = run("describe", "--resolve-pom", "pom.xml", "com.example.Foo");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error:"));
        assertTrue(result.err.contains("--resolve-pom"));
    }

    @Test
    public void resolvePomOnProphesizeCommandExitsWithUsageError() {
        CommandResult result = run("prophesize", "--resolve-pom", "pom.xml",
                "java.util.List");
        assertEquals(64, result.exitCode);
        assertTrue(result.err.contains("Error:"));
        assertTrue(result.err.contains("--resolve-pom"));
    }

    @Test
    public void resolvePomWithEmptyDepsRunsNormally() throws Exception {
        // Write a minimal POM with no dependencies.
        File pom = temporaryFolder.newFile("pom.xml");
        Files.write(pom.toPath(),
                ("<project><groupId>g</groupId><artifactId>a</artifactId>"
                + "<version>1.0</version><dependencies/></project>")
                .getBytes(StandardCharsets.UTF_8));
        File specRoot = temporaryFolder.newFolder("specs");
        // No specs — just verify the command completes without error.
        CommandResult result = run("run", "--spec-root", specRoot.getPath(),
                "--resolve-pom", pom.getPath());
        assertEquals(0, result.exitCode);
        assertEquals("", result.err);
    }

    @Test
    public void prophesizeUsesGeneratedSourcesAsDefaultOutputRoot() throws Exception {
        File wrapperFile = new File(TEST_GENERATED_SOURCES,
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "MailerProphecy.java");
        File sourceTreeWrapper = new File("src/test/java",
                "spec" + File.separator + "com" + File.separator + "example" + File.separator + "MailerProphecy.java");

        CommandResult result = run("prophesize", Mailer.class.getName(), "--package", "spec.com.example");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Generated prophecy wrapper: " + wrapperFile.getPath()));
        assertEquals("", result.err);
        assertTrue(wrapperFile.isFile());
        assertFalse(sourceTreeWrapper.exists());
    }

    @Test
    public void describeExistingSpecDoesNotOverwrite() throws Exception {
        File specRoot = temporaryFolder.newFolder("existing-spec-root");
        File specFile = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ExistingSpec.java");
        File supportFile = new File(TEST_GENERATED_SOURCES, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "ExistingSpecSupport.java");
        assertTrue(specFile.getParentFile().mkdirs());
        Files.write(specFile.toPath(), "existing spec\n".getBytes(StandardCharsets.UTF_8));

        CommandResult result = run("describe", "com.example.Existing", "--spec-dir", specRoot.getAbsolutePath());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Specification spec.com.example.ExistingSpec exists"));
        assertTrue(result.out.contains("Generated specification support: " + supportFile.getPath()));
        assertTrue(result.out.contains("No production class was generated"));
        assertEquals("", result.err);
        assertEquals("existing spec\n", readFile(specFile));
        assertTrue(supportFile.isFile());
        assertEquals(1, countFiles(specRoot));
    }

    @Test
    public void describeRejectsGenerateBecauseRunOwnsProductionGeneration() throws Exception {
        File specRoot = temporaryFolder.newFolder("reject-generate-spec-root");

        CommandResult result = run("describe", "com.example.Calculator", "--spec-dir", specRoot.getAbsolutePath(), "--generate");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("describe creates only a specification skeleton"));
        assertTrue(result.err.contains("javaspec run"));
        assertEquals(0, countFiles(specRoot));
    }

    @Test
    public void runMissingClassAsksAndDoesNotWriteWhenUserDeclines() throws Exception {
        File specRoot = temporaryFolder.newFolder("missing-spec-root");
        File sourceRoot = temporaryFolder.newFolder("missing-source-root");
        File specFile = writeSpec(specRoot, "spec.com.example.MissingSpec");
        File targetFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Missing.java");

        CommandResult result = runWithInput("n\n", "run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(1, result.exitCode);
        assertTrue(result.out.contains("Found 1 specification(s)"));
        assertTrue(result.out.contains("spec.com.example.MissingSpec describes missing class com.example.Missing."));
        assertTrue(result.out.contains("Spec file: " + specFile.getPath()));
        assertTrue(result.out.contains("Target path: " + targetFile.getPath()));
        assertTrue(result.out.contains("Do you want me to create com.example.Missing for you? [Y/n]"));
        assertTrue(result.out.contains("No production files were written."));
        assertEquals("", result.err);
        assertFalse(targetFile.exists());
        assertEquals(0, countFiles(sourceRoot));
    }

    @Test
    public void runMissingClassGeneratesWhenUserAcceptsPrompt() throws Exception {
        File specRoot = temporaryFolder.newFolder("accept-spec-root");
        File sourceRoot = temporaryFolder.newFolder("accept-source-root");
        writeSpec(specRoot, "spec.com.example.AcceptedSpec");
        File targetFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Accepted.java");

        CommandResult result = runWithInput("y\n", "run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.AcceptedSpec describes missing class com.example.Accepted."));
        assertTrue(result.out.contains("Do you want me to create com.example.Accepted for you? [Y/n]"));
        assertTrue(result.out.contains("Generated class skeleton: " + targetFile.getPath()));
        assertEquals("", result.err);
        assertTrue(targetFile.isFile());
        assertEquals("package com.example;\n\npublic class Accepted { }\n", readFile(targetFile));
        assertEquals(1, countFiles(sourceRoot));
    }

    @Test
    public void runMissingClassGeneratesWhenUserAcceptsDefaultPrompt() throws Exception {
        File specRoot = temporaryFolder.newFolder("default-accept-spec-root");
        File sourceRoot = temporaryFolder.newFolder("default-accept-source-root");
        writeSpec(specRoot, "spec.com.example.DefaultAcceptedSpec");
        File targetFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "DefaultAccepted.java");

        CommandResult result = runWithInput("\n", "run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Generated class skeleton: " + targetFile.getPath()));
        assertEquals("", result.err);
        assertTrue(targetFile.isFile());
    }

    @Test
    public void runWithGenerateWritesClassSkeletonInferredFromSpecWithoutPrompting() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-source-root");
        writeSpec(specRoot, "spec.com.example.GeneratedSpec");
        File targetFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Generated.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.GeneratedSpec describes missing class com.example.Generated."));
        assertFalse(result.out.contains("Do you want me to create"));
        assertTrue(result.out.contains("Generated class skeleton: " + targetFile.getPath()));
        assertEquals("", result.err);
        assertTrue(targetFile.isFile());
        assertEquals("package com.example;\n\npublic class Generated { }\n", readFile(targetFile));
        assertEquals(1, countFiles(sourceRoot));
    }

    @Test
    public void runWithGenerateWritesInterfaceSkeletonInferredFromSpecMarker() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-interface-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-interface-source-root");
        writeSpec(specRoot, "spec.com.example.PaymentGatewaySpec", "shouldBeAnInterface();\n");
        File targetFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "PaymentGateway.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.PaymentGatewaySpec describes missing interface com.example.PaymentGateway."));
        assertTrue(result.out.contains("Generated interface skeleton: " + targetFile.getPath()));
        assertEquals("", result.err);
        assertTrue(targetFile.isFile());
        assertEquals("package com.example;\n\npublic interface PaymentGateway { }\n", readFile(targetFile));
    }

    @Test
    public void runWithGenerateWritesEnumAndAnnotationSkeletonsInferredFromSpecMarkers() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-kind-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-kind-source-root");
        writeSpec(specRoot, "spec.com.example.OrderStatusSpec", "shouldBeAnEnum();\n");
        writeSpec(specRoot, "spec.com.example.ExperimentalSpec", "shouldBeAnAnnotation();\n");
        File enumFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "OrderStatus.java");
        File annotationFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Experimental.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.OrderStatusSpec describes missing enum com.example.OrderStatus."));
        assertTrue(result.out.contains("Generated enum skeleton: " + enumFile.getPath()));
        assertTrue(result.out.contains("spec.com.example.ExperimentalSpec describes missing annotation com.example.Experimental."));
        assertTrue(result.out.contains("Generated annotation skeleton: " + annotationFile.getPath()));
        assertEquals("", result.err);
        assertEquals("package com.example;\n\npublic enum OrderStatus { }\n", readFile(enumFile));
        assertEquals("package com.example;\n\npublic @interface Experimental { }\n", readFile(annotationFile));
    }

    @Test
    public void runWithGenerateWritesPostJava8TypeSkeletonsInferredFromSpecMarkers() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-post-java8-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-post-java8-source-root");
        writeSpec(specRoot, "spec.com.example.UserSpec", "shouldBeARecord();\n");
        writeSpec(specRoot, "spec.com.example.ShapeSpec", "shouldBeASealedClass();\n");
        writeSpec(specRoot, "spec.com.example.MessageSpec", "shouldBeASealedInterface();\n");
        File recordFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "User.java");
        File sealedClassFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Shape.java");
        File sealedInterfaceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Message.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--profile", "java17", "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.UserSpec describes missing record com.example.User."));
        assertTrue(result.out.contains("Generated record skeleton: " + recordFile.getPath()));
        assertTrue(result.out.contains("spec.com.example.ShapeSpec describes missing sealed class com.example.Shape."));
        assertTrue(result.out.contains("Generated sealed class skeleton: " + sealedClassFile.getPath()));
        assertTrue(result.out.contains("spec.com.example.MessageSpec describes missing sealed interface com.example.Message."));
        assertTrue(result.out.contains("Generated sealed interface skeleton: " + sealedInterfaceFile.getPath()));
        assertEquals("package com.example;\n\npublic record User() { }\n", readFile(recordFile));
        assertEquals("package com.example;\n\n" +
                "public sealed class Shape permits Shape.Permitted {\n" +
                "    static final class Permitted extends Shape { }\n" +
                "}\n", readFile(sealedClassFile));
        assertEquals("package com.example;\n\n" +
                "public sealed interface Message permits Message.Permitted {\n" +
                "    final class Permitted implements Message { }\n" +
                "}\n", readFile(sealedInterfaceFile));
    }

    @Test
    public void runWithGenerateWritesSealedClassWithExplicitPermitsInferredFromSpec() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-sealed-permits-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-sealed-permits-source-root");
        writeSpec(
                specRoot,
                "spec.com.example.ShapeSpec",
                "import com.example.Circle;\n" +
                        "import com.example.Rectangle;\n" +
                        "shouldBeASealedClass();\n" +
                        "shouldPermit(Circle.class, Rectangle.class);\n"
        );
        File sealedClassFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Shape.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--profile", "java17", "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.ShapeSpec describes missing sealed class com.example.Shape."));
        assertTrue(result.out.contains("Generated sealed class skeleton: " + sealedClassFile.getPath()));
        assertEquals("package com.example;\n\npublic sealed class Shape permits Circle, Rectangle { }\n", readFile(sealedClassFile));
    }

    @Test
    public void runWithGenerateCreatesSpecsForMissingExtendsAndImplementsTypesBeforeClasses() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-relationships-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-relationships-source-root");
        writeSpec(
                specRoot,
                "spec.com.example.ServiceSpec",
                "import com.example.BaseService;\n" +
                        "import com.example.PaymentGateway;\n" +
                        "shouldExtend(BaseService.class);\n" +
                        "shouldImplement(PaymentGateway.class);\n"
        );
        File serviceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Service.java");
        File baseServiceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "BaseService.java");
        File paymentGatewayFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "PaymentGateway.java");
        File baseServiceSpec = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BaseServiceSpec.java");
        File paymentGatewaySpec = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "PaymentGatewaySpec.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Related class com.example.BaseService is missing."));
        assertTrue(result.out.contains("Generated related specification: " + baseServiceSpec.getPath()));
        assertTrue(result.out.contains("Related interface com.example.PaymentGateway is missing."));
        assertTrue(result.out.contains("Generated related specification: " + paymentGatewaySpec.getPath()));
        assertEquals("package com.example;\n\npublic class Service extends BaseService implements PaymentGateway { }\n", readFile(serviceFile));
        assertEquals("package com.example;\n\npublic class BaseService { }\n", readFile(baseServiceFile));
        assertEquals("package com.example;\n\npublic interface PaymentGateway { }\n", readFile(paymentGatewayFile));
        assertTrue(readFile(paymentGatewaySpec).contains("shouldBeAnInterface();"));
    }

    @Test
    public void runWithGenerateCreatesSpecsForPermittedTypesThatExtendSealedRoot() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-permitted-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-permitted-source-root");
        writeSpec(
                specRoot,
                "spec.com.example.ShapeSpec",
                "import com.example.Circle;\n" +
                        "shouldBeASealedClass();\n" +
                        "shouldPermit(Circle.class);\n"
        );
        File circleSpec = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "CircleSpec.java");
        File circleFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Circle.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--profile", "java17", "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Generated related specification: " + circleSpec.getPath()));
        assertTrue(readFile(circleSpec).contains("shouldBeAFinalClass();"));
        assertTrue(readFile(circleSpec).contains("shouldExtend(Shape.class);"));
        assertEquals("package com.example;\n\npublic final class Circle extends Shape { }\n", readFile(circleFile));
    }

    @Test
    public void runWithGenerateKeepsSealedInterfacePermittedTypesInSameFile() throws Exception {
        File specRoot = temporaryFolder.newFolder("generate-sealed-interface-local-spec-root");
        File sourceRoot = temporaryFolder.newFolder("generate-sealed-interface-local-source-root");
        writeSpec(
                specRoot,
                "spec.com.example.MessageSpec",
                "import com.example.EmailMessage;\n" +
                        "import com.example.SmsMessage;\n" +
                        "shouldBeASealedInterface();\n" +
                        "shouldPermit(EmailMessage.class, SmsMessage.class);\n"
        );
        File messageFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Message.java");
        File emailMessageFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "EmailMessage.java");
        File emailMessageSpec = new File(specRoot, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "EmailMessageSpec.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--profile", "java17", "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.MessageSpec describes missing sealed interface com.example.Message."));
        assertFalse(result.out.contains("Related final class com.example.EmailMessage is missing."));
        assertFalse(emailMessageFile.exists());
        assertFalse(emailMessageSpec.exists());
        assertEquals("package com.example;\n\n" +
                "public sealed interface Message permits Message.EmailMessage, Message.SmsMessage {\n" +
                "    final class EmailMessage implements Message { }\n" +
                "    final class SmsMessage implements Message { }\n" +
                "}\n", readFile(messageFile));
    }

    @Test
    public void runExistingSourceClassReportsItAndGeneratesNothing() throws Exception {
        File specRoot = temporaryFolder.newFolder("existing-run-spec-root");
        File sourceRoot = temporaryFolder.newFolder("existing-run-source-root");
        writeSpec(specRoot, "spec.com.example.ExistingSpec");
        File sourceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Existing.java");
        assertTrue(sourceFile.getParentFile().mkdirs());
        Files.write(sourceFile.toPath(), "package com.example; public class Existing { }\n".getBytes(StandardCharsets.UTF_8));

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("spec.com.example.ExistingSpec describes com.example.Existing; class exists."));
        assertTrue(result.out.contains("Source file: " + sourceFile.getPath()));
        assertEquals("", result.err);
        assertEquals(1, countFiles(sourceRoot));
    }

    @Test
    public void runWithGenerateWritesMethodSkeletonsAndSpecificationSupportForTypedProxySpec() throws Exception {
        File specRoot = temporaryFolder.newFolder("typed-proxy-spec-root");
        File sourceRoot = temporaryFolder.newFolder("typed-proxy-source-root");
        writeSpec(specRoot, "spec.com.example.BookSpec",
                "    public void it_has_rating() {\n" +
                "        getRating().shouldReturn(5);\n" +
                "    }\n" +
                "\n" +
                "    public void it_has_title() {\n" +
                "        getTitle().shouldContain(\"Wizard\");\n" +
                "    }\n" +
                "\n" +
                "    public void it_is_enabled() {\n" +
                "        isEnabled().shouldReturn(true);\n" +
                "    }\n" +
                "\n" +
                "    public void it_rejects_negative_rating() {\n" +
                "        shouldThrow(IllegalArgumentException.class).duringSetRating(-3);\n" +
                "    }\n");
        File sourceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Book.java");
        File supportFile = new File(TEST_GENERATED_SOURCES, "spec" + File.separator + "com" + File.separator + "example" + File.separator + "BookSpecSupport.java");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath(), "--generate");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Updated specification support: " + supportFile.getPath()));
        assertTrue(result.out.contains("Generated class skeleton: " + sourceFile.getPath()));
        assertEquals("", result.err);
        assertTrue(sourceFile.isFile());
        assertTrue(supportFile.isFile());
        assertEquals("package com.example;\n\n" +
                "public class Book {\n" +
                "    public int getRating() {\n" +
                "        return 0;\n" +
                "    }\n" +
                "\n" +
                "    public String getTitle() {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    public boolean isEnabled() {\n" +
                "        return false;\n" +
                "    }\n" +
                "\n" +
                "    public void setRating(int rating) {\n" +
                "    }\n" +
                "}\n", readFile(sourceFile));
        String supportSource = readFile(supportFile);
        assertTrue(supportSource.contains("protected org.javaspec.matcher.Matchable<Integer> getRating()"));
        assertTrue(supportSource.contains("return match(subject().getRating());"));
        assertTrue(supportSource.contains("protected org.javaspec.matcher.Matchable<String> getTitle()"));
        assertTrue(supportSource.contains("protected org.javaspec.matcher.Matchable<Boolean> isEnabled()"));
        assertTrue(supportSource.contains("protected void setRating(int rating)"));
        assertTrue(supportSource.contains("public void duringSetRating(final int rating)"));
    }

    @Test
    public void runWithoutGeneratePromptsBeforeUpdatingMissingMethodsInExistingSource() throws Exception {
        File specRoot = temporaryFolder.newFolder("prompt-method-spec-root");
        File sourceRoot = temporaryFolder.newFolder("prompt-method-source-root");
        writeSpec(specRoot, "spec.com.example.BookSpec",
                "    public void it_has_rating() {\n" +
                "        getRating().shouldReturn(5);\n" +
                "    }\n" +
                "\n" +
                "    public void it_has_title() {\n" +
                "        getTitle().shouldReturn(\"Wizard\");\n" +
                "    }\n");
        File sourceFile = new File(sourceRoot, "com" + File.separator + "example" + File.separator + "Book.java");
        assertTrue(sourceFile.getParentFile().mkdirs());
        Files.write(sourceFile.toPath(), ("package com.example;\n\n" +
                "public class Book {\n" +
                "    public int getRating() {\n" +
                "        return 5;\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        CommandResult result = runWithInput("y\n", "run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("Do you want me to add missing method skeletons to com.example.Book in " + sourceFile.getPath() + "? [Y/n]"));
        assertTrue(result.out.contains("Updated methods in " + sourceFile.getPath()));
        assertEquals("", result.err);
        String updatedSource = readFile(sourceFile);
        assertEquals(1, countOccurrences(updatedSource, "public int getRating()"));
        assertTrue(updatedSource.contains("public String getTitle()"));
        assertTrue(updatedSource.contains("return null;"));
    }

    @Test
    public void runWithoutSpecsExitsOk() throws Exception {
        File specRoot = temporaryFolder.newFolder("empty-spec-root");
        File sourceRoot = temporaryFolder.newFolder("empty-source-root");

        CommandResult result = run("run", "--spec-dir", specRoot.getAbsolutePath(), "--source-dir", sourceRoot.getAbsolutePath());

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("No specifications found"));
        assertEquals("", result.err);
        assertEquals(0, countFiles(sourceRoot));
    }

    @Test
    public void invalidArgumentsExitUsageAndPrintError() {
        assertUsageError(run());
        assertUsageError(run("describe"));
        assertUsageError(run("run", "unexpected"));
        assertUsageError(run("describe", "com.example.Valid", "--unknown"));
    }

    @Test
    public void helpListsConstructorPolicyValuesAndCommentDefault() {
        CommandResult result = run("--help");

        assertEquals(0, result.exitCode);
        assertTrue(result.out.contains("[--constructor-policy <delete|preserve|comment>]"));
        assertTrue(result.out.contains("Valid values: delete, preserve, comment (default: comment)."));
        assertEquals("", result.err);
    }

    @Test
    public void invalidConstructorPolicyDiagnosticListsExactValues() {
        File specRoot = new File("unused-spec-root");
        File sourceRoot = new File("unused-source-root");

        CommandResult result = run("run", "--spec-dir", specRoot.getPath(), "--source-dir", sourceRoot.getPath(), "--constructor-policy", "keep");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.startsWith("Error: Invalid constructor policy: keep. Valid values: delete, preserve, comment.\n"));
        assertTrue(result.err.contains("[--constructor-policy <delete|preserve|comment>]"));
        assertTrue(result.err.contains("Valid values: delete, preserve, comment (default: comment)."));
    }

    @Test
    public void invalidClassNameExitsUsageAndPrintsError() {
        CommandResult result = run("describe", "class");

        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Error: Invalid class name"));
        assertTrue(result.err.contains("Usage:"));
    }

    private static void assertUsageError(CommandResult result) {
        assertEquals(64, result.exitCode);
        assertEquals("", result.out);
        assertTrue(result.err.contains("Error:"));
        assertTrue(result.err.contains("Usage:"));
    }

    private static File writeSpec(File specRoot, String specQualifiedName) throws Exception {
        return writeSpec(specRoot, specQualifiedName, "");
    }

    private static File writeSpec(File specRoot, String specQualifiedName, String body) throws Exception {
        int lastDot = specQualifiedName.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : specQualifiedName.substring(0, lastDot);
        String simpleName = lastDot < 0 ? specQualifiedName : specQualifiedName.substring(lastDot + 1);
        String relativePath;
        if (packageName.length() == 0) {
            relativePath = simpleName + ".java";
        } else {
            relativePath = packageName.replace('.', File.separatorChar) + File.separator + simpleName + ".java";
        }
        File specFile = new File(specRoot, relativePath);
        File parent = specFile.getParentFile();
        assertTrue(parent == null || parent.isDirectory() || parent.mkdirs());
        String content;
        if (packageName.length() == 0) {
            content = "public class " + simpleName + " {\n" + body + "}\n";
        } else {
            content = "package " + packageName + "; public class " + simpleName + " {\n" + body + "}\n";
        }
        Files.write(specFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return specFile;
    }

    private static CommandResult run(String... args) {
        return runWithInput("", args);
    }

    private static CommandResult runWithInput(String input, String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = Main.run(
                args,
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out),
                new PrintStream(err)
        );
        return new CommandResult(
                exitCode,
                new String(out.toByteArray(), StandardCharsets.UTF_8),
                new String(err.toByteArray(), StandardCharsets.UTF_8)
        );
    }

    private static String readFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static int countFiles(File root) {
        File[] files = root.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                count += countFiles(file);
            } else {
                count++;
            }
        }
        return count;
    }

    private static int countOccurrences(String text, String fragment) {
        int count = 0;
        int index = 0;
        while (true) {
            int found = text.indexOf(fragment, index);
            if (found < 0) {
                return count;
            }
            count++;
            index = found + fragment.length();
        }
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String out;
        private final String err;

        private CommandResult(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }
    }
}
