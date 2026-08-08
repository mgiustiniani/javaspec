# JavaSpec project-specific native example

This standalone Maven consumer builds a machine executable containing its compiled production class,
specification, generated support, JavaSpec core runner, and Native Image reflection metadata.

The preview was added after `v1.0.0-RC5`; install the current checkout before building:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-maven-plugin/pom.xml -DskipTests install
mvn -f examples/native-basic/pom.xml -Pnative clean package

examples/native-basic/target/javaspec-native-basic
examples/native-basic/target/javaspec-native-basic --formatter json
```

Requirements: Maven and GraalVM Native Image 25. The executable is a consumer build output, not a
published JavaSpec artifact.

It demonstrates:

- generated `CalculatorSpecSupport`;
- a constructor argument configured in `let()`;
- `letGo()` lifecycle execution;
- passing, pending, and skipped examples;
- pretty/progress/JSON console output;
- generated build-linked launcher and reflection metadata.

See [`../../docs/native-image.md`](../../docs/native-image.md) for current limits and the reusable
Maven configuration.
