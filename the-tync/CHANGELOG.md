# Changelog — the-tync

All notable changes to `the-tync` are documented here.

## [Unreleased]

### Added
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
