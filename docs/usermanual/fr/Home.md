# Manuel utilisateur de javaspec

[English](../Home.md) · [Italiano](../it/Home.md) · [Español](../es/Home.md) · [Deutsch](../de/Home.md) ·
**Français** · [简体中文](../ch/Home.md)

Cette page est l'édition française concise du manuel javaspec 1.0. Le
[manuel anglais](../Home.md) demeure la référence technique complète.

## État de la version

- `1.0.0-RC5` est disponible sur Maven Central sous le groupe `io.github.jvmspec`.
- Le core, le plugin Maven, le moteur JUnit Platform, bytecode doubles et bytecode agent disposent
  d'artefacts principaux, sources et Javadoc signés.
- L'identifiant Gradle est `io.github.jvmspec`, mais la première publication est toujours en cours
  d'approbation. Tant que le marqueur RC5 répond HTTP 404, ne présentez pas le plugin comme
  disponible sur le Portal ; utilisez l'included build de l'exemple ou une publication locale.
- Le correctif de reproductibilité Javadoc postérieur à RC5 est sur `develop` et sera intégré au
  prochain candidat stable.

## Prérequis et installation

Le core est compatible Java 8 et n'a aucune dépendance d'exécution tierce :

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>1.0.0-RC5</version>
  <scope>test</scope>
</dependency>
```

Depuis un checkout du projet :

```sh
mvn -q -DskipTests install
bin/javaspec --launcher-fingerprint
bin/javaspec --help
```

`--launcher-fingerprint` affiche la version, le JAR sélectionné et son SHA-256. Exécutez-le avant de
collecter des preuves reproductibles ou de lancer un travail automatisé.

## Première spécification

Créez uniquement les squelettes de spécification et de support :

```sh
bin/javaspec describe com.example.PriceCalculator
```

Ajoutez un seul comportement à `PriceCalculatorSpec` :

```java
public void it_calculates_the_total() {
    match(subject().calculate(2, 3)).shouldReturn(5);
}
```

Planifiez la génération sans modifier le workspace :

```sh
bin/javaspec run --dry-run \
  --generation-report target/javaspec-generation.json
```

Appliquez l'échafaudage autorisé, compilez et exécutez :

```sh
bin/javaspec run --generate --compile --formatter pretty \
  --generation-report target/javaspec-generation.json
```

`describe` ne crée jamais de code de production. Les corps générés contiennent
`// javaspec:stub` : une exécution compilée reste BROKEN jusqu'au remplacement de chaque stub requis
par le comportement réel.

## Exécution ciblée et rapports

```sh
mkdir -p target/javaspec
bin/javaspec run --compile \
  --class PriceCalculator \
  --example it_calculates_the_total \
  --report target/javaspec/run-report.json \
  --junit-xml target/javaspec/junit.xml
```

Les répertoires parents des rapports doivent exister. Le JSON utilise `schemaVersion: 1` ; le rapport
de génération est déterministe, distingue `PROPOSED` et `APPLIED` et compte les changements réels
dans `appliedWrites`.

Autres commandes :

```sh
bin/javaspec list-extensions
bin/javaspec prophesize com.example.Mailer --output target/generated-sources/javaspec
```

## Configuration minimale

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

Le format est volontairement linéaire : `=` ou `:`, commentaires avec `#`, aucun parseur
YAML/JSON/TOML et aucune dépendance runtime supplémentaire. Les profils disponibles sont `java8`,
`java11`, `java17`, `java21` et `java25`.

## Adaptateurs optionnels

- `javaspec-maven-plugin` : goals `javaspec:generate` et `javaspec:run`.
- `javaspec-gradle-plugin` : tâche `javaspecRun`, à utiliser localement avant disponibilité du marqueur.
- `javaspec-junit-platform-engine` : engine id `javaspec` pour les IDE et la CI.
- `javaspec-bytecode-doubles` : classes concrètes non finales.
- `javaspec-bytecode-agent` : classes finales et méthodes statiques avec instrumentation JVM explicite.

Tous les adaptateurs sont autonomes ; le réacteur Maven racine vérifie volontairement uniquement le
core. Vérification agrégée :

```sh
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

## Développement piloté par les spécifications

L'[agent javaspec d'exemple](../../agent/javaspec-guided-development-assistant.md) traite une seule
tranche sémantiquement atomique : admission, baseline, RED significatif, génération sûre optionnelle,
GREEN cohérent minimal, refactoring ciblé et handoff structuré. Il ne modifie jamais manuellement les
sources générées `*SpecSupport` ou `*Prophecy`.

## Sécurité et codes de sortie

- `0` : succès, aucune spec ou uniquement des exemples skipped/pending.
- `1` : exemples failed/broken, génération refusée ou en attente, ou compilation échouée.
- `64` : erreur d'utilisation, profil, compilateur, bootstrap ou compatibilité.
- `70` : erreur d'E/S ou de sécurité du système de fichiers.

La génération fonctionne en mode fail-closed, exige une autorisation explicite et utilise des
écritures atomiques lorsque le système de fichiers le permet. Les preuves de source ou de type
ambiguës sont refusées avant toute écriture.

## Pour aller plus loin

- [Manuel technique complet](../Home.md)
- [Page man française](../../man/fr/man1/javaspec.1)
- [Dépannage](../../troubleshooting.md)
- [Contrat de génération](../../generation-contract-1.0.md)
- [Contrat des résultats](../../result-contract-1.0.md)
- [Index de documentation](../../README.md)
