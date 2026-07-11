package io.github.jvmspec.cli.run;

/**
 * A single classpath entry specification: either a path-list argument or a
 * classpath-file argument.
 */
public final class ClasspathArgument {
    private final String optionName;
    private final String value;
    private final boolean file;

    private ClasspathArgument(String optionName, String value, boolean file) {
        this.optionName = optionName;
        this.value = value;
        this.file = file;
    }

    public static ClasspathArgument pathList(String optionName, String value) {
        return new ClasspathArgument(optionName, value, false);
    }

    public static ClasspathArgument file(String optionName, String value) {
        return new ClasspathArgument(optionName, value, true);
    }

    public String optionName() {
        return optionName;
    }

    public String value() {
        return value;
    }

    public boolean isFile() {
        return file;
    }
}
