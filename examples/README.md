# javaspec standalone examples

These examples are standalone consumer projects. They are not root Maven modules and are not part of
the repository-root Maven reactor.

Artifacts are published on Maven Central under `io.github.jvmspec`. For development from source,
install local snapshots before running the Maven, JUnit Platform, bytecode doubles, and bytecode
agent examples.

The Gradle example uses an included build for `javaspec-gradle-plugin`, but that plugin build still
resolves the core `io.github.jvmspec:javaspec` from Maven local for snapshot builds.

The easiest local check is:

```sh
scripts/verify-examples.sh
```

Manual setup and runs from the repository root:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -DskipTests install
mvn -q -f javaspec-junit-platform-engine/pom.xml -DskipTests install
mvn -q -f javaspec-bytecode-doubles/pom.xml -DskipTests install
mvn -q -f javaspec-bytecode-agent/pom.xml -DskipTests install

mvn -q -f examples/maven-basic/pom.xml verify
mvn -q -f examples/prophecy-basic/pom.xml verify
mvn -q -f examples/junit-platform-basic/pom.xml test
mvn -q -f examples/bytecode-doubles-basic/pom.xml verify
mvn -q -f examples/bytecode-agent-basic/pom.xml verify
gradle -p examples/gradle-basic clean javaspecRun
```

Outputs:

- `examples/maven-basic/target/javaspec/run-report.json`
- `examples/maven-basic/target/javaspec/junit-report.xml`
- `examples/gradle-basic/build/reports/javaspec/run-report.json`
- `examples/gradle-basic/build/reports/javaspec/junit-report.xml`
- `examples/junit-platform-basic/target/surefire-reports/`
- `examples/bytecode-doubles-basic/target/javaspec/run-report.json`
- `examples/bytecode-doubles-basic/target/javaspec/junit-report.xml`
- `examples/bytecode-agent-basic/target/javaspec/run-report.json`
- `examples/bytecode-agent-basic/target/javaspec/junit-report.xml`
- `examples/prophecy-basic/target/javaspec/run-report.json`
- `examples/prophecy-basic/target/javaspec/junit-report.xml`

## Prophecy-style doubles example

[`examples/prophecy-basic/`](prophecy-basic/) demonstrates the Prophecy-style doubles API with:

- `Mailer.java` — an interface to prophesize
- `MailerSpec.java` — a javaspec spec using `prophesize()`, `MethodProphecy`, argument matchers,
  promises (`willReturn`, `willThrow`), and predictions (`shouldBeCalled`, `shouldNotBeCalled`,
  `shouldBeCalledTimes`)
- `Verify.java` — a standalone verification test exercising the API programmatically

Specs can explicitly skip or mark examples pending without adding dependencies:

```java
import io.github.jvmspec.api.Pending;
import io.github.jvmspec.api.Skip;

public class PaymentSpec extends PaymentSpecSupport {
    @Skip(reason = "requires external sandbox")
    public void it_refunds_a_charge() {
    }

    @Pending("waiting for billing provider")
    public void it_charges_a_card() {
    }

    public void it_can_stop_at_runtime() {
        pending("replace stub with contract test");
        // or: skip("requires test data")
    }
}
```

JSON reports include separate `pending` counts and `status: "PENDING"`.

JUnit XML-compatible reports map pending examples to `<skipped message="Pending: ...">` and include
pending in the testsuite `skipped` attribute.

## Bytecode agent example

[`examples/bytecode-agent-basic/`](bytecode-agent-basic/) demonstrates the optional
`javaspec-bytecode-agent` adapter with:

- a final-class collaborator doubled through `Doubles.concreteDouble(FinalGreeter.class)`;
- a scoped static-method double through `BytecodeAgentDoubles.staticDouble(StaticFormatter.class)`.

If Gradle is not available locally, set `JAVASPEC_SKIP_GRADLE_EXAMPLE=1` when running
`scripts/verify-examples.sh`. To skip only bytecode examples, set
`JAVASPEC_SKIP_BYTECODE_DOUBLES_EXAMPLE=1` or `JAVASPEC_SKIP_BYTECODE_AGENT_EXAMPLE=1`.
