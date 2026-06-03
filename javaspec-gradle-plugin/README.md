# javaspec Gradle Plugin

Standalone optional Gradle plugin artifact for `org.javaspec:javaspec`.

This plugin is intentionally not registered as a root Maven module and is outside the zero-runtime-dependency core artifact. Repository-root `mvn verify` remains focused on the core artifact.

## Local verification

Install the current core snapshot first, then run the standalone plugin build with a compatible Gradle executable:

```sh
mvn -q -DskipTests install
gradle -p javaspec-gradle-plugin build
```

Phase 16 verification on the installed Java 21 runtime used Gradle 8.8 downloaded to `/tmp/gradle-8.8` and not committed:

```sh
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin test
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin build
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration runtimeClasspath
/tmp/gradle-8.8/bin/gradle -p javaspec-gradle-plugin dependencies --configuration testRuntimeClasspath
```

A cached Gradle 7.4.2 command was attempted but blocked by Java 21 with `Unsupported class file major version 65`; this is an environment/tooling compatibility blocker for that cached executable, not a javaspec feature failure.

## Usage

The plugin id is `org.javaspec`. In a consuming Gradle build where the standalone plugin artifact is available:

```groovy
plugins {
    id 'java'
    id 'org.javaspec' version '0.1.0-SNAPSHOT'
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

Supported task/extension options include `skip`, `failOnFailure` (default `true`), `stopOnFailure`, `configFile`, `suite`, `specDir`/`specRoot`, class filters, example filters, formatter `progress` or `pretty`, JSON report aliases (`reportFile`, `jsonReportFile`), and JUnit XML-compatible report aliases (`junitXmlReportFile`, `junitXmlFile`).

The task delegates to canonical no-JUnit `org.javaspec.invocation.JavaspecLauncher` without `System.exit`. Projects under test do not need JUnit; JUnit is only a plugin test dependency.
