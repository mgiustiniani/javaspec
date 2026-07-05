package io.github.jvmspec.reporting;

import io.github.jvmspec.runner.ExampleResult;
import io.github.jvmspec.runner.ExampleStatus;
import io.github.jvmspec.runner.FailureDetail;
import io.github.jvmspec.runner.RunResult;
import io.github.jvmspec.runner.SpecResult;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JUnitXmlReportWriterTest {
    private static final String TIMESTAMP = "2026-06-11T12:34:56Z";
    private static final String HOSTNAME = "ci-host";
    private static final ReportMetadata METADATA = ReportMetadata.of(TIMESTAMP, HOSTNAME, 1234L);

    @Test
    public void writesDeterministicJUnitXmlForAllExampleOutcomes() throws Exception {
        RunResult runResult = runResult(
                example("it_passes", ExampleStatus.PASSED, "", null),
                example("it_fails", ExampleStatus.FAILED, "Assertion failed", failure(new AssertionError("expected five"))),
                example("it_breaks", ExampleStatus.BROKEN, "Example method threw an unexpected throwable", failure(new IllegalStateException("boom"))),
                example("it_is_skipped", ExampleStatus.SKIPPED, "skipped temporarily", null),
                example("it_is_pending", ExampleStatus.PENDING, "pending implementation", null)
        );

        String xml = JUnitXmlReportWriter.toXml(runResult, METADATA);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<testsuite name=\"javaspec\" tests=\"5\" failures=\"1\" errors=\"1\" skipped=\"2\" timestamp=\"2026-06-11T12:34:56Z\" hostname=\"ci-host\" time=\"1.234\">\n" +
                "  <properties>\n" +
                "    <property name=\"javaspec.report.schemaVersion\" value=\"1\"/>\n" +
                "    <property name=\"javaspec.report.tool\" value=\"javaspec\"/>\n" +
                "  </properties>\n" +
                "  <testcase classname=\"spec.example.CalculatorSpec\" name=\"it_passes\" time=\"0\"/>\n" +
                "  <testcase classname=\"spec.example.CalculatorSpec\" name=\"it_fails\" time=\"0\">\n" +
                "    <failure type=\"java.lang.AssertionError\" message=\"expected five\">Assertion failed\n" +
                "java.lang.AssertionError: expected five</failure>\n" +
                "  </testcase>\n" +
                "  <testcase classname=\"spec.example.CalculatorSpec\" name=\"it_breaks\" time=\"0\">\n" +
                "    <error type=\"java.lang.IllegalStateException\" message=\"boom\">Example method threw an unexpected throwable\n" +
                "java.lang.IllegalStateException: boom</error>\n" +
                "  </testcase>\n" +
                "  <testcase classname=\"spec.example.CalculatorSpec\" name=\"it_is_skipped\" time=\"0\">\n" +
                "    <skipped message=\"skipped temporarily\"/>\n" +
                "  </testcase>\n" +
                "  <testcase classname=\"spec.example.CalculatorSpec\" name=\"it_is_pending\" time=\"0\">\n" +
                "    <skipped message=\"Pending: pending implementation\"/>\n" +
                "  </testcase>\n" +
                "</testsuite>\n", xml);
        assertParsesAsXml(xml);
    }

    @Test
    public void pendingWithDefaultReasonMapsToSkippedWithPendingMessage() throws Exception {
        String xml = JUnitXmlReportWriter.toXml(runResult(
                example("it_is_pending", ExampleStatus.PENDING, "Pending by javaspec.", null)
        ), METADATA);

        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"1\" timestamp=\"2026-06-11T12:34:56Z\" hostname=\"ci-host\" time=\"1.234\">");
        assertContains(xml, "<skipped message=\"Pending by javaspec.\"/>");
        assertParsesAsXml(xml);
    }

    @Test
    public void writesMetadataPropertiesAsFirstChildAndEscapesPropertyAttributes() throws Exception {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("prop&<>\"'", "value&<>\"'");
        properties.put("line\n\t", "return\r\n\t");
        ReportMetadata metadata = ReportMetadata.of(TIMESTAMP, "host&<>\"'", 1500L, properties);

        String xml = JUnitXmlReportWriter.toXml(RunResult.of(Collections.<SpecResult>emptyList()), metadata);

        assertContains(xml, "<testsuite name=\"javaspec\" tests=\"0\" failures=\"0\" errors=\"0\" skipped=\"0\" timestamp=\"2026-06-11T12:34:56Z\" hostname=\"host&amp;&lt;&gt;&quot;&apos;\" time=\"1.5\">\n" +
                "  <properties>\n" +
                "    <property name=\"prop&amp;&lt;&gt;&quot;&apos;\" value=\"value&amp;&lt;&gt;&quot;&apos;\"/>\n" +
                "    <property name=\"line&#10;&#9;\" value=\"return&#13;&#10;&#9;\"/>\n" +
                "  </properties>\n");
        Document document = parseXml(xml);
        Element root = document.getDocumentElement();
        assertEquals("testsuite", root.getTagName());
        assertEquals(TIMESTAMP, root.getAttribute("timestamp"));
        assertEquals("host&<>\"'", root.getAttribute("hostname"));
        assertEquals("1.5", root.getAttribute("time"));

        Element propertiesElement = firstChildElement(root);
        assertEquals("properties", propertiesElement.getTagName());
        NodeList propertyElements = propertiesElement.getElementsByTagName("property");
        assertEquals(2, propertyElements.getLength());
        Element firstProperty = (Element) propertyElements.item(0);
        assertEquals("prop&<>\"'", firstProperty.getAttribute("name"));
        assertEquals("value&<>\"'", firstProperty.getAttribute("value"));
    }

    @Test
    public void writesSourceFileAndLineAttributesWhenAvailable() throws Exception {
        String sourceFile = "src/test/java/spec/example/Calculator&Spec<One>\"Quote.java";
        ExampleResult sourced = ExampleResult.of(
                "spec.example.CalculatorSpec",
                "it_has_source",
                "it has source",
                0,
                ExampleStatus.PASSED,
                "",
                null,
                sourceFile,
                19
        );
        RunResult runResult = RunResult.of(Arrays.asList(SpecResult.of(
                "spec.example.CalculatorSpec",
                sourceFile,
                Arrays.asList(sourced)
        )));

        String xml = JUnitXmlReportWriter.toXml(runResult, METADATA);

        Element testcase = singleTestcase(xml);
        assertEquals("spec.example.CalculatorSpec", testcase.getAttribute("classname"));
        assertEquals("it_has_source", testcase.getAttribute("name"));
        assertEquals("0", testcase.getAttribute("time"));
        assertEquals(sourceFile, testcase.getAttribute("file"));
        assertEquals("19", testcase.getAttribute("line"));
        assertParsesAsXml(xml);
    }

    @Test
    public void writesDeterministicJUnitXmlForNoSpecRunResult() throws Exception {
        String xml = JUnitXmlReportWriter.toXml(RunResult.of(Collections.<SpecResult>emptyList()), METADATA);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<testsuite name=\"javaspec\" tests=\"0\" failures=\"0\" errors=\"0\" skipped=\"0\" timestamp=\"2026-06-11T12:34:56Z\" hostname=\"ci-host\" time=\"1.234\">\n" +
                "  <properties>\n" +
                "    <property name=\"javaspec.report.schemaVersion\" value=\"1\"/>\n" +
                "    <property name=\"javaspec.report.tool\" value=\"javaspec\"/>\n" +
                "  </properties>\n" +
                "</testsuite>\n", xml);
        assertParsesAsXml(xml);
    }

    @Test
    public void escapesAttributesTextAndProblematicCharacters() throws Exception {
        String invalidTextCharacter = String.valueOf((char) 0x0001);
        String invalidMessageCharacter = String.valueOf((char) 0x0002);
        String highSurrogate = String.valueOf((char) 0xD800);
        String emoji = new String(Character.toChars(0x1F600));
        String specName = "spec.example.Escaping&Spec<One>\"Quote'New\nLine";
        String methodName = "it_escapes_&<>\"'\n";
        String detail = "detail & < > \" '\n invalid " + invalidTextCharacter + " surrogate " + highSurrogate;
        String message = "message & < > \" '\n tab\t return\r invalid "
                + invalidMessageCharacter + " surrogate " + highSurrogate + " emoji " + emoji;
        IllegalArgumentException throwable = new IllegalArgumentException(message);
        throwable.setStackTrace(new StackTraceElement[0]);

        RunResult runResult = RunResult.of(Arrays.asList(SpecResult.of(specName, Arrays.asList(
                ExampleResult.of(
                        specName,
                        methodName,
                        "display name",
                        0,
                        ExampleStatus.FAILED,
                        detail,
                        FailureDetail.of(throwable)
                )
        ))));

        String xml = JUnitXmlReportWriter.toXml(runResult, METADATA);

        assertParsesAsXml(xml);
        assertContains(xml, "classname=\"spec.example.Escaping&amp;Spec&lt;One&gt;&quot;Quote&apos;New&#10;Line\"");
        assertContains(xml, "name=\"it_escapes_&amp;&lt;&gt;&quot;&apos;&#10;\"");
        assertContains(xml, "message=\"message &amp; &lt; &gt; &quot; &apos;&#10; tab&#9; return&#13; invalid \\u0002 surrogate \\uD800 emoji &#x1F600;\"");
        assertContains(xml, "detail &amp; &lt; &gt; \" '\n invalid \\u0001 surrogate \\uD800");
        assertContains(xml, "java.lang.IllegalArgumentException: message &amp; &lt; &gt; \" '\n tab\t return\r invalid \\u0002 surrogate \\uD800 emoji &#x1F600;");
        assertFalse(xml.indexOf((char) 0x0001) >= 0);
        assertFalse(xml.indexOf((char) 0x0002) >= 0);
        assertFalse(xml.indexOf((char) 0xD800) >= 0);
    }

    private static RunResult runResult(ExampleResult... examples) {
        return RunResult.of(Arrays.asList(SpecResult.of("spec.example.CalculatorSpec", Arrays.asList(examples))));
    }

    private static ExampleResult example(String methodName, ExampleStatus status, String detail, FailureDetail failureDetail) {
        return ExampleResult.of("spec.example.CalculatorSpec", methodName, methodName.replace('_', ' '), 0, status, detail, failureDetail);
    }

    private static FailureDetail failure(Throwable throwable) {
        throwable.setStackTrace(new StackTraceElement[0]);
        return FailureDetail.of(throwable);
    }

    private static void assertParsesAsXml(String xml) throws Exception {
        parseXml(xml);
    }

    private static Element singleTestcase(String xml) throws Exception {
        Document document = parseXml(xml);
        NodeList testcases = document.getElementsByTagName("testcase");
        assertEquals(1, testcases.getLength());
        return (Element) testcases.item(0);
    }

    private static Element firstChildElement(Element parent) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        throw new AssertionError("Expected first child element");
    }

    private static Document parseXml(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );
    }

    private static void assertContains(String value, String expected) {
        assertTrue("Expected to contain: " + expected + "\nActual value:\n" + value, value.contains(expected));
    }
}
