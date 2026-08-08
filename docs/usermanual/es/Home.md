# Manual de usuario de javaspec

[English](../Home.md) · [Italiano](../it/Home.md) · **Español** · [Deutsch](../de/Home.md) ·
[Français](../fr/Home.md) · [简体中文](../ch/Home.md)

Esta es la edición española resumida del manual de javaspec 1.0. El
[manual inglés](../Home.md) sigue siendo la referencia técnica completa.

## Estado de la versión

- `1.0.0-RC5` está disponible en Maven Central bajo el grupo `io.github.jvmspec`.
- El core, el plugin Maven, el motor JUnit Platform, bytecode doubles y bytecode agent tienen JAR
  principal, de fuentes y Javadoc firmados.
- El id Gradle es `io.github.jvmspec`, pero la primera publicación continúa en revisión. Mientras el
  marcador RC5 responda HTTP 404, no se debe anunciar el plugin como disponible en el Portal; use el
  included build del ejemplo o una publicación local.
- La corrección de reproducibilidad Javadoc posterior a RC5 está en `develop` y se incluirá en el
  próximo candidato estable.

## Requisitos e instalación

El core es compatible con Java 8 y no tiene dependencias de ejecución de terceros:

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>1.0.0-RC5</version>
  <scope>test</scope>
</dependency>
```

Desde un checkout del proyecto:

```sh
mvn -q -DskipTests install
bin/javaspec --launcher-fingerprint
bin/javaspec --help
```

`--launcher-fingerprint` muestra la versión, el JAR seleccionado y su SHA-256. Ejecútelo antes de
recoger evidencia reproducible o de iniciar trabajo automatizado.

## Primera especificación

Cree solamente los esqueletos de especificación y soporte:

```sh
bin/javaspec describe com.example.PriceCalculator
```

Añada un único comportamiento a `PriceCalculatorSpec`:

```java
public void it_calculates_the_total() {
    match(subject().calculate(2, 3)).shouldReturn(5);
}
```

Planifique la generación sin modificar el workspace:

```sh
bin/javaspec run --dry-run \
  --generation-report target/javaspec-generation.json
```

Aplique el andamiaje autorizado, compile y ejecute:

```sh
bin/javaspec run --generate --compile --formatter pretty \
  --generation-report target/javaspec-generation.json
```

`describe` nunca crea código de producción. Los cuerpos generados contienen
`// javaspec:stub`: una ejecución compilada permanece BROKEN hasta sustituir cada stub necesario por
comportamiento real.

## Ejecución selectiva e informes

```sh
mkdir -p target/javaspec
bin/javaspec run --compile \
  --class PriceCalculator \
  --example it_calculates_the_total \
  --report target/javaspec/run-report.json \
  --junit-xml target/javaspec/junit.xml
```

Los directorios padre de los informes deben existir. El JSON usa `schemaVersion: 1`; el informe de
generación es determinista y diferencia acciones `PROPOSED` y `APPLIED` mediante `appliedWrites`.

Otros comandos:

```sh
bin/javaspec list-extensions
bin/javaspec prophesize com.example.Mailer --output target/generated-sources/javaspec
```

## Configuración mínima

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

El formato es deliberadamente lineal: `=` o `:`, comentarios con `#`, sin parser YAML/JSON/TOML ni
nuevas dependencias runtime. Los perfiles admitidos son `java8`, `java11`, `java17`, `java21` y
`java25`.

## Adaptadores opcionales

- `javaspec-maven-plugin`: goals `javaspec:generate` y `javaspec:run`.
- `javaspec-gradle-plugin`: tarea `javaspecRun`; úsela localmente hasta que resuelva el marcador.
- `javaspec-junit-platform-engine`: engine id `javaspec` para IDE y CI.
- `javaspec-bytecode-doubles`: clases concretas no finales.
- `javaspec-bytecode-agent`: clases finales y métodos estáticos con instrumentación JVM explícita.

Los adaptadores son proyectos independientes; el reactor Maven raíz verifica únicamente el core.
Verificación agregada:

```sh
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

## Vista previa del ejecutable nativo (`develop`)

Después de RC5, `javaspec:native-prepare` puede generar el lanzador y los metadatos de GraalVM para
enlazar el núcleo, las clases de producción y las especificaciones compiladas en un ejecutable
específico del proyecto. Requiere GraalVM Native Image 25; la RC5 publicada aún no lo incluye.

```sh
scripts/verify-native-example.sh
```

El primer alcance Linux x86-64 cubre ciclo de vida, constructores, matchers integrados,
skipped/pending y salida pretty/progress/JSON. Se aplazan descubrimiento, compilación/generación en
tiempo de ejecución, classpath dinámico, informes en archivos, dobles dinámicos, agente,
Gradle/JUnit y otras plataformas. Consulte la [guía Native Image](../../native-image.md).

## Desarrollo guiado por especificaciones

El [agente javaspec de ejemplo](../../agent/javaspec-guided-development-assistant.md) implementa una
sola porción semánticamente atómica: admisión, baseline, RED significativo, generación segura
opcional, GREEN mínimo, refactor focalizado y handoff estructurado. Nunca edita manualmente fuentes
`*SpecSupport` o `*Prophecy` generadas.

## Seguridad y códigos de salida

- `0`: éxito, ninguna spec o solamente ejemplos skipped/pending.
- `1`: ejemplos failed/broken, generación rechazada o pendiente, o compilación fallida.
- `64`: error de uso, perfil, compilador, bootstrap o compatibilidad.
- `70`: error de E/S o seguridad del sistema de archivos.

La generación falla de forma segura, requiere autorización explícita y usa escrituras atómicas
cuando el sistema de archivos lo permite. La evidencia ambigua de fuentes o tipos se rechaza antes
de escribir.

## Más información

- [Manual técnico completo](../Home.md)
- [Página man española](../../man/es/man1/javaspec.1)
- [Solución de problemas](../../troubleshooting.md)
- [Contrato de generación](../../generation-contract-1.0.md)
- [Contrato de resultados](../../result-contract-1.0.md)
- [Índice de documentación](../../README.md)
