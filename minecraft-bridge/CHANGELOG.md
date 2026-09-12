# Changelog — minecraft-bridge

All notable changes to `minecraft-bridge` are documented here.

## [Unreleased]

### Added
- Initial public source distribution: Tync connection layer, server
  detection/launch, runtime mod injection, and CLI interface.
- Per-file syntax verification and package.json validation.

### Changed
- README rewritten around the cross-loader proof pipeline (receive → adapt →
  verify → deliver): removed claims for features that do not exist in this tier
  (live injection, prebuilt downloads, universal loader targets) and documented
  the real CLI surface (`--receive-jar` / `--receive-url`), all `RNK_*` env vars,
  and the test/verification commands.
- `engines.node` raised from `>=16` to `>=18` (global `fetch` requires 18; CI
  tests Node 20/22).
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
