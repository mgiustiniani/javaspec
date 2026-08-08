# ByteBuddy concrete-class doubles

## Overview

javaspec core creates ordinary interface doubles with JDK dynamic proxies. The optional
`javaspec-bytecode-doubles` adapter registers a `ConcreteDoubleProvider` that uses ByteBuddy
subclass generation for non-final concrete classes. ByteBuddy remains outside the
zero-runtime-dependency core.

## Installation

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec-bytecode-doubles</artifactId>
  <version>1.0.0-RC5</version>
  <scope>test</scope>
</dependency>
```

The adapter is available from Maven Central. For source-checkout development, install core and the
standalone adapter before running a consumer:

```sh
mvn -q -DskipTests install
mvn -q -f javaspec-bytecode-doubles/pom.xml -DskipTests install
```

## Direct double API

`Doubles.concreteDouble(Class<T>)` returns the same `InterfaceDouble<T>` handle used by interface
doubles, but its instance is a generated subclass of the concrete type:

```java
import io.github.jvmspec.doubles.Doubles;
import io.github.jvmspec.doubles.InterfaceDouble;

InterfaceDouble<DataStore> store = Doubles.concreteDouble(DataStore.class);
store.control().returnsFor("save", true, "item");
store.control().returnsFor("find", "saved item", "item-1");

DataService service = new DataService(store.instance());
match(service.save("item")).shouldReturn(true);
match(service.lookup("item-1")).shouldReturn("saved item");

store.control().verifyCalled("save", "item");
```

`Doubles.classDouble(Class<T>)` is an alias. Without a matching provider on the effective
classloader, both methods fail with an `IllegalStateException` that identifies the missing adapter.

## Prophecy API

`Prophecies.prophesize(Class<T>)` and `ObjectBehavior.prophesize(Class<T>)` choose the appropriate
double mechanism. Interfaces use core JDK proxies; a non-final concrete class uses the installed
bytecode-doubles provider:

```java
ObjectProphecy<DataStore> store = prophesize(DataStore.class);
store.method("find", "item-1").willReturn("saved item");

DataService service = new DataService(store.reveal());
match(service.lookup("item-1")).shouldReturn("saved item");
```

Generated typed `*Prophecy` wrappers remain the preferred PHPSpec-like syntax when available. Do
not create an invented interface bridge or call a two-class `concreteDouble(...)` overload; no such
public API exists in 1.0.

## Supported targets and limits

`javaspec-bytecode-doubles` supports non-final concrete classes. It rejects:

- final classes;
- interfaces (use core `Doubles.interfaceDouble(...)` instead);
- enums, arrays, annotations, and primitive types;
- static methods and constructor interception.

For final-class, static-method, or construction-aware doubles, use the separate test-scoped
`javaspec-bytecode-agent` adapter and explicit JVM instrumentation. Keep both bytecode adapters out
of core and prefer interface-oriented design where practical.

## See also

- [Concrete-class example](../examples/bytecode-doubles-basic/)
- [Bytecode-agent example](../examples/bytecode-agent-basic/)
- [README doubles section](../README.md#doubles)
- [README Prophecy section](../README.md#prophecy-style-doubles)
- [Migration guide](migration-guide-1.0.md)
- [Subclass-adapter decision](adr/0024-standalone-optional-bytecode-doubles-adapter.md)
- [Instrumentation-adapter decision](adr/0027-standalone-bytecode-agent-adapter.md)
