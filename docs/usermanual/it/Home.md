# Manuale utente javaspec

[English](../Home.md) · **Italiano** · [Español](../es/Home.md) · [Deutsch](../de/Home.md) ·
[Français](../fr/Home.md) · [简体中文](../ch/Home.md)

Questa è l'edizione italiana concisa del manuale javaspec 1.0. Il
[manuale inglese](../Home.md) resta il riferimento tecnico completo.

## Stato della release

- `1.0.0-RC5` è disponibile su Maven Central nel gruppo `io.github.jvmspec`.
- Core, plugin Maven, motore JUnit Platform, bytecode doubles e bytecode agent hanno artefatti
  principali, source e Javadoc firmati.
- L'id Gradle è `io.github.jvmspec`, ma la prima pubblicazione è ancora in approvazione. Finché il
  marker RC5 restituisce HTTP 404, non dichiarare disponibile il plugin dal Portal: usare l'included
  build dell'esempio o una pubblicazione locale.
- La correzione post-RC5 della riproducibilità Javadoc è su `develop`; sarà inclusa nel prossimo
  candidato stabile.

## Requisiti e installazione

Il core è compatibile con Java 8 e non ha dipendenze runtime di terze parti. Aggiungerlo come
dipendenza di test Maven:

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>1.0.0-RC5</version>
  <scope>test</scope>
</dependency>
```

Da un checkout del progetto:

```sh
mvn -q -DskipTests install
bin/javaspec --launcher-fingerprint
bin/javaspec --help
```

`--launcher-fingerprint` mostra versione, JAR selezionato e SHA-256. Usarlo prima di raccogliere
evidenza riproducibile o lavorare con un agente.

## Prima specifica

Creare soltanto lo scheletro della specifica e del supporto generato:

```sh
bin/javaspec describe com.example.PriceCalculator
```

Aggiungere un solo comportamento a `PriceCalculatorSpec`:

```java
public void it_calculates_the_total() {
    match(subject().calculate(2, 3)).shouldReturn(5);
}
```

Pianificare la generazione senza modificare il workspace:

```sh
bin/javaspec run --dry-run \
  --generation-report target/javaspec-generation.json
```

Applicare la sola impalcatura autorizzata, compilare ed eseguire:

```sh
bin/javaspec run --generate --compile --formatter pretty \
  --generation-report target/javaspec-generation.json
```

`describe` non crea mai codice di produzione. I corpi generati contengono
`// javaspec:stub`: una run compilata resta BROKEN finché ogni stub richiesto non è sostituito dal
comportamento reale.

## Esecuzione mirata e report

```sh
mkdir -p target/javaspec
bin/javaspec run --compile \
  --class PriceCalculator \
  --example it_calculates_the_total \
  --report target/javaspec/run-report.json \
  --junit-xml target/javaspec/junit.xml
```

Le directory parent dei report devono esistere. Il report JSON usa `schemaVersion: 1`; il report di
generazione è deterministico e distingue azioni `PROPOSED` e `APPLIED` tramite `appliedWrites`.

Comandi aggiuntivi:

```sh
bin/javaspec list-extensions
bin/javaspec prophesize com.example.Mailer --output target/generated-sources/javaspec
```

## Configurazione minima

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

Il formato è lineare e ristretto: `=` o `:`, commenti con `#`, nessun parser YAML/JSON/TOML e
nessuna dipendenza runtime aggiuntiva. I profili supportati sono `java8`, `java11`, `java17`,
`java21` e `java25`.

## Adattatori opzionali

- `javaspec-maven-plugin`: goal `javaspec:generate` e `javaspec:run`.
- `javaspec-gradle-plugin`: task `javaspecRun`; usare localmente finché il marker Portal non risolve.
- `javaspec-junit-platform-engine`: engine id `javaspec`, utile per IDE e CI.
- `javaspec-bytecode-doubles`: classi concrete non final.
- `javaspec-bytecode-agent`: classi final e metodi statici con strumentazione JVM esplicita.

Tutti gli adattatori sono standalone: il reactor Maven root verifica intenzionalmente solo il core.
Per la verifica aggregata:

```sh
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

## Anteprima eseguibile nativo (`develop`)

Dopo RC5, `javaspec:native-prepare` può generare launcher e metadati GraalVM per collegare core,
classi di produzione e spec compilate in un eseguibile specifico del progetto. Richiede GraalVM
Native Image 25; RC5 pubblicato non contiene ancora questa funzionalità.

```sh
scripts/verify-native-example.sh
```

Il primo scope Linux x86-64 copre lifecycle, costruttori, matcher integrati, skipped/pending e output
pretty/progress/JSON. Discovery, compilazione/generazione runtime, classpath dinamico, report su file,
doubles dinamici, agent, Gradle/JUnit e altre piattaforme sono differiti. Vedere la
[guida Native Image](../../native-image.md).

## Sviluppo guidato da specifiche

L'[agente di esempio javaspec](../../agent/javaspec-guided-development-assistant.md) applica una sola
slice semanticamente atomica: ammissione del comportamento, baseline, RED significativo, generazione
sicura opzionale, GREEN minimo, refactor focalizzato e handoff strutturato. Non modifica mai i file
`*SpecSupport` o `*Prophecy` generati a mano.

## Sicurezza e codici di uscita

- `0`: successo, nessuna spec, oppure soli esempi skipped/pending.
- `1`: esempi failed/broken, generazione rifiutata o pendente, compilazione fallita.
- `64`: uso, profilo, compilatore, bootstrap o compatibilità non validi.
- `70`: errore I/O o sicurezza del filesystem.

La generazione è fail-closed, richiede autorizzazione esplicita ed effettua scritture atomiche quando
il filesystem lo consente. Evidenza di tipo o sorgente ambigua viene rifiutata prima delle scritture.

## Approfondimenti

- [Manuale tecnico completo](../Home.md)
- [Pagina man italiana](../../man/it/man1/javaspec.1)
- [Risoluzione problemi](../../troubleshooting.md)
- [Contratto di generazione](../../generation-contract-1.0.md)
- [Contratto dei risultati](../../result-contract-1.0.md)
- [Indice documentazione](../../README.md)
