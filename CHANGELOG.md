# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- RNK Studios Ecosystem public distribution: `the-tync` (Java 21 meta-loader engine) and
  `minecraft-bridge` (Node.js server bridge).
- GPL-3.0 LICENSE applying to all code in this repository.
- Proprietary transformation engines replaced with compile-compatible stubs
  (warn + unsupported result) so the framework builds and runs out of the box.
- Per-project CHANGELOG.md files (`the-tync/`, `minecraft-bridge/`).
- JaCoCo coverage reporting configured for `the-tync` (Bible-standard CI setup);
  test suite expansion tracked separately.

### Changed
- Full rebrand to RNK Studios: Maven coordinates `com.rnk`,
  package roots `com.rnk.*`, bridge package `rnk-minecraft-bridge`, env vars
  `RNK_*`, executable `rnk-bridge`, and all documentation.
- Copyright and attribution moved to RNK Studios.
- Environment and tooling URLs point to github.com/RNK-Enterprise.

### Removed
- All prior branding, fundraising links, and prior proprietary-license references
  from code, poms, and documentation.
- Proprietary engine source, trained models, training pipelines, bundled JDK
  and Maven distributions, build outputs, and AI-assistant artifacts (excluded
  from the public distribution).
