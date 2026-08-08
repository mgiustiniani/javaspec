package io.github.jvmspec.maven;

import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;
import io.github.jvmspec.discovery.SpecNamingConvention;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Prepares a project-specific closed-world launcher and reachability metadata for GraalVM Native Image.
 *
 * <p>This goal does not invoke {@code native-image}. It generates a test source that statically links
 * the discovered spec/subject classes and reflection metadata under {@code META-INF/native-image}.
 * Run it before {@code testCompile}, then use GraalVM Native Build Tools to create the executable from
 * the project's test classpath.</p>
 */
@Mojo(
        name = "native-prepare",
        defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
        requiresDependencyResolution = ResolutionScope.TEST,
        requiresProject = true,
        threadSafe = true
)
public final class JavaspecNativePrepareMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(property = "javaspec.native.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "javaspec.specDir", defaultValue = "${project.basedir}/src/test/java")
    private File specDir;

    @Parameter(
            property = "javaspec.native.generatedSourcesDir",
            defaultValue = "${project.build.directory}/generated-test-sources/javaspec-native"
    )
    private File generatedNativeSourcesDir;

    @Parameter(
            property = "javaspec.native.configDir",
            defaultValue = "${project.build.testOutputDirectory}/META-INF/native-image/io.github.jvmspec/javaspec-native"
    )
    private File nativeImageConfigDir;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("javaspec: native image preparation skipped.");
            return;
        }

        File effectiveBasedir = basedir == null ? new File(".").getAbsoluteFile() : basedir;
        File effectiveSpecDir = projectFile(effectiveBasedir, specDir);
        File effectiveGeneratedSourcesDir = projectFile(effectiveBasedir, generatedNativeSourcesDir);
        File effectiveConfigDir = projectFile(effectiveBasedir, nativeImageConfigDir);
        List<DiscoveredSpec> specs;
        try {
            specs = SpecDiscovery.discover(SpecDiscoveryRequest.of(
                    effectiveSpecDir,
                    SpecNamingConvention.defaults()
            ));
        } catch (RuntimeException ex) {
            throw new MojoExecutionException("javaspec native source discovery failed: " + messageOf(ex), ex);
        }

        validateLinkableTypes(specs);
        int exampleCount = exampleCount(specs);
        if (exampleCount == 0) {
            throw new MojoFailureException("javaspec native preparation requires at least one discovered "
                    + "it_*/its_* example under " + effectiveSpecDir.getPath() + ".");
        }

        project.addTestCompileSourceRoot(effectiveGeneratedSourcesDir.getAbsolutePath());
        try {
            NativeImageSourceGenerator.PreparationResult result = NativeImageSourceGenerator.write(
                    specs,
                    effectiveBasedir,
                    effectiveGeneratedSourcesDir,
                    effectiveConfigDir
            );
            String action = result.changed() ? "prepared" : "kept unchanged";
            getLog().info("javaspec: " + action + " native image launcher "
                    + NativeImageSourceGenerator.GENERATED_QUALIFIED_NAME + " for " + specs.size()
                    + " specification(s) / " + exampleCount + " example(s).");
            getLog().debug("javaspec: native launcher source: " + result.launcherFile().getPath());
            getLog().debug("javaspec: native reflection metadata: " + result.reflectionFile().getPath());
        } catch (IOException ex) {
            throw new MojoExecutionException("I/O error preparing javaspec native image inputs: "
                    + messageOf(ex) + ".", ex);
        } catch (SecurityException ex) {
            throw new MojoExecutionException("I/O error preparing javaspec native image inputs: "
                    + messageOf(ex) + ".", ex);
        }
    }

    private static void validateLinkableTypes(List<DiscoveredSpec> specs) throws MojoFailureException {
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            if (spec.specQualifiedName().indexOf('.') < 0
                    || spec.describedClass().qualifiedName().indexOf('.') < 0) {
                throw new MojoFailureException("javaspec native preparation requires specifications and "
                        + "described types in named packages; default-package type found for "
                        + spec.specQualifiedName() + ".");
            }
        }
    }

    private static int exampleCount(List<DiscoveredSpec> specs) {
        int count = 0;
        for (int i = 0; i < specs.size(); i++) {
            count += specs.get(i).exampleMetadata().size();
        }
        return count;
    }

    private static File projectFile(File basedir, File file) {
        if (file.isAbsolute()) {
            return file;
        }
        return new File(basedir, file.getPath());
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.length() == 0 ? throwable.getClass().getSimpleName() : message;
    }
}
