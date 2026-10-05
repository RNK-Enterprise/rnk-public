# Changelog — the-tync

All notable changes to `the-tync` are documented here.

## [Unreleased]

### Added
- main-app: turbo test coverage for all 94 turbos. A parametrized lifecycle
  suite (`TemplateTurboTest`, 182 dynamic tests) exercises all 91 template
  turbos end to end (uninitialized rejection, initialize, execute result
  contract, shutdown reset, acceleration factor), and dedicated suites cover
  the three context-dependent turbos (`BytecodeTurbo` cache hit/miss,
  `ShimTurbo` shim generation, `InjectionTurbo` pass-through). JaCoCo
  confirms 0 missed lines and branches across every file in the turbos
  package (314 tests total in main-app).
- main-app: unit test suites for the five infrastructure engines
  (`CachingPerformanceEngine`, `MonitoringMetricsEngine`, `SandboxingEngine`,
  `DataSerializationEngine`, `NetworkSynchronizationEngine`; 115 tests total
  in main-app). All five engines verified at 0 missed lines, branches, and
  methods in the module JaCoCo report.

### Changed
- main-app: `SandboxingEngine` 2.0 runs untrusted code in a separate, locked-down
  JVM instead of in-process: heap cap, wall-clock limit that kills the process
  tree, output cap, empty environment, throwaway working directory. On Linux
  with bubblewrap it adds OS isolation: no network, private PID/IPC/UTS
  namespaces, a read-only view of only the system libraries, JDK and classpath,
  and a size-capped scratch directory. Requests now name a classpath and main
  class instead of passing an in-process `Runnable`. 100% line and branch
  coverage (28 tests, including the OS-isolation guarantees).
- Credits: RNK-Enterprise is the sole contributor listed in the README and
  Maven metadata.
- the-tync is now generated from the private RNK source tree by its export
  step (proprietary engines become the stubs in this distribution; training
  code is left out), so this tree tracks the source of truth.
- Java 25 LTS: every module compiles for release 25 (ASM 9.10.1, Byte Buddy
  1.18.14, Mockito 5.24.0, Surefire 3.5.6, JaCoCo 0.8.15). Adapted artifacts
  still carry Java 21 class files for the target Minecraft server JVM.
- Dependencies: jackson-databind 2.18.10, commons-lang3 3.18.0,
  micrometer-core 1.14.14.
- `EngineHealth` and `TurboHealth` are enums; `Engine.getHealth()` and
  `Turbo.getHealth()` no longer have defaults (the stubs report `HEALTHY`).
- main-app tests: 314 -> 161, all covering code that ships (adds
  `EngineManagerTest` and `TurboManagerTest`; the template-turbo suite now
  runs over the 21 template turbos that remain).
- Updated the delivery architecture documentation to refer to The End War and
  retained Lisa's Dungeon as the sole Maven contributor entry.
- Reproducible builds enforced: the parent POM now sets
  `project.build.outputTimestamp` (Reproducible Builds convention).
  Back-to-back `clean package` runs verified byte-identical across all 59
  reactor JARs. (Earlier documentation claimed this property; it was not
  actually configured until now.)
- main-app: `SandboxingEngine` no longer installs a `SecurityManager`.
  The Security Manager API permanently throws on JDK 17+ (this project's
  target), so sandbox execution previously always failed; sandboxed work
  now runs under the engine's dedicated scheduler isolation.
- main-app: `NetworkSynchronizationEngine` reads response bodies directly.
  OkHttp guarantees a non-null body from `execute()`, so the null-body
  branches were unreachable dead code and have been removed.

### Fixed
- main-app: network and serialization engines now report `UNHEALTHY` health
  (and surface the underlying cause) when their initialize-time self-tests
  fail, instead of failing silently.
- main-app: contract suite for all eleven proprietary-engine stubs plus the
  `Engine` interface defaults (7 tests; stubs and interface at 0 missed
  lines, branches, methods).
- main-app: turbo infrastructure tests (`TurboContext`, `TurboMetrics`,
  `TurboResult`, `TurboHealth`, `TurboManager`, `Turbo` default methods) plus
  positive-branch health checks for both managers (24 tests; turbo infra and
  both managers at 0 missed lines, branches, methods).
- main-app: unit tests for engine value types (`EngineResult`,
  `EngineMetrics`, `EngineContext`, `EngineHealth`) and `EngineManager`
  (26 tests; all five files at 0 missed lines, branches, methods).
- forge-api-library: coverage for `getInterface` lookups (2 tests; module now
  0 missed lines, branches, methods).
- issue-pattern-library: coverage for invalid-regex and null-input paths of
  `IssuePattern.matches` (2 tests; module now 0 missed).
- fix-pattern-library: coverage for unknown-template path of `canAutoFix`
  (1 test; module now 0 missed).
- Unit test suites for the nine single-class loader libraries (magma,
  mohist, liteloader, quilt, sponge, cuberite, dragonproxy, patchwork,
  cardinal-components; 3 tests each). JaCoCo confirms 0 missed lines,
  branches, and methods per module.
- glowstone-api-library: unit tests for `GlowstoneApi` (3 tests; 100% line,
  branch, and method coverage verified in module JaCoCo report).
- common-api: contract test suite for `LoaderApi` (6 tests; module main
  sources are interface-only, coverage complete).
- Initial public source distribution: parent POM plus 19 loader API library
  modules and the `main-app` engine host.
- Engine infrastructure with full source: `EngineManager`, `ComponentLoader`,
  `TriggerManager`, `OptimizationOrchestrator`, `TyncCore`,
  `EngineRunner`/`SimpleEngineRunner`, `TurboManager` and 24 turbos.
- Bridge delivery pipeline (`bridge/ModDeliverySystem`) with full source.
- Compile-compatible stubs for 11 proprietary transformation engines
  (bytecode metamorphosis, shim generation, injection, API mapping, API
  bridge, multi-loader bridge, validation, pattern recognition, issue
  resolution, adaptive learning, predictive optimization). Stubs implement
  the `Engine` interface, log a warning, and return an unsupported
  `EngineResult`.
- JaCoCo coverage reporting wired into the parent build (report at
  `target/site/jacoco` after `mvn verify`).

### Changed
- Maven group identifiers moved to `com.rnk`; all package
  roots standardized under `com.rnk.*`.
- Project naming standardized under RNK Studios.
- Deeplearning4j, ND4J, and OpenNLP dependencies removed from the parent POM
  (proprietary ML code is not part of this distribution).

### Removed
- Unused code from the original snapshot that nothing in the shipped entry
  points needed: 70 template turbos, `ComponentLoader`, `TriggerManager`,
  `OptimizationOrchestrator`, `EngineRunner`, `bridge/ModDeliverySystem`, and
  the duplicate `engines.EngineManager` / `turbos.TurboManager` registries.
  `TheTync` now wires engines and turbos through `EngineManager` and
  `TurboManager` directly.
- Proprietary engine implementations, model archives, and training code
  (excluded from the public distribution).
- Bundled JDK and Maven distributions, build outputs, logs, and assistant
  artifacts (not part of the source distribution).
