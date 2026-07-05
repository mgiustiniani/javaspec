package io.github.jvmspec.resolver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Built-in dependency resolver that reads a Maven {@code pom.xml} and locates
 * JAR artifacts in the local Maven repository ({@code ~/.m2/repository}).
 *
 * <p>This resolver uses only JDK APIs (DOM XML parser, {@link File}) and adds
 * zero runtime dependencies to the core artifact.  It performs <em>offline</em>
 * resolution only: artifacts that are absent from the local repository produce
 * a diagnostic warning rather than a network download.</p>
 *
 * <h2>Scope handling</h2>
 * <ul>
 *   <li>From the root POM: {@code compile} and {@code runtime} scopes are
 *       included; {@code test}, {@code provided}, and {@code system} are
 *       excluded.</li>
 *   <li>In transitive POMs: same exclusions apply.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>No network resolution — local repo only.</li>
 *   <li>No BOM / {@code dependencyManagement} import scope support.</li>
 *   <li>No Maven profile activation.</li>
 *   <li>Parent POM is used only for {@code groupId}/{@code version}
 *       inheritance and {@code <properties>}; full parent inheritance is not
 *       implemented.</li>
 * </ul>
 */
public final class LocalMavenRepoResolver implements DependencyResolver {

    private static final int MAX_DEPTH = 10;
    private static final String SCOPE_TEST = "test";
    private static final String SCOPE_PROVIDED = "provided";
    private static final String SCOPE_SYSTEM = "system";
    private static final String SCOPE_IMPORT = "import";

    private final File localRepository;

    /** Constructs a resolver using the default local Maven repository. */
    public LocalMavenRepoResolver() {
        this(defaultLocalRepository());
    }

    /** Constructs a resolver using the given local repository root. */
    public LocalMavenRepoResolver(File localRepository) {
        if (localRepository == null) {
            throw new IllegalArgumentException("localRepository must not be null");
        }
        this.localRepository = localRepository;
    }

    @Override
    public String name() {
        return "local-maven-repo";
    }

    @Override
    public boolean supports(File descriptor) {
        if (descriptor == null) {
            return false;
        }
        String name = descriptor.getName();
        return name.equals("pom.xml") || name.endsWith(".pom");
    }

    @Override
    public List<File> resolve(File descriptor) throws DependencyResolutionException {
        Set<String> visited = new LinkedHashSet<String>();
        List<File> result = new ArrayList<File>();
        resolveTransitive(descriptor, visited, result, 0, false);
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal

    private void resolveTransitive(
            File pomFile,
            Set<String> visited,
            List<File> result,
            int depth,
            boolean transitiveMode
    ) throws DependencyResolutionException {
        if (depth > MAX_DEPTH) {
            return;
        }
        PomData pom;
        try {
            pom = parsePom(pomFile);
        } catch (DependencyResolutionException ex) {
            if (depth == 0) {
                throw ex;
            }
            // Tolerate parsing failures in transitive POMs.
            return;
        }

        String key = pom.groupId + ":" + pom.artifactId + ":" + pom.version;
        if (visited.contains(key)) {
            return;
        }
        visited.add(key);

        for (DependencyEntry dep : pom.dependencies) {
            String scope = dep.scope;
            if (SCOPE_SYSTEM.equals(scope) || SCOPE_IMPORT.equals(scope)) {
                continue;
            }
            if (transitiveMode && (SCOPE_TEST.equals(scope) || SCOPE_PROVIDED.equals(scope))) {
                continue;
            }
            if (!transitiveMode && (SCOPE_TEST.equals(scope) || SCOPE_PROVIDED.equals(scope))) {
                continue;
            }
            if (dep.optional) {
                continue;
            }
            String depGroupId = interpolate(dep.groupId, pom.properties);
            String depArtifactId = interpolate(dep.artifactId, pom.properties);
            String depVersion = interpolate(dep.version, pom.properties);
            if (depGroupId.isEmpty() || depArtifactId.isEmpty() || depVersion.isEmpty()) {
                continue;
            }
            String depKey = depGroupId + ":" + depArtifactId + ":" + depVersion;
            if (visited.contains(depKey)) {
                continue;
            }
            File jar = locateJar(depGroupId, depArtifactId, depVersion);
            if (jar != null && jar.isFile()) {
                result.add(jar);
            }
            File depPom = locatePom(depGroupId, depArtifactId, depVersion);
            if (depPom != null && depPom.isFile()) {
                resolveTransitive(depPom, visited, result, depth + 1, true);
            } else {
                // Mark visited even if POM is missing to avoid re-trying.
                visited.add(depKey);
            }
        }
    }

    // -------------------------------------------------------------------------
    // POM parsing

    PomData parsePom(File pomFile) throws DependencyResolutionException {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(pomFile);
        } catch (ParserConfigurationException ex) {
            throw new DependencyResolutionException(
                    "Cannot configure XML parser: " + ex.getMessage(), ex);
        } catch (SAXException ex) {
            throw new DependencyResolutionException(
                    "Cannot parse POM file: " + pomFile.getPath() + " (" + ex.getMessage() + ")", ex);
        } catch (IOException ex) {
            throw new DependencyResolutionException(
                    "Cannot read POM file: " + pomFile.getPath() + " (" + ex.getMessage() + ")", ex);
        }

        Element root = document.getDocumentElement();
        Map<String, String> properties = new LinkedHashMap<String, String>();

        // Parent coordinates (for inheritance of groupId/version)
        String parentGroupId = firstChildText(root, "parent", "groupId");
        String parentVersion = firstChildText(root, "parent", "version");

        String groupId = directChildText(root, "groupId");
        if (groupId == null || groupId.isEmpty()) {
            groupId = parentGroupId != null ? parentGroupId : "";
        }
        String artifactId = directChildText(root, "artifactId");
        if (artifactId == null) {
            artifactId = "";
        }
        String version = directChildText(root, "version");
        if (version == null || version.isEmpty()) {
            version = parentVersion != null ? parentVersion : "";
        }

        // Built-in properties
        properties.put("project.groupId", groupId);
        properties.put("project.artifactId", artifactId);
        properties.put("project.version", version);
        properties.put("pom.groupId", groupId);
        properties.put("pom.artifactId", artifactId);
        properties.put("pom.version", version);

        // <properties> block
        Element propertiesEl = directChildElement(root, "properties");
        if (propertiesEl != null) {
            NodeList childNodes = propertiesEl.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node instanceof Element) {
                    properties.put(node.getNodeName(), node.getTextContent().trim());
                }
            }
        }

        // <dependencies>
        List<DependencyEntry> dependencies = new ArrayList<DependencyEntry>();
        Element dependenciesEl = directChildElement(root, "dependencies");
        if (dependenciesEl != null) {
            NodeList depNodes = dependenciesEl.getElementsByTagName("dependency");
            for (int i = 0; i < depNodes.getLength(); i++) {
                Node node = depNodes.item(i);
                if (!(node instanceof Element)) {
                    continue;
                }
                Element depEl = (Element) node;
                // Skip dependencies inside <dependencyManagement>
                if (isInsideDependencyManagement(depEl)) {
                    continue;
                }
                String depGroupId = textOf(depEl, "groupId");
                String depArtifactId = textOf(depEl, "artifactId");
                String depVersion = textOf(depEl, "version");
                String depScope = textOf(depEl, "scope");
                String depOptional = textOf(depEl, "optional");
                if (depScope == null || depScope.isEmpty()) {
                    depScope = "compile";
                }
                boolean optional = "true".equalsIgnoreCase(depOptional);
                dependencies.add(new DependencyEntry(
                        depGroupId != null ? depGroupId : "",
                        depArtifactId != null ? depArtifactId : "",
                        depVersion != null ? depVersion : "",
                        depScope,
                        optional
                ));
            }
        }

        return new PomData(groupId, artifactId, version, properties, dependencies);
    }

    private static boolean isInsideDependencyManagement(Element depEl) {
        Node parent = depEl.getParentNode();
        if (parent == null) {
            return false;
        }
        Node grandparent = parent.getParentNode();
        if (grandparent == null) {
            return false;
        }
        return "dependencyManagement".equals(grandparent.getNodeName());
    }

    private static String textOf(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getParentNode() == parent) {
                return node.getTextContent().trim();
            }
        }
        return null;
    }

    private static String directChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && tagName.equals(child.getNodeName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    private static Element directChildElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && tagName.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static String firstChildText(Element root, String parentTag, String childTag) {
        Element parentEl = directChildElement(root, parentTag);
        if (parentEl == null) {
            return null;
        }
        return directChildText(parentEl, childTag);
    }

    // -------------------------------------------------------------------------
    // Property interpolation

    static String interpolate(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value != null ? value : "";
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int start = value.indexOf("${", i);
            if (start < 0) {
                result.append(value.substring(i));
                break;
            }
            result.append(value, i, start);
            int end = value.indexOf("}", start + 2);
            if (end < 0) {
                result.append(value.substring(start));
                break;
            }
            String key = value.substring(start + 2, end);
            String resolved = properties.get(key);
            if (resolved != null) {
                result.append(resolved);
            } else {
                // Fall back to system property, then leave placeholder.
                String sysProp = System.getProperty(key);
                result.append(sysProp != null ? sysProp : "${" + key + "}");
            }
            i = end + 1;
        }
        return result.toString();
    }

    // -------------------------------------------------------------------------
    // Local repository lookup

    private File locateJar(String groupId, String artifactId, String version) {
        File dir = artifactDir(groupId, artifactId, version);
        return new File(dir, artifactId + "-" + version + ".jar");
    }

    private File locatePom(String groupId, String artifactId, String version) {
        File dir = artifactDir(groupId, artifactId, version);
        return new File(dir, artifactId + "-" + version + ".pom");
    }

    private File artifactDir(String groupId, String artifactId, String version) {
        File dir = localRepository;
        String[] groupParts = groupId.split("\\.");
        for (String part : groupParts) {
            dir = new File(dir, part);
        }
        dir = new File(dir, artifactId);
        dir = new File(dir, version);
        return dir;
    }

    private static File defaultLocalRepository() {
        String localRepoProperty = System.getProperty("maven.repo.local");
        if (localRepoProperty != null && !localRepoProperty.isEmpty()) {
            return new File(localRepoProperty);
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            return new File(userHome, ".m2" + File.separator + "repository");
        }
        return new File(".m2" + File.separator + "repository");
    }

    // -------------------------------------------------------------------------
    // Internal data classes

    static final class PomData {
        final String groupId;
        final String artifactId;
        final String version;
        final Map<String, String> properties;
        final List<DependencyEntry> dependencies;

        PomData(
                String groupId,
                String artifactId,
                String version,
                Map<String, String> properties,
                List<DependencyEntry> dependencies
        ) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.properties = properties;
            this.dependencies = dependencies;
        }
    }

    static final class DependencyEntry {
        final String groupId;
        final String artifactId;
        final String version;
        final String scope;
        final boolean optional;

        DependencyEntry(String groupId, String artifactId, String version, String scope, boolean optional) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
            this.optional = optional;
        }
    }
}
