# Changelog — the-tync

All notable changes to `the-tync` are documented here.

## [Unreleased]

### Added
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
- Maven group identifiers moved to `com.ld`; all package
  roots standardized under `com.ld.*`.
- Project naming standardized under LD (Lisa's Dungeon).
- Deeplearning4j, ND4J, and OpenNLP dependencies removed from the parent POM
  (proprietary ML code is not part of this distribution).

### Removed
- Proprietary engine implementations, model archives, and training code
  (excluded from the public distribution).
- Bundled JDK and Maven distributions, build outputs, logs, and assistant
  artifacts (not part of the source distribution).
