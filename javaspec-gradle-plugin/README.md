# javaspec Gradle Plugin

Standalone optional Gradle plugin artifact for `io.github.jvmspec:javaspec`.

This plugin is intentionally not registered as a root Maven module and is outside the zero-runtime-dependency core artifact. Repository-root `mvn verify` remains focused on the core artifact.

## Local verification

Install the current core snapshot first, then run the standalone plugin build with a compatible Gradle executable:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
```

Phase 16 verification on the installed Java 21 runtime used Gradle 8.8 downloaded to `/tmp/gradle-8.8` and not committed. Phase 18 verification reused `/tmp/gradle-8.8` and passed `clean test build` with 11 tests plus the `runtimeClasspath` audit.

```sh
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin clean test build
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration testRuntimeClasspath
```

A cached Gradle 7.4.2 command was attempted but blocked by Java 21 with `Unsupported class file major version 65`; this is an environment/tooling compatibility blocker for that cached executable, not a javaspec feature failure.

## Usage

The plugin id is `io.github.jvmspec`. The plugin is published on the Gradle Plugin Portal. In a consuming Gradle build:

```groovy
plugins {
    id 'java'
    id 'io.github.jvmspec' version '1.0.0-RC4'
}

javaspec {
    suite = 'default'
    formatter = 'progress'
    reportFile = file("$buildDir/reports/javaspec/report.json")
    junitXmlReportFile = file("$buildDir/test-results/javaspec.xml")
}

tasks.named('javaspecRun') {
    stopOnFailure = true
    failOnFailure = true
}
```

The plugin registers extension `javaspec` and task `javaspecRun` in Gradle's `verification` group. When the Gradle Java plugin/source sets are present, `javaspecRun` defaults to the `test` source set runtime classpath and depends on `testClasses`.

Supported task/extension options include `skip`, `failOnFailure` (default `true`), `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, class filters, example filters, formatter selection, JSON report aliases (`reportFile`, `jsonReportFile`), and JUnit XML-compatible report aliases (`junitXmlReportFile`, `junitXmlFile`). JSON and JUnit XML-compatible reports are produced by core writers and include Phase 18 stable id/source metadata plus Phase 22 pending statuses/counts where available.

`javaspecRun` loads built-in formatters first and then discovers external formatter/extension providers through JDK `ServiceLoader` from the task run classloader. Provider jars can be on the configured task classpath, the extension classpath, or the default Java test runtime classpath when that default is active. Formatter selection precedence is task setting, extension setting, project property `javaspec.formatter`, config `formatter`, then default `progress`. Invalid formatter diagnostics list all discovered formatter names.

A provider jar can expose a direct formatter or an extension with service files such as:

```text
# META-INF/services/io.github.jvmspec.formatter.RunFormatter
com.example.javaspec.MarkdownRunFormatter

# META-INF/services/io.github.jvmspec.extension.JavaspecExtension
com.example.javaspec.MarkdownExtension

# alias service type also supported:
# META-INF/services/io.github.jvmspec.extension.Extension
com.example.javaspec.MarkdownExtension
```

Example consumer configuration when the formatter jar is on `testRuntimeClasspath`:

```groovy
dependencies {
    testRuntimeOnly 'com.example:javaspec-markdown-formatter:1.0.0'
}

tasks.named('javaspecRun') {
    formatter = 'markdown'
}
```

When `configFile` points at a javaspec config that defines top-level report destinations, the task uses those destinations as defaults if no explicit extension/task report setting is present. Supported config aliases are `report`, `reportFile`, `report-file`, `jsonReport`, `jsonReportFile`, and `json-report-file` for JSON, plus `junitXml`, `junit-xml`, `junitXmlFile`, `junit-xml-file`, `junitXmlReportFile`, and `junit-xml-report-file` for JUnit XML-compatible reports. Explicit Gradle adapter settings override config values.

When discovery finds source specs/examples whose compiled spec class, expected public no-argument example method, or dependencies are unavailable on the Gradle classpath, `javaspecRun` logs `javaspec:` warning diagnostics plus the Gradle classpath element count. This is classpath guidance only: the task still expects Gradle/Java compilation (`testClasses` by default) to produce compiled outputs, and failure semantics are unchanged.

The task delegates to canonical no-JUnit `io.github.jvmspec.invocation.JavaspecLauncher` without `System.exit`. Projects under test do not need JUnit; JUnit is only a plugin test dependency.
