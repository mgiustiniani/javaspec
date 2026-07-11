package io.github.jvmspec.maven;

import io.github.jvmspec.compatibility.ProfileEnforcement;
import io.github.jvmspec.compatibility.ProfileEnforcementReport;
import io.github.jvmspec.discovery.DiscoveredSpec;
import io.github.jvmspec.discovery.SpecDiscovery;
import io.github.jvmspec.discovery.SpecDiscoveryRequest;
import io.github.jvmspec.discovery.SpecNamingConvention;
import io.github.jvmspec.discovery.ProductionSignatureReader;
import io.github.jvmspec.generation.SpecGenerationPlan;
import io.github.jvmspec.generation.SpecSkeletonGenerator;
import io.github.jvmspec.generation.SpecSupportFileGenerator;
import io.github.jvmspec.model.DescribedType;
import io.github.jvmspec.profile.TargetProfile;
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
 * Generates or updates production skeletons and the support sources required to compile specifications.
 *
 * <p>This goal is source-first: specification discovery parses source without requiring the generated
 * support superclass to exist. The default {@code generate-test-sources} binding makes a clean Maven
 * build regenerate support before {@code testCompile}.</p>
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
        requiresDependencyResolution = ResolutionScope.TEST,
        requiresProject = true,
        threadSafe = true
)
public final class JavaspecGenerateMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(property = "javaspec.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "javaspec.specDir", defaultValue = "${project.basedir}/src/test/java")
    private File specDir;

    @Parameter(property = "javaspec.sourceDir", defaultValue = "${project.basedir}/src/main/java")
    private File sourceDir;

    @Parameter(
            property = "javaspec.generatedSourcesDir",
            defaultValue = "${project.build.directory}/generated-sources/javaspec"
    )
    private File generatedSourcesDir;

    @Parameter(property = "javaspec.profile", defaultValue = "java8")
    private String profile;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("javaspec: generation skipped.");
            return;
        }

        File effectiveSpecDir = projectFile(specDir);
        File effectiveSourceDir = projectFile(sourceDir);
        File effectiveGeneratedSourcesDir = projectFile(generatedSourcesDir);
        SpecNamingConvention namingConvention = SpecNamingConvention.defaults();
        List<DiscoveredSpec> specs;
        try {
            specs = SpecDiscovery.discover(SpecDiscoveryRequest.of(effectiveSpecDir, namingConvention));
        } catch (RuntimeException ex) {
            throw new MojoExecutionException("javaspec source discovery failed: " + messageOf(ex), ex);
        }

        project.addTestCompileSourceRoot(effectiveGeneratedSourcesDir.getAbsolutePath());
        if (specs.isEmpty()) {
            getLog().info("javaspec: no specifications found for generation in "
                    + effectiveSpecDir.getPath() + ".");
            return;
        }

        TargetProfile targetProfile = targetProfile();
        for (int i = 0; i < specs.size(); i++) {
            DiscoveredSpec spec = specs.get(i);
            DescribedType describedType = ProductionSignatureReader.refine(spec.describedType(), effectiveSourceDir);
            enforceProfile(spec, describedType, targetProfile);
            SpecGenerationPlan plan = SpecSkeletonGenerator.supportPlan(
                    describedType, effectiveSpecDir, effectiveGeneratedSourcesDir, namingConvention);
            try {
                SpecSupportFileGenerator.SupportWriteResult writeResult =
                        SpecSupportFileGenerator.writeOrUpdateResult(plan);
                if (writeResult.changed()) {
                    getLog().info("javaspec: generated specification support " + writeResult.file().getPath() + ".");
                }
            } catch (IOException ex) {
                throw new MojoExecutionException("I/O error generating javaspec support: "
                        + plan.targetFile().getPath() + " (" + messageOf(ex) + ").", ex);
            } catch (SecurityException ex) {
                throw new MojoExecutionException("I/O error generating javaspec support: "
                        + plan.targetFile().getPath() + " (" + messageOf(ex) + ").", ex);
            }
        }
        getLog().info("javaspec: generated support for " + specs.size() + " specification(s) in "
                + effectiveGeneratedSourcesDir.getPath() + ".");
    }

    private void enforceProfile(
            DiscoveredSpec spec,
            DescribedType describedType,
            TargetProfile targetProfile
    ) throws MojoFailureException {
        ProfileEnforcementReport report = ProfileEnforcement.defaultEnforcement()
                .enforce(targetProfile, describedType);
        if (report.isDenied()) {
            throw new MojoFailureException("javaspec profile compatibility error for "
                    + spec.specQualifiedName() + ": " + report.violations().get(0).message());
        }
    }

    private TargetProfile targetProfile() throws MojoExecutionException {
        try {
            return TargetProfile.parse(profile);
        } catch (IllegalArgumentException ex) {
            throw new MojoExecutionException("Invalid javaspec.profile: " + profile + ".", ex);
        }
    }

    private File projectFile(File file) {
        if (file.isAbsolute()) {
            return file;
        }
        File projectDirectory = basedir == null ? new File(".").getAbsoluteFile() : basedir;
        return new File(projectDirectory, file.getPath());
    }

    private static String messageOf(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.length() == 0 ? throwable.getClass().getSimpleName() : message;
    }
}
