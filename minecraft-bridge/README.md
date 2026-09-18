# RNK Minecraft Bridge

The user-facing half of [RNK's open cross-loader compatibility stack](../README.md).
The Bridge detects local Minecraft servers, receives compiled Fabric mods, drives
The Tync's **CrossLoaderAdapterEngine** to rewrite them into loadable Paper plugins
(real ASM bytecode transformation), verifies each adapted artifact, and delivers it
into the target server — all in one direction, all inspectable on disk.

```
User → Bridge → The Tync engines → adapted mod → server launch
```

The proven boundary today: **Fabric 1.20.x server mods → Paper plugins**, with
automated execution proof (29/29 checks) and live Paper 1.20.1 server validation.
Methodology and boundaries: [WHITEPAPER.md](../WHITEPAPER.md).

## Features

- **One-way architecture** — the Bridge initiates every call; nothing connects back.
  No listening ports, no reverse tunnels, no persistent sockets.
- **Local or hosted engine** — calls The Tync over a spawned-JVM JSON contract, or an
  HTTP engine endpoint (`RNK_TYNC_BASE_URL` + Bearer token) with identical semantics.
- **Transport safety** — refuses plaintext HTTP to non-loopback endpoints; requires
  HTTPS (or an explicit `RNK_ALLOW_INSECURE_TYNC=1` override for trusted networks).
- **Receive → adapt → verify → deliver** — every step writes on-disk artifacts and a
  JSON manifest, so an operator can always answer *what was adapted and what entered
  my server*.
- **Integrity checks downstream** — adapted artifacts are HMAC-SHA256-signed by the
  engine and carry an embedded MIT components grant; the verifier rejects tampered
  or unlicensed artifacts before delivery.
- **Server detection & launch** — finds local server installations by JAR
  filename heuristics (`paper`, `spigot`, `bukkit`, `forge`, `minecraft`, `server`)
  and launches them with the adapted artifacts in place.

## System Requirements

- **Node.js** ≥ 18 (CI tests on Node 20 and 22)
- **Java** JDK 21 — required to build and run The Tync, which performs the actual
  adaptation; server launching uses your installed JDK
- **The Tync** — build once from the repository root:
  `cd the-tync && mvn clean package`

## Installation

Clone and run from source:

```bash
git clone https://github.com/lisasdungeon/rnk-public.git
cd rnk-public/minecraft-bridge

npm install
npm start
```

Prebuilt executables are not yet published. `npm run build` (pkg) creates
standalone binaries in `dist/` if you want them.

## Usage

### Running the application

```bash
npm start
```

The CLI walks through server detection, mod reception, adaptation, and launch.

### Receiving a mod

```bash
node index.js path/to/mod.jar            # bare JAR path
node index.js --receive-jar mod.jar      # explicit
node index.js --receive-url https://example.com/mod.jar   # download first
```

`RNK_RECEIVE_JAR` / `RNK_RECEIVE_URL` do the same via environment.

### Environment variables

| Variable                | Purpose |
| ----------------------- | ------- |
| `RNK_TYNC_PATH`         | Path to a local The Tync checkout (default: parent directory) |
| `RNK_TYNC_BASE_URL`     | Use a hosted HTTP engine instead of the local JVM transport |
| `RNK_TYNC_AUTH_TOKEN`   | Bearer token for the hosted engine transport |
| `RNK_RECEIVE_JAR`       | Mod JAR to receive at startup |
| `RNK_RECEIVE_URL`       | Mod JAR URL to download and receive at startup |
| `RNK_ALLOW_INSECURE_TYNC` | Set to `1` to permit plaintext HTTP to a trusted private engine |

## The cross-loader proof

From this directory:

```bash
npm run test:crossloader    # 29/29 checks: real fixtures built from source, ASM
                            # adaptation, execution proof, both entrypoint paths
npm run measure:boundary    # adapt+verify the top-N downloaded Modrinth mods in the
                            # Fabric 1.20.x server-side population (default 50)
npm run verify:paper        # boot one adapted artifact on a real Paper 1.20.1 server
npm run verify:paper:batch  # batch boot the measured boundary artifacts
npm test                    # 10 unit tests
npm run test:end-war        # 12 checks: end-war-api contract conformance
```

Measured boundary results are committed at
[`boundary-measurement.json`](boundary-measurement.json) (36/50 = 72%, per-mod rows).
The two fixture mods and the Paper harness live in
[`test-fixtures/`](test-fixtures/).

## Architecture

| Layer | File | Role |
|---|---|---|
| Tync connection | [`TyncConnectionLayer.js`](TyncConnectionLayer.js) | Local JVM spawn or HTTP transport; build-artifact preflight; TLS policy |
| Server detection/launch | [`ServerLauncherLayer.js`](ServerLauncherLayer.js) | Finds and starts local Minecraft servers |
| Mod injection | [`UniversalModInjectionLayer.js`](UniversalModInjectionLayer.js) | Delivery pipeline; generates the `injector.jar` placeholder (agent-based live injection ships with the hosted tier) |
| CLI | [`UserInterfaceLayer.js`](UserInterfaceLayer.js) | User interaction |

The Bridge calls The Tync **one-way** over the JSON engine contract
(`com.rnk.thetync.SimpleEngineRunner` locally, `/engine` remotely). The open
corpus-bundle contract for third-party End War services lives in
  [`end-war-api/`](end-war-api/).

## Security notes

- Every network flow is client-initiated; each engine call is independent.
- The hosted-transport token is sent as `Authorization: Bearer …` and never logged.
- Adaptation is pre-delivery: adapted artifacts are plain JARs you can decompile
  before they touch a server.
- See the repository [security policy](../SECURITY.md) for reporting vulnerabilities.
  Please do not open public issues for exploitable bugs.

## License

Licensed under the GNU General Public License v3.0 — see the repository root
[LICENSE](../LICENSE).

## Contributors

- **Lisa's Dungeon** — [github.com/lisasdungeon](https://github.com/lisasdungeon)
