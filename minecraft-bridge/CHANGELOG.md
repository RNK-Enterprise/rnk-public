# Changelog — minecraft-bridge

All notable changes to `minecraft-bridge` are documented here.

## [Unreleased]

### Added
- Initial public source distribution: Tync connection layer, server
  detection/launch, runtime mod injection, and CLI interface.
- Per-file syntax verification and package.json validation.

### Changed
- Package name standardized to `rnk-minecraft-bridge`;
  binary name standardized to `rnk-bridge`.
- Environment variables standardized to `RNK_*`
  (`RNK_TYNC_PATH`, `RNK_TYNC_BASE_URL`, `RNK_RECEIVE_URL`, `RNK_RECEIVE_JAR`).
- License changed to GPL-3.0-only (repository root LICENSE).
- Contact and support links moved to RNK Studios channels
  (github.com/RNK-Enterprise).

### Removed
- Pre-built executables and the bundled injection agent binary
  (distributed via releases, not source control).
- Prior branding and fundraising references.
