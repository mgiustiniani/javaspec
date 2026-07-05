package io.github.jvmspec.compatibility;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Java8ApiUsageTest {
    @Test
    public void mainAndTestSourcesAvoidCommonPostJava8Apis() throws Exception {
        List<File> javaFiles = new ArrayList<File>();
        collectJavaFiles(new File("src/main/java"), javaFiles);
        collectJavaFiles(new File("src/test/java"), javaFiles);

        assertTrue("Expected Java sources to scan", javaFiles.size() > 0);

        String[] forbiddenTokens = forbiddenApiTokens();
        for (int fileIndex = 0; fileIndex < javaFiles.size(); fileIndex++) {
            File javaFile = javaFiles.get(fileIndex);
            String source = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
            for (int tokenIndex = 0; tokenIndex < forbiddenTokens.length; tokenIndex++) {
                String token = forbiddenTokens[tokenIndex];
                assertFalse(javaFile.getPath() + " contains a post-Java 8 API token: " + token, source.contains(token));
            }
        }
    }

    private static String[] forbiddenApiTokens() {
        List<String> tokens = new ArrayList<String>();
        staticCall(tokens, "List", "of");
        staticCall(tokens, "List", "copyOf");
        staticCall(tokens, "Set", "of");
        staticCall(tokens, "Set", "copyOf");
        staticCall(tokens, "Map", "of");
        staticCall(tokens, "Map", "ofEntries");
        staticCall(tokens, "Map", "entry");
        staticCall(tokens, "Map", "copyOf");
        staticCall(tokens, "Path", "of");
        staticCall(tokens, "Files", "readString");
        staticCall(tokens, "Files", "writeString");
        staticCall(tokens, "Stream", "ofNullable");
        staticCall(tokens, "Collectors", "filtering");
        staticCall(tokens, "Collectors", "flatMapping");
        staticCall(tokens, "Collectors", "teeing");
        staticCall(tokens, "Objects", "requireNonNullElse");
        staticCall(tokens, "Objects", "requireNonNullElseGet");
        staticCall(tokens, "Objects", "checkIndex");
        staticCall(tokens, "Objects", "checkFromToIndex");
        staticCall(tokens, "Objects", "checkFromIndexSize");
        instanceCall(tokens, "isBlank");
        instanceCall(tokens, "strip");
        instanceCall(tokens, "stripLeading");
        instanceCall(tokens, "stripTrailing");
        instanceCall(tokens, "lines");
        instanceCall(tokens, "repeat");
        instanceCall(tokens, "formatted");
        instanceCall(tokens, "transferTo");
        return tokens.toArray(new String[tokens.size()]);
    }

    private static void staticCall(List<String> tokens, String typeName, String methodName) {
        tokens.add(typeName + "." + methodName + "(");
    }

    private static void instanceCall(List<String> tokens, String methodName) {
        tokens.add("." + methodName + "(");
    }

    private static void collectJavaFiles(File root, List<File> javaFiles) {
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                collectJavaFiles(file, javaFiles);
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file);
            }
        }
    }
}
