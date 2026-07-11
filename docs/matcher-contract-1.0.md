# Matcher contract — 1.0

javaspec matchers are part of the PHPSpec-first authoring API. The 1.0 contract keeps matcher use
subject-centric and zero-runtime-dependency while explicitly narrowing custom matcher scope to the
stable Java registry API.

## Built-in matcher scope

The stable 1.0 built-in matcher surface includes:

- equality/identity aliases through `match(actual)` and generated typed support helpers;
- type/instance/implementation checks;
- string containment, prefix/suffix, and Java regular-expression checks;
- collection/map/array/iterable/iterator containment, count, and emptiness checks;
- map key/value checks;
- Java-adapted approximate numeric checks;
- generated object-state expectations such as `shouldBeActive()` and `shouldHaveTitle(expected)`;
- exception expectations through generated support `shouldThrow(...).during...` helpers.

Approximate numeric semantics are Java-adapted: values compare through deterministic decimal
`BigDecimal` conversion, tolerance is inclusive, and `NaN`, infinities, `null`, non-numeric values,
and negative tolerances fail with assertion errors.

Iterator semantics are Java-adapted and explicit: containment/count checks intentionally consume
iterator elements, while empty/not-empty checks only call `hasNext()`.

Generated object-state expectations are Java-adapted instead of PHP dynamic dispatch. They are emitted
in generated `*SpecSupport` helpers when a subject accessor is known and unambiguous. Ambiguous helper
signatures are skipped fail-closed.

## Custom matcher scope

The stable 1.0 custom matcher surface is programmatic and per-spec/per-registry:

- `io.github.jvmspec.matcher.Matcher`;
- `io.github.jvmspec.matcher.CustomMatcher`;
- `io.github.jvmspec.matcher.MatchResult`;
- `io.github.jvmspec.matcher.MatcherRegistry`;
- `ObjectBehavior.matcherRegistry()` and `ObjectBehavior.setMatcherRegistry(...)`;
- `match(actual).shouldMatch("name", args...)`.

Custom matchers receive the actual subject value, including `null`, and the expected argument list.
They return `MatchResult.passed()` or `MatchResult.failure(message)`. Failure messages are surfaced as
`AssertionError` messages by `shouldMatch(...)`.

Example:

```java
matcherRegistry().register("beAbsent", new CustomMatcher<Object>(
    "beAbsent",
    new CustomMatcher.MatchPredicate<Object>() {
        public boolean test(Object subject, Object... expected) {
            return subject == null;
        }
    }
));

match(null).shouldMatch("beAbsent");
```

Matcher names are exact registry keys. javaspec 1.0 does not normalize custom matcher names or load
custom matchers implicitly from configuration.

## Deferred custom matcher features

The following PHPSpec-like custom matcher conveniences are explicitly deferred from 1.0:

- configuration-file registration of custom matcher classes;
- extension-driven automatic matcher registration through the javaspec config model;
- PHP-style inline matcher arrays or dynamic method generation for arbitrary matcher names;
- automatic conversion of custom matcher names into generated typed proxy methods;
- negated custom matcher auto-generation beyond explicitly registered matcher names.

These deferrals do not affect the PHPSpec-first 1.0 workflow because specs can still register custom
matcher objects directly in Java and call them through `shouldMatch(...)`. Future releases may add
configured or extension-provided matcher registration additively without changing the 1.0 registry
contract.

## Discovery and generation boundary

`SpecDiscovery` recognizes the built-in matcher/helper names used for source-generation inference.
Custom `shouldMatch("name", ...)` calls are assertions against existing values; they do not imply that
javaspec should generate production methods for the custom matcher name.

Adapters and extensions must not reinterpret custom matcher failures as Jupiter assumptions or other
foreign result states. They remain normal javaspec assertion failures unless the matcher itself throws
an unexpected throwable, in which case normal javaspec broken-example rules apply.
