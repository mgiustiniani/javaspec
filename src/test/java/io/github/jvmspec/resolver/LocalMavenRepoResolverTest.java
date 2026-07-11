package io.github.jvmspec.resolver;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class LocalMavenRepoResolverTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    // -------------------------------------------------------------------------
    // supports()

    @Test
    public void supportsNullReturnsFalse() {
        assertFalse(new LocalMavenRepoResolver().supports(null));
    }

    @Test
    public void supportsPomXmlByName() throws Exception {
        File pom = tmp.newFile("pom.xml");
        assertTrue(new LocalMavenRepoResolver().supports(pom));
    }

    @Test
    public void supportsDotPomExtension() throws Exception {
        File pom = tmp.newFile("artifact-1.0.pom");
        assertTrue(new LocalMavenRepoResolver().supports(pom));
    }

    @Test
    public void doesNotSupportJavaFile() throws Exception {
        File f = tmp.newFile("Foo.java");
        assertFalse(new LocalMavenRepoResolver().supports(f));
    }

    // -------------------------------------------------------------------------
    // parsePom()

    @Test
    public void parsesGroupIdArtifactIdVersion() throws Exception {
        File pom = writePom(
                "<project>\n"
                + "  <groupId>com.example</groupId>\n"
                + "  <artifactId>mylib</artifactId>\n"
                + "  <version>1.2.3</version>\n"
                + "  <dependencies/>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(tmp.newFolder("repo"));
        LocalMavenRepoResolver.PomData data = resolver.parsePom(pom);
        assertEquals("com.example", data.groupId);
        assertEquals("mylib", data.artifactId);
        assertEquals("1.2.3", data.version);
        assertTrue(data.dependencies.isEmpty());
    }

    @Test
    public void inheritsGroupIdAndVersionFromParent() throws Exception {
        File pom = writePom(
                "<project>\n"
                + "  <parent>\n"
                + "    <groupId>org.parent</groupId>\n"
                + "    <version>2.0</version>\n"
                + "  </parent>\n"
                + "  <artifactId>child</artifactId>\n"
                + "  <dependencies/>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(tmp.newFolder("repo"));
        LocalMavenRepoResolver.PomData data = resolver.parsePom(pom);
        assertEquals("org.parent", data.groupId);
        assertEquals("2.0", data.version);
    }

    @Test
    public void parsesDependency() throws Exception {
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId>\n"
                + "  <artifactId>a</artifactId>\n"
                + "  <version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>dep.group</groupId>\n"
                + "      <artifactId>dep-art</artifactId>\n"
                + "      <version>3.0</version>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(tmp.newFolder("repo"));
        LocalMavenRepoResolver.PomData data = resolver.parsePom(pom);
        assertEquals(1, data.dependencies.size());
        LocalMavenRepoResolver.DependencyEntry dep = data.dependencies.get(0);
        assertEquals("dep.group", dep.groupId);
        assertEquals("dep-art", dep.artifactId);
        assertEquals("3.0", dep.version);
        assertEquals("compile", dep.scope);
        assertFalse(dep.optional);
    }

    @Test
    public void parsesTestScopeDependency() throws Exception {
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>junit</groupId><artifactId>junit</artifactId>\n"
                + "      <version>4.13.2</version><scope>test</scope>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(tmp.newFolder("repo"));
        LocalMavenRepoResolver.PomData data = resolver.parsePom(pom);
        assertEquals(1, data.dependencies.size());
        assertEquals("test", data.dependencies.get(0).scope);
    }

    @Test
    public void ignoresDependencyManagementDependencies() throws Exception {
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencyManagement>\n"
                + "    <dependencies>\n"
                + "      <dependency>\n"
                + "        <groupId>bom.group</groupId><artifactId>bom-art</artifactId>\n"
                + "        <version>9.9</version>\n"
                + "      </dependency>\n"
                + "    </dependencies>\n"
                + "  </dependencyManagement>\n"
                + "  <dependencies/>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(tmp.newFolder("repo"));
        LocalMavenRepoResolver.PomData data = resolver.parsePom(pom);
        assertTrue("dependencyManagement entries must not appear in dependencies list",
                data.dependencies.isEmpty());
    }

    @Test
    public void parsesProperties() throws Exception {
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <properties>\n"
                + "    <dep.version>5.0.1</dep.version>\n"
                + "  </properties>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>x</groupId><artifactId>y</artifactId>\n"
                + "      <version>${dep.version}</version>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(tmp.newFolder("repo"));
        LocalMavenRepoResolver.PomData data = resolver.parsePom(pom);
        assertEquals("${dep.version}", data.dependencies.get(0).version);
        // Interpolation happens at resolve time, not parse time
        String interpolated = LocalMavenRepoResolver.interpolate(
                data.dependencies.get(0).version, data.properties);
        assertEquals("5.0.1", interpolated);
    }

    // -------------------------------------------------------------------------
    // interpolate()

    @Test
    public void interpolateNoPlaceholdersReturnsOriginal() {
        Map<String, String> props = new LinkedHashMap<String, String>();
        assertEquals("hello", LocalMavenRepoResolver.interpolate("hello", props));
    }

    @Test
    public void interpolateNullReturnsEmpty() {
        Map<String, String> props = new LinkedHashMap<String, String>();
        assertEquals("", LocalMavenRepoResolver.interpolate(null, props));
    }

    @Test
    public void interpolateKnownProperty() {
        Map<String, String> props = new LinkedHashMap<String, String>();
        props.put("project.version", "1.2.3");
        assertEquals("1.2.3", LocalMavenRepoResolver.interpolate("${project.version}", props));
    }

    @Test
    public void interpolateUnknownPropertyFallsBackToSystemPropOrPlaceholder() {
        Map<String, String> props = new LinkedHashMap<String, String>();
        String result = LocalMavenRepoResolver.interpolate("${no.such.prop.xyz123}", props);
        // Either the system property value (if defined) or the placeholder is retained.
        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // resolve() — filesystem integration

    @Test
    public void resolveEmptyDependenciesReturnsEmptyList() throws Exception {
        File repo = tmp.newFolder("repo");
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies/>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        List<File> result = resolver.resolve(pom);
        assertTrue(result.isEmpty());
    }

    @Test
    public void resolveTestScopeDependencyIsExcluded() throws Exception {
        File repo = tmp.newFolder("repo");
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>junit</groupId><artifactId>junit</artifactId>\n"
                + "      <version>4.13.2</version><scope>test</scope>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        List<File> result = resolver.resolve(pom);
        assertTrue("test-scope deps must be excluded", result.isEmpty());
    }

    @Test
    public void resolveProvidedScopeDependencyIsExcluded() throws Exception {
        File repo = tmp.newFolder("repo");
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>javax.servlet</groupId><artifactId>servlet-api</artifactId>\n"
                + "      <version>2.5</version><scope>provided</scope>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        List<File> result = resolver.resolve(pom);
        assertTrue("provided-scope deps must be excluded", result.isEmpty());
    }

    @Test
    public void resolveOptionalDependencyIsExcluded() throws Exception {
        File repo = tmp.newFolder("repo");
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>com.optional</groupId><artifactId>opt</artifactId>\n"
                + "      <version>1.0</version><optional>true</optional>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        List<File> result = resolver.resolve(pom);
        assertTrue("optional deps must be excluded", result.isEmpty());
    }

    @Test
    public void resolveCompileScopeDependencyPresentInRepoIsIncluded() throws Exception {
        File repo = tmp.newFolder("repo");
        // Place a fake JAR in the local repo
        File jarDir = new File(repo, "com/example/mylib/1.0");
        assertTrue(jarDir.mkdirs());
        File jar = new File(jarDir, "mylib-1.0.jar");
        assertTrue(jar.createNewFile());
        // Also place a minimal POM so transitive resolution doesn't fail
        File depPom = new File(jarDir, "mylib-1.0.pom");
        Files.write(depPom.toPath(),
                ("<project><groupId>com.example</groupId><artifactId>mylib</artifactId>"
                + "<version>1.0</version><dependencies/></project>")
                .getBytes(StandardCharsets.UTF_8));

        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>com.example</groupId><artifactId>mylib</artifactId>\n"
                + "      <version>1.0</version>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        List<File> result = resolver.resolve(pom);
        assertEquals(1, result.size());
        assertEquals(jar.getCanonicalPath(), result.get(0).getCanonicalPath());
    }

    @Test
    public void resolveMissingJarInRepoIsSkippedGracefully() throws Exception {
        File repo = tmp.newFolder("repo");
        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>missing.group</groupId><artifactId>missing-art</artifactId>\n"
                + "      <version>9.9.9</version>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        // Should not throw — missing artifacts are skipped.
        List<File> result = resolver.resolve(pom);
        assertTrue(result.isEmpty());
    }

    @Test
    public void resolveTransitiveDependency() throws Exception {
        File repo = tmp.newFolder("repo");

        // Transitive dep: com.transitive:trans:1.0
        File transDirR = new File(repo, "com/transitive/trans/1.0");
        assertTrue(transDirR.mkdirs());
        File transJar = new File(transDirR, "trans-1.0.jar");
        assertTrue(transJar.createNewFile());
        File transPomFile = new File(transDirR, "trans-1.0.pom");
        Files.write(transPomFile.toPath(),
                ("<project><groupId>com.transitive</groupId><artifactId>trans</artifactId>"
                + "<version>1.0</version><dependencies/></project>")
                .getBytes(StandardCharsets.UTF_8));

        // Direct dep: com.direct:direct:2.0 — depends on com.transitive:trans:1.0
        File directDir = new File(repo, "com/direct/direct/2.0");
        assertTrue(directDir.mkdirs());
        File directJar = new File(directDir, "direct-2.0.jar");
        assertTrue(directJar.createNewFile());
        File directPomFile = new File(directDir, "direct-2.0.pom");
        Files.write(directPomFile.toPath(),
                ("<project><groupId>com.direct</groupId><artifactId>direct</artifactId>"
                + "<version>2.0</version><dependencies>"
                + "<dependency><groupId>com.transitive</groupId><artifactId>trans</artifactId>"
                + "<version>1.0</version></dependency>"
                + "</dependencies></project>")
                .getBytes(StandardCharsets.UTF_8));

        File pom = writePom(
                "<project>\n"
                + "  <groupId>g</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>com.direct</groupId><artifactId>direct</artifactId>\n"
                + "      <version>2.0</version>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        List<File> result = resolver.resolve(pom);
        assertEquals(2, result.size());
        assertTrue("direct dep must be first", result.get(0).getName().startsWith("direct-"));
        assertTrue("transitive dep must be second", result.get(1).getName().startsWith("trans-"));
    }

    @Test
    public void resolveCycleIsBrokenGracefully() throws Exception {
        File repo = tmp.newFolder("repo");

        // a:1.0 depends on b:1.0; b:1.0 depends on a:1.0 — cycle
        File aDir = new File(repo, "cycle/a/1.0");
        assertTrue(aDir.mkdirs());
        new File(aDir, "a-1.0.jar").createNewFile();
        Files.write(new File(aDir, "a-1.0.pom").toPath(),
                ("<project><groupId>cycle</groupId><artifactId>a</artifactId><version>1.0</version>"
                + "<dependencies>"
                + "<dependency><groupId>cycle</groupId><artifactId>b</artifactId><version>1.0</version></dependency>"
                + "</dependencies></project>")
                .getBytes(StandardCharsets.UTF_8));

        File bDir = new File(repo, "cycle/b/1.0");
        assertTrue(bDir.mkdirs());
        new File(bDir, "b-1.0.jar").createNewFile();
        Files.write(new File(bDir, "b-1.0.pom").toPath(),
                ("<project><groupId>cycle</groupId><artifactId>b</artifactId><version>1.0</version>"
                + "<dependencies>"
                + "<dependency><groupId>cycle</groupId><artifactId>a</artifactId><version>1.0</version></dependency>"
                + "</dependencies></project>")
                .getBytes(StandardCharsets.UTF_8));

        File pom = writePom(
                "<project>\n"
                + "  <groupId>root</groupId><artifactId>root</artifactId><version>1.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>cycle</groupId><artifactId>a</artifactId><version>1.0</version>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>\n"
        );
        LocalMavenRepoResolver resolver = new LocalMavenRepoResolver(repo);
        // Must not throw / loop forever
        List<File> result = resolver.resolve(pom);
        assertEquals(2, result.size());
    }

    @Test
    public void resolverNameIsLocalMavenRepo() {
        assertEquals("local-maven-repo", new LocalMavenRepoResolver().name());
    }

    // -------------------------------------------------------------------------
    // Helpers

    private File writePom(String content) throws IOException {
        File pom = tmp.newFile("pom.xml");
        Files.write(pom.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return pom;
    }
}
