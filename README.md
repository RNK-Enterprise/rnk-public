# RNK Studios Ecosystem

Universal Minecraft mod compatibility tooling: load any mod on any server.

| Project | What it is | Language |
|---|---|---|
| [`the-tync/`](the-tync/) | Processing engine — a meta-loader that runs above all loaders and transforms mods for universal compatibility | Java 21 / Maven |
| [`minecraft-bridge/`](minecraft-bridge/) | User-facing bridge — auto-detects Minecraft servers, connects them to The Tync, and launches servers with transformed mods | Node.js |

## How it fits together

```
User → Bridge → The Tync engines → transformed mod → server launch
```

- **The Bridge** (this repo, `minecraft-bridge/`) detects your server, calls The Tync
  one-way, and launches the server with the result injected.
- **The Tync** (`the-tync/`) hosts 16 processing engines behind a common `Engine`
  interface, plus loader API libraries (Forge, Fabric, Paper, Spigot, Bedrock, Quilt,
  Sponge, and more), performance "turbos", and a reactive (Project Reactor) pipeline.

## Status of the open-source release

The public distribution contains the full framework: the engine abstraction, engine
manager, component loader, trigger system, mod delivery pipeline, turbos, all 19
loader API libraries, and the complete Bridge source.

**The core transformation engine implementations (bytecode metamorphosis, API
mapping/bridging, injection, shim generation, validation, pattern recognition,
issue resolution, adaptive learning, predictive optimization) are proprietary and
are not included.** They are replaced by compile-compatible stubs that log a warning
and return an "unsupported" result, so the framework builds and runs out of the box.
The Bridge's native injection agent (`injector.jar`) is likewise distributed in
releases only.

Building your own engine is as easy as implementing the `Engine` interface and
registering it — see `the-tync/main-app/src/main/java/com/rnk/thetync/engines/Engine.java`.

## Building

### The Tync (Java 21+, Maven 3.9+)

```bash
cd the-tync
mvn clean package
```

### The Bridge (Node.js 16+)

```bash
cd minecraft-bridge
npm install
npm start
```

## License

Copyright (c) 2026 RNK Studios

This program is free software: you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation, version 3 of the License. See [LICENSE](LICENSE).

The proprietary engine implementations referenced above are **not** licensed under
the GPL and are not part of this repository.
