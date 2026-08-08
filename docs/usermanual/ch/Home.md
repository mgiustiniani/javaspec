# javaspec 用户手册

[English](../Home.md) · [Italiano](../it/Home.md) · [Español](../es/Home.md) · [Deutsch](../de/Home.md) ·
[Français](../fr/Home.md) · **简体中文**

本页是 javaspec 1.0 用户手册的简体中文版摘要。完整技术细节以
[英文手册](../Home.md)为准。

## 发布状态

- `1.0.0-RC5` 已在 Maven Central 发布，group 为 `io.github.jvmspec`。
- core、Maven 插件、JUnit Platform 引擎、bytecode doubles 和 bytecode agent 均提供已签名的主 JAR、
  source JAR 与 Javadoc JAR。
- Gradle 插件 id 为 `io.github.jvmspec`，但首次发布仍在审批中。只要 RC5 marker 仍返回 HTTP 404，
  就不能宣称该插件已在 Portal 可用；请使用示例中的 included build 或本地发布。
- RC5 之后的 Javadoc 可复现性修复已进入 `develop`，并将包含在下一个稳定候选版本中。

## 环境要求与安装

core 兼容 Java 8，且没有第三方运行时依赖：

```xml
<dependency>
  <groupId>io.github.jvmspec</groupId>
  <artifactId>javaspec</artifactId>
  <version>1.0.0-RC5</version>
  <scope>test</scope>
</dependency>
```

在项目 checkout 中执行：

```sh
mvn -q -DskipTests install
bin/javaspec --launcher-fingerprint
bin/javaspec --help
```

`--launcher-fingerprint` 会显示版本、实际选择的 JAR 路径和 SHA-256。在收集可复现证据或启动自动化代理之前，
应先记录该输出。

## 第一个规格

只创建规格与生成支持代码的骨架：

```sh
bin/javaspec describe com.example.PriceCalculator
```

在 `PriceCalculatorSpec` 中加入一个行为：

```java
public void it_calculates_the_total() {
    match(subject().calculate(2, 3)).shouldReturn(5);
}
```

先规划生成，不修改 workspace：

```sh
bin/javaspec run --dry-run \
  --generation-report target/javaspec-generation.json
```

确认计划后，应用机械骨架、编译并执行：

```sh
bin/javaspec run --generate --compile --formatter pretty \
  --generation-report target/javaspec-generation.json
```

`describe` 永远不会创建生产代码。生成的方法体包含 `// javaspec:stub`；在所有必要 stub 都被真实领域行为替换之前，
编译执行结果仍为 BROKEN。

## 定向执行与报告

```sh
mkdir -p target/javaspec
bin/javaspec run --compile \
  --class PriceCalculator \
  --example it_calculates_the_total \
  --report target/javaspec/run-report.json \
  --junit-xml target/javaspec/junit.xml
```

报告文件的父目录必须已经存在。JSON 报告使用 `schemaVersion: 1`；确定性的生成报告区分 `PROPOSED` 与
`APPLIED`，并通过 `appliedWrites` 记录实际发生内容变化的写入次数。

其他命令：

```sh
bin/javaspec list-extensions
bin/javaspec prophesize com.example.Mailer --output target/generated-sources/javaspec
```

## 最小配置

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

配置格式刻意保持为逐行键值：使用 `=` 或 `:`，以 `#` 开始注释，不引入 YAML/JSON/TOML 解析器，也不增加运行时依赖。
支持 `java8`、`java11`、`java17`、`java21` 和 `java25` profile。

## 可选适配器

- `javaspec-maven-plugin`：提供 `javaspec:generate` 和 `javaspec:run` goal。
- `javaspec-gradle-plugin`：提供 `javaspecRun` task；Portal marker 可解析之前请在本地使用。
- `javaspec-junit-platform-engine`：engine id 为 `javaspec`，用于 IDE 与 CI。
- `javaspec-bytecode-doubles`：支持非 final 的具体类。
- `javaspec-bytecode-agent`：通过显式 JVM instrumentation 支持 final 类与 static 方法。

所有适配器都是独立项目；根 Maven reactor 有意只验证 core。聚合验证命令：

```sh
scripts/verify-all.sh
scripts/verify-release-dry-run.sh
```

## 规格驱动开发

[javaspec 示例代理](../../agent/javaspec-guided-development-assistant.md)每次只实现一个语义原子行为：行为准入、
baseline、有效 RED、可选安全生成、最小一致 GREEN、聚焦重构和结构化 handoff。它不会手工修改生成的
`*SpecSupport` 或 `*Prophecy` 文件。

## 安全规则与退出码

- `0`：成功、未找到 spec，或只有 skipped/pending 示例。
- `1`：failed/broken 示例、拒绝或待处理的生成、编译失败。
- `64`：用法、profile、编译器、bootstrap 或兼容性错误。
- `70`：I/O 或文件系统安全错误。

生成过程采用 fail-closed 策略，需要显式授权，并在文件系统支持时使用原子写入。存在歧义的源代码或类型证据会在写入前被拒绝。

## 延伸阅读

- [完整英文技术手册](../Home.md)
- [简体中文 man 页面](../../man/ch/man1/javaspec.1)
- [故障排查](../../troubleshooting.md)
- [生成契约](../../generation-contract-1.0.md)
- [结果契约](../../result-contract-1.0.md)
- [文档索引](../../README.md)
