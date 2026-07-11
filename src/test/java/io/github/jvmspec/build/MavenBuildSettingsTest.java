package io.github.jvmspec.build;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MavenBuildSettingsTest {
    @Test
    public void compilerPropertiesUseJava8SourceAndTarget() throws Exception {
        Document pom = readPom();
        Element properties = directChild(pom.getDocumentElement(), "properties");

        assertNotNull(properties);
        assertEquals("1.8", directChildText(properties, "maven.compiler.source"));
        assertEquals("1.8", directChildText(properties, "maven.compiler.target"));
    }

    @Test
    public void junitIsTheOnlyProjectDependencyAndItIsTestScoped() throws Exception {
        Document pom = readPom();
        Element dependencies = directChild(pom.getDocumentElement(), "dependencies");
        assertNotNull(dependencies);

        Element dependency = null;
        int dependencyCount = 0;
        NodeList children = dependencies.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && "dependency".equals(child.getNodeName())) {
                dependency = (Element) child;
                dependencyCount++;
            }
        }

        assertEquals(1, dependencyCount);
        assertNotNull(dependency);
        assertEquals("junit", directChildText(dependency, "groupId"));
        assertEquals("junit", directChildText(dependency, "artifactId"));
        assertEquals("test", directChildText(dependency, "scope"));
    }

    private static Document readPom() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(new File("pom.xml"));
    }

    private static String directChildText(Element parent, String childName) {
        Element child = directChild(parent, childName);
        if (child == null) {
            return null;
        }
        return child.getTextContent().trim();
    }

    private static Element directChild(Element parent, String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && childName.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }
}
