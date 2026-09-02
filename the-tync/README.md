# The Tync — Universal Minecraft Loader Engine

The Tync is a meta-loader engine that operates above all Minecraft loaders. It hosts
specialized processing engines behind a single reactive `Engine` interface and ships
API libraries for every major loader, so mods can be transformed for universal
compatibility.

**Java 21 LTS · Maven · Project Reactor · Micrometer**

## Architecture

```
The Curator (server-side delivery)
        ↓
The Tync (this project — processing engines)
        ↓
The Bridge (user-facing loader — see ../minecraft-bridge)
```

### Engines

Engines implement the `Engine` interface
(`main-app/src/main/java/com/ld/thetync/engines/Engine.java`):
`getName / getVersion / initialize / execute / shutdown / getMetrics`, executing
reactively over a shared `EngineContext` and returning `EngineResult`s.

| Engine | In this distribution |
|---|---|
| CachingPerformanceEngine, DataSerializationEngine, MonitoringMetricsEngine, NetworkSynchronizationEngine, SandboxingEngine | Full source |
| BytecodeMetamorphosis, ShimGeneration, Injection, ApiMapping, ApiBridge, MultiLoaderBridge, Validation, PatternRecognition, IssueResolution, AdaptiveLearning, PredictiveOptimization | Proprietary — compile-compatible stubs included |

The proprietary engines are the core transformation pipeline. Their implementations
are not open source; the included stubs log a warning and return an
`EngineResult.failure(...)` so the framework builds and runs. Implement the
`Engine` interface and register via `ComponentLoader.registerLazyEngine(...)` to
plug in your own.

### Engine infrastructure (full source)
- `EngineManager` — registration, lookup, lifecycle
- `ComponentLoader` — lazy loading with suppliers
- `TriggerManager`, `OptimizationOrchestrator`, `TyncCore`
- `EngineRunner` / `SimpleEngineRunner` — CLI entry points
- `TurboManager` + 24 turbos — performance accelerators
- `bridge/ModDeliverySystem` — the Curator → Tync → Bridge delivery pipeline

### Loader API libraries (19 modules, full source)

Forge, Fabric, Paper, Spigot, Bedrock, Quilt, Sponge, LiteLoader, Glowstone,
Magma, Mohist, Cuberite, DragonProxy, Patchwork, Cardinal Components, Common API,
API Mapper, Issue Patterns, Fix Patterns.

Each is a Maven module under `libraries/` defining the loader's API surface
(classes, methods, events), known issue patterns, and fix templates used by the
transformation pipeline.

## Building

Requires JDK 21 and Maven 3.9+:

```bash
mvn clean package
```

Run the CLI:

```bash
mvn -pl main-app exec:java -Dexec.mainClass=com.ld.thetync.SimpleEngineRunner
```

## License

Copyright (c) 2026 Lisa's Dungeon

Licensed under the GNU General Public License v3.0 — see the repository root
[LICENSE](../LICENSE). The proprietary engine implementations referenced above are
not licensed under the GPL and are not part of this repository.
