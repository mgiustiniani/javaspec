package org.javaspec.generation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans spec source files for {@code prophesize()} and {@code prophecy()} calls
 * to detect missing typed wrapper classes.
 */
public final class ProphecyExistenceChecker {

    private static final Pattern PROPHESIZE_PATTERN =
            Pattern.compile("\\b(?:prophesize|prophecy)\\(\\s*(\\w+(?:\\.\\w+)*)\\.class\\s*\\)");

    private ProphecyExistenceChecker() {
    }

    /**
     * Scans a spec source file for prophesize/prophecy calls and returns
     * the qualified class names found.
     *
     * @param specFile the spec source file to scan
     * @return list of qualified class names found, or empty list
     * @throws IOException if reading fails
     */
    public static List<String> findProphesizedInterfaces(File specFile) throws IOException {
        List<String> results = new ArrayList<String>();
        String source = new String(Files.readAllBytes(specFile.toPath()), StandardCharsets.UTF_8);
        Matcher matcher = PROPHESIZE_PATTERN.matcher(source);
        while (matcher.find()) {
            String className = matcher.group(1);
            if (!results.contains(className)) {
                results.add(className);
            }
        }
        return results;
    }

    /**
     * Scans multiple spec source files for prophesize/prophecy calls.
     *
     * @param specFiles list of spec source files
     * @return list of qualified class names found
     * @throws IOException if reading any file fails
     */
    public static List<String> findProphesizedInterfaces(List<File> specFiles) throws IOException {
        List<String> results = new ArrayList<String>();
        for (File specFile : specFiles) {
            List<String> found = findProphesizedInterfaces(specFile);
            for (String fqcn : found) {
                if (!results.contains(fqcn)) {
                    results.add(fqcn);
                }
            }
        }
        return results;
    }

    /**
     * Checks whether a prophecy wrapper class exists for the given interface.
     *
     * @param interfaceFqcn the fully qualified interface class name
     * @param classLoader   the class loader to check with
     * @return true if the wrapper class is loadable
     */
    public static boolean wrapperExists(String interfaceFqcn, ClassLoader classLoader) {
        String wrapperFqcn = interfaceFqcn + "Prophecy";
        try {
            Class.forName(wrapperFqcn, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Resolves an interface class from its qualified name.
     *
     * @param fqcn       the fully qualified class name
     * @param classLoader the class loader
     * @return the interface class, or null if not found or not an interface
     */
    public static Class<?> resolveInterface(String fqcn, ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(fqcn, false, classLoader);
            return clazz.isInterface() ? clazz : null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Determines the target package for a prophecy wrapper given an interface FQCN.
     * Uses the interface's package by default.
     *
     * @param interfaceFqcn the fully qualified interface class name
     * @return the package name (may be empty for default package)
     */
    public static String defaultPackage(String interfaceFqcn) {
        int lastDot = interfaceFqcn.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return interfaceFqcn.substring(0, lastDot);
    }
}
