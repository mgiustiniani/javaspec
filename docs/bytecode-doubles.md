# ByteBuddy Concrete-Class Doubles

## Overview

javaspec's core doubles API supports interface doubles via JDK dynamic proxies. For non-final
concrete classes, the optional `javaspec-bytecode-doubles` adapter provides ByteBuddy-based
subclass generation.

## Usage

Add the adapter dependency to your project:

```xml
<dependency>
    <groupId>io.github.jvmspec</groupId>
    <artifactId>javaspec-bytecode-doubles</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

Create concrete-class doubles:

```java
import org.javaspec.doubles.Doubles;
import org.javaspec.doubles.InterfaceDouble;

InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(DataStore.class);
storeDouble.control().returns("save", true);
```

## Using Concrete-Class Doubles with Prophecy Wrappers

Prophecy-style doubles (`ObjectProphecy`, `MethodProphecy`) normally wrap an `InterfaceDouble<T>`
created by `Doubles.interfaceDouble(Class<T>)`, which requires an interface type. However, you
**can** use a concrete-class double with the Prophecy API when the concrete class is wrapped
behind an interface.

### Pattern: Interface-prophecy wrapping a concrete double

Create an interface that describes the concrete class's contract, then use the concrete double
behind that interface:

```java
// 1. Define an interface matching the concrete class's contract
interface DataStore {
    boolean save(String item);
    String find(String key);
}

// 2. Concrete class (non-final, non-interface)
public class FileDataStore {
    public boolean save(String item) { /* real impl */ }
    public String find(String key) { /* real impl */ }
}

// 3. Use concrete double via interface prophecy
InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(FileDataStore.class,
        DataStore.class);
// Note: concreteDouble(concreteClass, asInterfaceType) bridges the concrete class
// through an interface type.

ObjectProphecy<DataStore> store = new ObjectProphecy<>(storeDouble, null);
store.method("save", any()).willReturn(true);
```

To use concrete-class doubles with Prophecy-style predictions, wrap the concrete double in an
interface-based prophecy as shown above, or continue using the existing `DoubleControl` API directly:

```java
InterfaceDouble<DataStore> storeDouble = Doubles.concreteDouble(DataStore.class);
storeDouble.control().returns("save", true);
storeDouble.control().verifyCalled("save", "item");
```

## Limitations

- Concrete-class doubles support **non-final classes only**.
- Final classes, enums, arrays, annotations, primitives, and interfaces are rejected.
- Static method mocking, constructor mocking, and final-class mocking are outside scope.
- Direct `ObjectProphecy<ConcreteClass>` is not covered — use an interface wrapper.

## See also

- [examples/bytecode-doubles-basic/](../examples/bytecode-doubles-basic/) — concrete-class doubles example.
- [README Doubles section](../README.md#doubles) — interface doubles in core.
- [README Prophecy section](../README.md#prophecy-style-doubles) — Prophecy-style doubles overview.
- [docs/migration-guide.md](migration-guide.md) — migrating from existing doubles API.
