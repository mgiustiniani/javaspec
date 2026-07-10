# javaspec 1.0 collaborator and Prophecy contract

Prophecy-style collaborators are part of the PHPSpec-first semantic core of javaspec. They are not a generic Java DI container or a Jupiter extension model.

## Collaborator injection

Supported injection targets for `let`, example methods, and `letGo`:

- ordinary interfaces, injected as the same revealed double instance within one example lifecycle;
- generated typed `*Prophecy` wrappers, injected as the same prophecy wrapper/control within one example lifecycle.

Rules:

- One collaborator identity is shared across `let`, the example, and `letGo` for the same supported type.
- Duplicate same-type collaborator parameters in one method are ambiguous and produce `BROKEN`.
- Unsupported parameter types produce `BROKEN` with an actionable unsupported-injection diagnostic.
- Injection is deterministic and subject to normal Java method parameter order.
- Collaborator injection does not construct arbitrary services and does not inspect application containers.

## Prediction lifecycle

- Predictions configured through Prophecy are checked automatically after the example body.
- Prediction checking runs before `letGo`.
- `letGo` always runs after examples, pending/skip signals, and prediction failures.
- If prediction checking fails and `letGo` also fails, the final result is `BROKEN` with teardown failure detail and suppressed prediction context.
- Manual prediction checking remains available through the registry/control APIs for advanced cases.

## Generated typed wrappers

Generated `*Prophecy` wrappers are the canonical PHPSpec-like syntax for Java:

```java
public void let(MailerProphecy mailer) {
    mailer.send("user@example.com").willReturn(true).shouldBeCalled();
    setMailer(mailer.reveal());
}
```

The reflective `method("name", ...)` API remains a bootstrap/fallback surface. Wrapper generation must be deterministic and fail closed when Java overloads would make a helper ambiguous.

## Argument tokens

Supported token families:

- `any(type)` / `type(type)`;
- equality tokens;
- identity tokens: `same(...)` / `identicalTo(...)`;
- set membership: `in(...)` / `notIn(...)`;
- predicate/custom tokens: `matching(...)`, `ArgumentToken`, `Argument.token(...)`, `Argument.custom(...)`, `Arg.token(...)`, `Arg.custom(...)`;
- generated wrapper overloads accept token values through same-name `Object` overloads where Java overload resolution requires it.

## Predictions and callbacks

Supported prediction forms include should-be-called / should-not-be-called style predictions, call-count checks, and custom callbacks through `PredictionCallback` and `PredictionContext`.

Callback contract:

- `PredictionContext` exposes the method name, argument pattern, calls, matching calls, and call count.
- Callback failures are assertion failures in prediction checking.
- Failure diagnostics include recorded and matching call context where available.

## Overloads, generics, primitives, varargs, and defaults

Java adaptation rules:

- Generated wrappers preserve overloads where Java can call them unambiguously.
- Same-name token overloads are added for matcher ergonomics, avoiding duplicate all-`Object` and zero-arg duplicates.
- Primitive arguments use boxed token/default handling at the wrapper boundary.
- Generic signatures are generated from erased Java reflection/source signatures where necessary.
- Static/private/Object methods are not prophecy interaction methods.
- Default interface methods can be called by the underlying proxy when not explicitly stubbed/predicted.

## Diagnostics

Diagnostics must distinguish:

- unexpected call;
- unmet prediction;
- argument mismatch;
- unsupported collaborator injection;
- duplicate ambiguous collaborator parameters;
- wrapper generation ambiguity;
- lifecycle teardown failure after prediction failure.

## Verification

Primary automated evidence:

- `SpecRunnerTest` collaborator injection and lifecycle/prediction precedence tests.
- `MethodProphecyTest` prediction, callback, argument-token, and diagnostics tests.
- `DoublesTest` argument matcher/token and double behavior tests.
- `ProphecySkeletonGeneratorTest` generated wrapper overload/token tests.
- `examples/prophecy-basic` verified by `scripts/verify-examples.sh` and `scripts/verify-all.sh`.
