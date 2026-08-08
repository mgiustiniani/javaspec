# javaspec-Benutzerhandbuch

[English](../Home.md) · [Italiano](../it/Home.md) · [Español](../es/Home.md) · **Deutsch** ·
[Français](../fr/Home.md) · [简体中文](../ch/Home.md)

Dies ist die kompakte deutsche Ausgabe des javaspec-1.0-Handbuchs. Das
[englische Handbuch](../Home.md) bleibt die vollständige technische Referenz.

## Release-Status

- `1.0.0-RC5` ist in Maven Central unter der Gruppe `io.github.jvmspec` verfügbar.
- Core, Maven-Plugin, JUnit-Platform-Engine, Bytecode-Doubles und Bytecode-Agent besitzen signierte
  Haupt-, Source- und Javadoc-Artefakte.
- Die Gradle-Plugin-ID lautet `io.github.jvmspec`; die erste Veröffentlichung wird jedoch noch
  geprüft. Solange der RC5-Marker HTTP 404 liefert, darf das Plugin nicht als im Portal verfügbar
  bezeichnet werden. Verwenden Sie den Included Build des Beispiels oder eine lokale Publikation.
- Die nach RC5 erstellte Korrektur der Javadoc-Reproduzierbarkeit liegt auf `develop` und wird in den
  nächsten stabilen Kandidaten übernommen.

## Voraussetzungen und Installation

Der Core läuft ab Java 8 und besitzt keine Laufzeitabhängigkeiten von Drittanbietern:

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>1.0.0-RC5</version>
  <scope>test</scope>
</dependency>
```

Aus einem Projekt-Checkout:

```sh
mvn -q -DskipTests install
bin/javaspec --launcher-fingerprint
bin/javaspec --help
```

`--launcher-fingerprint` zeigt Version, ausgewähltes JAR und SHA-256. Führen Sie den Befehl vor
reproduzierbarer Beweiserfassung oder automatisierter Agentenarbeit aus.

## Erste Spezifikation

Erzeugen Sie nur das Spezifikations- und Support-Gerüst:

```sh
bin/javaspec describe com.example.PriceCalculator
```

Fügen Sie in `PriceCalculatorSpec` genau ein Verhalten hinzu:

```java
public void it_calculates_the_total() {
    match(subject().calculate(2, 3)).shouldReturn(5);
}
```

Planen Sie die Generierung ohne Änderungen am Workspace:

```sh
bin/javaspec run --dry-run \
  --generation-report target/javaspec-generation.json
```

Wenden Sie das autorisierte Gerüst an, kompilieren und starten Sie es:

```sh
bin/javaspec run --generate --compile --formatter pretty \
  --generation-report target/javaspec-generation.json
```

`describe` erzeugt niemals Produktionscode. Generierte Methodenkörper enthalten
`// javaspec:stub`; ein kompilierter Lauf bleibt BROKEN, bis jeder benötigte Stub durch echtes
Domänenverhalten ersetzt wurde.

## Gezielte Ausführung und Berichte

```sh
mkdir -p target/javaspec
bin/javaspec run --compile \
  --class PriceCalculator \
  --example it_calculates_the_total \
  --report target/javaspec/run-report.json \
  --junit-xml target/javaspec/junit.xml
```

Übergeordnete Berichtverzeichnisse müssen vorhanden sein. JSON-Berichte verwenden
`schemaVersion: 1`; der deterministische Generierungsbericht unterscheidet `PROPOSED` und `APPLIED`
und zählt tatsächliche Änderungen in `appliedWrites`.

Weitere Befehle:

```sh
bin/javaspec list-extensions
bin/javaspec prophesize com.example.Mailer --output target/generated-sources/javaspec
```

## Minimale Konfiguration

```properties
profile = java17
formatter = progress
defaultSuite = domain
constructorPolicy = preserve

suite.domain.specDir = src/test/java
suite.domain.sourceDir = src/main/java
suite.domain.specPackagePrefix = spec
suite.domain.packagePrefix = com.example
```

Das Format ist absichtlich zeilenorientiert: `=` oder `:`, Kommentare mit `#`, kein
YAML-/JSON-/TOML-Parser und keine zusätzliche Laufzeitabhängigkeit. Unterstützte Profile sind
`java8`, `java11`, `java17`, `java21` und `java25`.

## Optionale Adapter

- `javaspec-maven-plugin`: Goals `javaspec:generate` und `javaspec:run`.
- `javaspec-gradle-plugin`: Task `javaspecRun`; lokal verwenden, bis der Portal-Marker auflösbar ist.
- `javaspec-junit-platform-engine`: Engine-ID `javaspec` für IDE und CI.
- `javaspec-bytecode-doubles`: nicht-finale konkrete Klassen.
- `javaspec-bytecode-agent`: finale Klassen und statische Methoden mit expliziter JVM-Instrumentierung.

Alle Adapter sind eigenständig; der Root-Maven-Reaktor prüft bewusst nur den Core. Aggregierte
Prüfung:

```sh
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

## Spezifikationsgetriebene Entwicklung

Der [javaspec-Beispielagent](../../agent/javaspec-guided-development-assistant.md) bearbeitet genau
einen semantisch atomaren Slice: Zulassung, Baseline, aussagekräftiges RED, optionale sichere
Generierung, kleinstes kohärentes GREEN, fokussiertes Refactoring und strukturiertes Handoff.
Generierte `*SpecSupport`- oder `*Prophecy`-Dateien werden nie manuell bearbeitet.

## Sicherheit und Exit-Codes

- `0`: Erfolg, keine Specs oder ausschließlich skipped/pending Beispiele.
- `1`: failed/broken Beispiele, abgelehnte/offene Generierung oder Kompilierungsfehler.
- `64`: Nutzungs-, Profil-, Compiler-, Bootstrap- oder Kompatibilitätsfehler.
- `70`: E/A- oder Dateisystem-Sicherheitsfehler.

Generierung arbeitet fail-closed, benötigt ausdrückliche Autorisierung und verwendet atomare
Schreibvorgänge, soweit das Dateisystem sie unterstützt. Mehrdeutige Quell- oder Typinformationen
werden vor dem Schreiben abgelehnt.

## Weiterführende Dokumentation

- [Vollständiges technisches Handbuch](../Home.md)
- [Deutsche Manpage](../../man/de/man1/javaspec.1)
- [Fehlerbehebung](../../troubleshooting.md)
- [Generierungsvertrag](../../generation-contract-1.0.md)
- [Ergebnisvertrag](../../result-contract-1.0.md)
- [Dokumentationsindex](../../README.md)
