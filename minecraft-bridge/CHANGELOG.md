# Changelog — minecraft-bridge

All notable changes to `minecraft-bridge` are documented here.

## [Unreleased]

### Added
- Initial public source distribution: Tync connection layer, server
  detection/launch, runtime mod injection, and CLI interface.
- Per-file syntax verification and package.json validation.

### Changed
- Package name standardized to `ld-minecraft-bridge`;
  binary name standardized to `ld-bridge`.
- Environment variables standardized to `LD_*`
  (`LD_TYNC_PATH`, `LD_TYNC_BASE_URL`, `LD_RECEIVE_URL`, `LD_RECEIVE_JAR`).
- License changed to GPL-3.0-only (repository root LICENSE).
- Contact and support links moved to Lisa's Dungeon channels
  (github.com/lisasdungeon, Discord MystryssLysa).

### Removed
- Pre-built executables and the bundled injection agent binary
  (distributed via releases, not source control).
- Prior branding and fundraising references.
