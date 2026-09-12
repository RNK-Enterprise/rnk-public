# RNK Studios — Open Cross-Loader Compatibility for Minecraft

[![CI](https://github.com/RNK-Enterprise/rnk-public/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/RNK-Enterprise/rnk-public/actions/workflows/ci.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

**RNK adapts compiled Minecraft mods across loader boundaries.** The currently proven
open-source boundary transforms compatible **Fabric 1.20.x server mods into Paper
plugins** using real ASM bytecode transformation and generated Bukkit entrypoints —
with automated execution proof and live Paper-server validation. Not a concept: the
adapted artifacts execute, and every claim below is reproducible from this repository.

**Current proof:** Fabric → Paper · real bytecode adaptation · automated execution
verification · real Paper 1.20.1 server validation.

> Full architecture, methodology, and open-service model:
> [**WHITEPAPER.md**](WHITEPAPER.md)

## What actually works here

| Capability | Status |
|---|---|
| Fabric server entrypoint → Paper/Bukkit plugin | ✅ Real ASM remap + generated `JavaPlugin`, execution-verified |
| Both Fabric entrypoint paths (`ModInitializer`, `DedicatedServerModInitializer`) | ✅ Proven end-to-end (29/29 automated checks) |
| Adapted-artifact execution proof (isolated classloader) | ✅ The mod's real `onInitialize()` runs |
| Live-server validation | ✅ Adapted plugins booted on a real Paper 1.20.1 server; `onEnable()` dispatched and asserted in server logs |
| Artifact integrity | ✅ Every adapted artifact HMAC-SHA256-signed; verifier rejects tampered artifacts |
| Mod-license protection | ✅ MIT components grant embedded; verifier fails artifacts missing it |
| Reproducible builds | ✅ Byte-identical Maven reactor output across runs |

**Measured compatibility boundary** (methodology and per-mod results in
[WHITEPAPER.md §4](WHITEPAPER.md)): sampling the 50 most-downloaded mods matching
*server-side-required, Fabric, 1.20.1* on Modrinth (population 3,782):

- **36 of 50 (72%) fall inside the structurally supported boundary**; all produced
  well-formed, signature-valid adapted artifacts
- Among mods with a standard class entrypoint, the rate is **36/39 (92%)**
- A real-Paper batch over 8 of the sampled real mods confirmed all 8 loaded
  successfully — 2 fully enabling, and 6 reaching their own initialization before
  encountering Fabric intermediary-name dependencies, the exact boundary the hosted
  corpus exists to close (load/enable split stated, not smoothed over — §4)

## Prove it yourself

Requirements: Java 21, Node.js ≥ 18, Maven (for the Tync build).

```bash
# 1. Build The Tync (Java) — engine + cross-loader adapter
cd the-tync && mvn clean package && cd ..

# 2. Bridge unit + lint
cd minecraft-bridge && npm install && npm test

# 3. The cross-loader proof: builds two real Fabric fixture mods from source,
#    adapts them (ASM), executes the adapted artifacts, delivers to a test server.
#    29 assertions, no mocks in the transformation path.
npm run test:crossloader

# 4. Measure the compatibility boundary against LIVE Modrinth data
#    (downloads real popular Fabric 1.20.1 mods and adapts each)
npm run measure:boundary        # default sample: 50

# 5. Boot an adapted artifact on a real Paper 1.20.1 server
npm run verify:paper            # single artifact
npm run verify:paper:batch      # batch over measured boundary artifacts
```

The two Fabric fixture mods (source-checked, with visible `onInitialize()` log lines)
live in [`minecraft-bridge/test-fixtures/`](minecraft-bridge/test-fixtures/) — the
proof does not depend on trusting our fixtures: swap in any compatible Fabric mod and
run the same harness.

## The two halves

| Project | What it is | Language |
|---|---|---|
| [`the-tync/`](the-tync/) | Processing engine — a meta-loader that runs above all loaders, transforms mods, and hosts the cross-loader adapter | Java 21 / Maven |
| [`minecraft-bridge/`](minecraft-bridge/) | User-facing bridge — detects Minecraft servers, receives mods, drives The Tync one-way, launches servers with adapted artifacts | Node.js |

```
User → Bridge → The Tync engines → adapted mod → server launch
```

The Bridge calls The Tync one-way over a JSON engine contract (local JVM transport or
HTTP for a hosted engine). `minecraft-bridge/curator-api/` additionally defines the
open corpus-bundle contract (JSON Schemas + conformance suite) that third-party
curation services can implement.

## What is open vs. proprietary

The open tier contains the **complete framework**: engine abstraction (`Engine`
interface), engine manager, component loader, trigger system, mod delivery pipeline,
performance turbos, all 19 loader API libraries, the **working Fabric → Paper
cross-loader adapter with its verifier**, and the complete Bridge source.

The **remaining** transformation engine implementations (bytecode metamorphosis across
arbitrary boundaries, adaptive learning, predictive optimization) are proprietary and
not part of this repository; they ship as compile-compatible stubs that log a warning
and return an "unsupported" result, so the framework builds and runs out of the box.
The Bridge's native injection agent (`injector.jar`) is likewise distributed in
releases only. Building your own engine is one interface away — see
`the-tync/main-app/src/main/java/com/rnk/thetync/engines/Engine.java`.

## License

Copyright (c) 2026 RNK Studios

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, version 3 of the License. See [LICENSE](LICENSE).

The proprietary engine implementations referenced above are **not** licensed under
the GPL and are not part of this repository. Adapted artifacts embed an MIT
components grant so that adapted mods never inherit RNK's GPL.

## Contributors

- **RNK-Enterprise** — [github.com/RNK-Enterprise](https://github.com/RNK-Enterprise)
- **Lisa's Dungeon** — [github.com/lisasdungeon](https://github.com/lisasdungeon) · Lisasdungeon@gmail.com
