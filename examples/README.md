# javaspec standalone examples

These examples are standalone consumer projects. They are not root Maven modules and are not part of the repository-root Maven reactor.

Until javaspec artifacts are publicly published, install local snapshots before running the Maven and JUnit Platform examples. The Gradle example uses an included build for `javaspec-gradle-plugin`, but that plugin build still resolves the core `org.javaspec:javaspec` snapshot from Maven local.

The easiest local check is:

```sh
scripts/verify-examples.sh
```

Manual setup and runs from the repository root:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml -DskipTests install

mvn -q -f examples/maven-basic/pom.xml verify
mvn -q -f examples/junit-platform-basic/pom.xml test
gradle -p examples/gradle-basic clean javaspecRun
```

Outputs:

- `examples/maven-basic/target/javaspec/run-report.json`
- `examples/maven-basic/target/javaspec/junit-report.xml`
- `examples/gradle-basic/build/reports/javaspec/run-report.json`
- `examples/gradle-basic/build/reports/javaspec/junit-report.xml`
- `examples/junit-platform-basic/target/surefire-reports/`

If Gradle is not available locally, set `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` when running `scripts/verify-examples.sh`.
