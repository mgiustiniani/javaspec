# Piano integrato: Refactoring → Prophecy-style doubles API

## Obiettivo

Integrare la prophecy-style doubles API (§1–28 del documento Prophecy) con il refactoring in corso
di `Main.java` e `ObjectBehavior.java`, in modo che ogni passo preparí la strada al successivo.

---

## Fase A — Completamento refactoring corrente (ora)

### A1: ObjectBehavior — SubjectTypeMarkers

Estrarre i marker di discovery (`shouldBeAClass`, `shouldBeAFinalClass`, …, `shouldPermit`)
in `SubjectTypeMarkers.java`.

**Perché serve alla prophecy**: libera `ObjectBehavior` da codice che non serve alla prophecy API.

**Dettaglio**: 11 metodi marker + `shouldHaveType(Class<?>)` (quello con lifecycle).  
`shouldHaveType(Class<?>)` può rimanere in `ObjectBehavior` perché usa `lifecycle`.

### A2: Main — Spostare describeClass() in DescribeCommandHandler

Attualmente `DescribeCommandHandler.execute()` delega a `Main.describeClass()`.
Spostare la logica dentro `DescribeCommandHandler`.

**Perché serve alla prophecy**: il comando `javaspec prophesize` (M8) sarà simile a `describe`.
Avere `DescribeCommandHandler` autonomo fa da template.

### A3: Main — Spostare runSpecifications() in RunCommandHandler

Attualmente `RunCommandHandler.execute()` delega a `Main.runSpecifications()`.
Spostare la logica dentro `RunCommandHandler`.

**Perché serve alla prophecy**: l'integrazione `run --generate` prophecy wrappers (M9)
modificherà il flusso `run`. Con la logica già in `RunCommandHandler`, la modifica è localizzata.

### A4: Main — Estrarre UsagePrinter

`printUsageError()` e `printUsage()` → `UsagePrinter.java`.

**Perché serve alla prophecy**: il nuovo comando `prophesize` avrà bisogno di help e error printing.
`UsagePrinter` condiviso evita duplicazione.

### A5: Main — Estrarre ConfigurationOrchestrator

`applyConfiguration()` → `ConfigurationOrchestrator.java`.

**Perché serve alla prophecy**: il comando `prophesize` caricherà configurazione (suite, naming).
Avere un orchestratore già isolato evita duplicazione.

### A6: Main — Estrarre ProfileEnforcementOrchestrator

`enforceProfileCompatibility()`, `relatedTypesOf()`, `add*RelatedTypes()`, `promptTarget()`,
`ProfileEnforcementFinding` → `ProfileEnforcementOrchestrator.java`.

**Perché serve alla prophecy**: indipendente, ma libera Main.

### A7: Main — Estrarre RunDiagnosticsPrinter

`printRunnerSummary()`, `printExecutionDiagnostics()`, `printRunConfiguration()` →
`RunDiagnosticsPrinter.java`.

**Perché serve alla prophecy**: il runner prophecy avrà output diagnostici simili.

### A8: Main — Estrarre FormatterHelper

`formatterFromConfiguration()`, `normalizeFormatter()`, `validateEffectiveFormatter()`,
`selectedFormatterDisplay()`, `joinNames()`, `validFormatterNames()` → `FormatterHelper.java`.

### A9: Main — Estrarre ConfigurationHelper

`bootstrapHooksFor()`, `extensionsFor()`, `resolveConstructorPolicy()`, `messageOf()`,
`displayPrefix()`, `policyOptionName()` → `ConfigurationHelper.java`.

---

## Fase B — Prophecy API core (dopo A1–A9)

### B1: Package prophecy + classi base

`src/main/java/org/javaspec/doubles/prophecy/` con:

| Classe | Dipende da | Note |
|---|---|---|
| `ObjectProphecy<T>` | — | Interfaccia, opzionale nel MVP |
| `BaseObjectProphecy<T>` | `Doubles`, `InterfaceDouble`, `DoubleControl` | Implementazione concreta |
| `MethodProphecy<R>` | — | Interfaccia fluente |
| `DefaultMethodProphecy<R>` | `DoubleControl.when()`, `PredictionRegistry` | Implementazione |
| `Promise<R>` | — | Interfaccia funzionale per callback |
| `Prediction` | — | Record/class per prediction singola |
| `PredictionMode` | — | Enum: `AT_LEAST_ONCE`, `NEVER`, `EXACTLY` |
| `PredictionRegistry` | `DoubleControl` | Registra e verifica prediction |
| `PredictionFailure` | — | Errore di prediction |

**Test**: `BaseObjectProphecyTest`, `DefaultMethodProphecyTest`, `PredictionRegistryTest`.

### B2: willReturn / willThrow / will

Mapping su `DoubleControl` esistente:

| Metodo prophecy | Mapping DoubleControl |
|---|---|
| `willReturn(R)` | `control.when(name, args).thenReturn(value)` |
| `willReturn(R first, R... next)` | Sequenza: `thenReturn(first)` poi `thenReturn(next[i])` per chiamate successive |
| `willThrow(Throwable)` | `control.when(name, args).thenThrow(error)` |
| `will(Promise<R>)` | `control.when(name, args).thenAnswer(args -> promise.execute(args))` |

**Test**: copertura §22 (willReturn, willReturn sequence, willThrow, will callback).

### B3: Prediction registry

`PredictionRegistry` accumula prediction durante l'example.
`checkPredictions()` le verifica tutte.

**Test**: copertura §22 (shouldBeCalled, shouldNotBeCalled, shouldBeCalledTimes).

### B4: shouldHaveBeenCalled (spy immediata)

Diversa dalla prediction: verifica subito invece di registrare per verifica futura.
Delega a `DoubleControl.verifyCalled(name, args)`.

### B5: ObjectBehavior integration

Aggiungere a `ObjectBehavior`:

```java
protected <T> ObjectProphecy<T> prophesize(Class<T> type)      // API generica
protected <P extends ObjectProphecy<?>> P prophecy(Class<P> prophecyType) // wrapper tipizzato
protected void checkPredictions()
```

Il registry delle prophecy può essere un field `PredictionRegistry` in `ObjectBehavior`,
resettato prima di ogni example dal runner.

**Test**: prophecy integration tests.

### B6: Argument / Arg

`Argument.java` e `Arg.java` come wrapper fluente di `ArgumentMatchers`.

Aggiungere a `ArgumentMatchers`:
- `containingString(String)` — manca
- `noop()` o `any()` già esiste

`Argument.containingString("SMTP")` → `ArgumentMatchers.containingString("SMTP")`.

**Test**: copertura §22 (Argument.any, Argument.type, Argument.eq, Argument.same,
Argument.that, Argument.containingString, mixed matchers).

---

## Fase C — Generatore wrapper prophecy (dopo B1–B6)

### C1: Generatore base

Analizzatore reflection che produce:
- Classe `*Prophecy extends BaseObjectProphecy<T>`
- Metodo per ogni metodo pubblico/intercettabile
- Mapping tipi: `int→Integer`, `boolean→Boolean`, `void→Void`
- Supporto overload (parameterTypes nel metodo generato)
- Supporto varargs
- Esclusione: `final`, `static`, `private`, `Object` methods

**Output**: file `.java` nel package `spec.<original.pkg>.prophecy.*Prophecy`.

### C2: CLI prophesize

Comando `javaspec prophesize <fqcn>` che genera il wrapper prophecy.

Flag:
- `--output <dir>` — directory output (default: spec root)
- `--package <pkg>` — package override (default: derivato)
- `--overwrite` — forza sovrascrittura
- `--dry-run` — mostra preview senza scrivere

### C3: run --generate prophecy

Durante `javaspec run --generate`, se lo spec usa `Prophecy` e il wrapper manca,
proporre generazione.

---

## Fase D — Auto-check predictions (dopo C1–C3)

### D1: Auto-check MVP

Il runner, dopo ogni example, chiama `checkPredictions()` automaticamente se
il prediction registry non è vuoto.

Configurabile:
- `ObjectBehavior.autoCheckPredictions(boolean)` — per-instance
- `--auto-check-predictions` / `javaspec.autoCheckPredictions` — globale

Default: `false` (solo esplicito).

### D2: Auto-check default (futuro)

Dopo validazione, cambiare default a `true`.

---

## Fase E — Documentation (dopo D1–D2)

### E1: README prophecy section

### E2: Examples prophecy

### E3: Migration guide (API esistente → prophecy)

### E4: ByteBuddy note

---

## Mappa dipendenze

```
A1 (SubjectTypeMarkers)
  └── B5 (ObjectBehavior integration) — libera ObjectBehavior per prophecy methods

A2 (DescribeCommandHandler autonomo)
  └── C2 (CLI prophesize) — template per nuovo comando

A3 (RunCommandHandler autonomo)
  └── C3 (run --generate prophecy) — flusso run modificabile localmente

A4 (UsagePrinter)
  └── C2 (CLI prophesize) — help/error printing condiviso

A5 (ConfigurationOrchestrator)
  └── C2 (CLI prophesize) — caricamento configurazione

A6 (ProfileEnforcementOrchestrator) — indipendente

A7 (RunDiagnosticsPrinter) — indipendente

A8 (FormatterHelper) — indipendente

A9 (ConfigurationHelper) — indipendente

B1–B6 (Prophecy API core)
  ├── C1 (Generatore wrapper)
  ├── C2 (CLI prophesize)
  ├── C3 (run --generate)
  └── D1–D2 (Auto-check)

C1–C3 (Generatore + CLI)
  └── D1–D2 (Auto-check) — prediction checking nel runner
```

---

## Calcolo righe finali stimate

### Dopo Fase A (refactoring completo)

| Classe | Prima | Dopo | Δ |
|---|---|---|---|
| `Main.java` | 772 | ~120 | −652 |
| `ObjectBehavior.java` | 614 | ~450 | −164 |
| Nuove classi A1–A9 | — | ~650 | +650 |

### Dopo Fase B–E (prophacy API completa)

| Nuova classe | Righe stimate |
|---|---|
| `BaseObjectProphecy` | ~100 |
| `DefaultMethodProphecy` | ~120 |
| `PredictionRegistry` | ~80 |
| `Prediction` / `PredictionMode` / `PredictionFailure` | ~30 |
| `Argument` / `Arg` | ~80 |
| `ObjectBehavior` aggiunte (B5) | ~30 |
| Generatore wrapper (C1) | ~200 |
| CLI handler (C2) | ~100 |
| Generatore integration (C3) | ~80 |
| Auto-check (D1) | ~40 |
| **Totale prophecy** | **~860** |

---

## Ordine di esecuzione consigliato

```
Settimana 1:   A1 → A2 → A3 (refactoring rapido)
Settimana 2:   A4 → A5 → A6 → A7 → A8 → A9 (refactoring isolato)
Settimana 3:   B1 → B2 → B3 (Prophecy API + predictions)
Settimana 4:   B4 → B5 → B6 (ObjectBehavior integration + Argument)
Settimana 5:   C1 → C2 (Generatore + CLI)
Settimana 6:   C3 → D1 → D2 (run integration + auto-check)
Settimana 7:   E1 → E2 → E3 → E4 (documentation)
```

---

## Cose da NON fare

- Non modificare `DoubleControl`, `InterfaceDouble`, `Doubles`, `ArgumentMatchers` in modo
  incompatibile. La prophecy API deve usarli come sono.
- Non introdurre dipendenze terze nel core.
- Non spostare la prophecy API nel modulo ByteBuddy.
- Non eliminare l'API doubles esistente (`interfaceDouble`, `DoubleControl`, ecc.).
- Non implementare M8 (CLI prophesize) prima che A2–A5 siano completati.
- Non implementare C3 (run --generate prophecy) prima che A3 sia completato.
