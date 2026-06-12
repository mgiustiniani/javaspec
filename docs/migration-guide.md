# Migration Guide: Existing Doubles API → Prophecy-Style Doubles API

This guide shows how to migrate existing javaspec specs that use the low-level
`Doubles.create()`, `InterfaceDouble`, and `DoubleControl` API to the expressive
Prophecy-style doubles API (`ObjectProphecy`, `MethodProphecy`, `Promise`, `Prediction`).

## Why migrate?

The Prophecy-style API is inspired by [phpspec/prophecy](https://github.com/phpspec/prophecy) and
offers:

- **Declarative stubs + predictions in one chain** — `.willReturn(value).shouldBeCalled()` reads
  like a sentence.
- **Argument matchers built in** — `any()`, `eq()`, `containingString()`, `isNull()`, `notNull()`
  available directly via `Argument` or `Arg` static imports.
- **Typed wrapper classes** — generated `*Prophecy` classes provide concise
  `mailer.send(any(), eq("hello")).willReturn(true)` without `.method("send", ...)` strings.
- **Auto-check predictions** — `--auto-check-predictions` flag automatically verifies all
  predictions at the end of each example.

Both APIs are available simultaneously; migration can be gradual.

## Migration steps

### Step 1: Replace `Doubles.create()` with `prophesize()`

**Before (existing doubles API):**

```java
InterfaceDouble<Mailer> mailerDouble = Doubles.interfaceDouble(Mailer.class);
beConstructedWith(mailerDouble.instance());
mailerDouble.control().returns("send", true);
match(subject().send("to", "sub", "body")).shouldReturn(true);
mailerDouble.control().verifyCalled("send", "to", "sub", "body");
```

**After (Prophecy-style reflective API):**

```java
ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);
beConstructedWith(mailer.reveal());
mailer.method("send", eq("to"), any(), any()).willReturn(true);
match(subject().send("to", "sub", "body")).shouldReturn(true);
mailer.method("send", eq("to"), any(), any()).shouldBeCalled();
```

### Step 2: Replace `control().returns()` with `.willReturn()`

| Old API | New API |
|---|---|
| `control().returns("method", value)` | `method("method", ...).willReturn(value)` |
| `control().returnsFor("method", value, matchers...)` | `method("method", matchers...).willReturn(value)` |
| `control().returnsThrowing("method", exception)` | `method("method", ...).willThrow(exception)` |
| `control().when("method", args).thenReturn(value)` | `method("method", args).willReturn(value)` |
| `control().when("method", args).thenThrow(ex)` | `method("method", args).willThrow(ex)` |

### Step 3: Replace verification calls with predictions

| Old API | New API |
|---|---|
| `control().verifyCalled("method", args)` | `method("method", args).shouldBeCalled()` |
| `control().verifyNotCalled("method", args)` | `method("method", args).shouldNotBeCalled()` |
| `control().verifyCallCount("method", count, args)` | `method("method", args).shouldBeCalledTimes(count)` |

### Step 4: Replace `Doubles.any()` / `Doubles.eq()` imports

Old import style:

```java
import static org.javaspec.doubles.Doubles.any;
import static org.javaspec.doubles.Doubles.eq;
```

New import style:

```java
import static org.javaspec.doubles.prophecy.Argument.*;
import static org.javaspec.doubles.prophecy.Arg.*;
```

`Argument` and `Arg` provide the same matchers: `any()`, `any(Class<?>)`, `eq(Object)`,
`isNull()`, `notNull()`, `containingString(String)`.

### Step 5 (optional): Use the generated typed wrapper

Instead of the reflective `.method("send", ...)` calls, generate a typed wrapper:

```sh
javaspec prophesize com.example.Mailer
```

Then use:

```java
MailerProphecy mailer = new MailerProphecy(
    Doubles.interfaceDouble(Mailer.class),
    new PredictionRegistry()
);
mailer.send(any(String.class), eq("hello"), any(String.class))
    .willReturn(true)
    .shouldBeCalled();
```

Or inside a spec with auto-generated wrapper discovery (`javaspec run --generate`):

```java
// No manual wrapper instantiation needed — the generated support class
// provides the MailerProphecy via prophesize() with auto-registered predictions.
ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);
// ... same as Step 1 but with typed method names available on the wrapper
```

### Step 6: Enable auto-check predictions

Add `--auto-check-predictions` to your `javaspec run` command:

```sh
javaspec run --auto-check-predictions --generate --compile --formatter pretty
```

Or set it in your spec's constructor:

```java
public MailerSpec() {
    super(Mailer.class);
    setAutoCheckPredictions(true);
}
```

When enabled, `checkPredictions()` is called automatically after each example.

### Step 7: Remove explicit `checkPredictions()` calls

**Before:**

```java
public void it_sends_mail() {
    ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);
    mailer.method("send", any(), any(), any()).willReturn(true).shouldBeCalled();
    mailer.reveal().send("to", "sub", "body");
    checkPredictions();  // explicit call
}
```

**After (with auto-check enabled):**

```java
public void it_sends_mail() {
    ObjectProphecy<Mailer> mailer = prophesize(Mailer.class);
    mailer.method("send", any(), any(), any()).willReturn(true).shouldBeCalled();
    mailer.reveal().send("to", "sub", "body");
    // checkPredictions() is called automatically by the runner
}
```

## Full migration example

### Before (existing API)

```java
import static org.javaspec.doubles.Doubles.*;

public class OldNotifierSpec extends ObjectBehavior<NotifierService> {
    public OldNotifierSpec() {
        super(NotifierService.class);
    }

    public void it_notifies_the_user() {
        InterfaceDouble<Notifier> notifier = Doubles.interfaceDouble(Notifier.class);
        notifier.control().returns("send", true);
        beConstructedWith(notifier.instance());
        match(subject().notify("hello")).shouldReturn(true);
        notifier.control().verifyCalled("send", "hello");
    }
}
```

### After (Prophecy-style API)

```java
import static org.javaspec.doubles.prophecy.Argument.*;

public class NewNotifierSpec extends ObjectBehavior<NotifierService> {
    public NewNotifierSpec() {
        super(NotifierService.class);
        setAutoCheckPredictions(true);
    }

    public void it_notifies_the_user() {
        ObjectProphecy<Notifier> notifier = prophesize(Notifier.class);
        notifier.method("send", eq("hello")).willReturn(true).shouldBeCalled();
        beConstructedWith(notifier.reveal());
        match(subject().notify("hello")).shouldReturn(true);
    }
}
```

## Mixed API usage

You can use both APIs in the same spec if needed. The old API is not deprecated.

```java
public void it_mixes_apis() {
    // Old-style double
    InterfaceDouble<Mailer> oldDouble = Doubles.interfaceDouble(Mailer.class);
    oldDouble.control().returns("send", true);

    // New-style prophecy on the same double
    ObjectProphecy<Mailer> mailer = new ObjectProphecy<>(oldDouble, null);
    mailer.method("name").willReturn("HybridMailer");
}
```

## What stays the same

- `beConstructedWith(...)` — unchanged.
- `match(...).shouldReturn(...)` — unchanged.
- `subject()` — unchanged.
- `interfaceDouble(Class)` / `Doubles.interfaceDouble()` — still available.
- `Doubles.concreteDouble(Class)` / `classDouble()` — still available for concrete-class doubles.

## What changes

- `control().returns(...)` → `.willReturn(...)` on a `MethodProphecy`.
- `control().verifyCalled(...)` → `.shouldBeCalled()` on a `MethodProphecy`.
- `Doubles.any()` / `Doubles.eq()` → `Argument.any()` / `Argument.eq()` or `Arg.any()` / `Arg.eq()`.
- No need for manual `control().when(...).thenReturn(...)` — use `method().willReturn(...)`.

## See also

- [README Prophecy section](../README.md#prophecy-style-doubles) — overview and code examples.
- [examples/prophecy-basic/](../examples/prophecy-basic/) — complete working example.
- [docs/bytecode-doubles.md](bytecode-doubles.md) — using concrete-class doubles with Prophecy wrappers.
