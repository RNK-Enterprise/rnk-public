# The RNK Ecosystem
## Universal Mod Compatibility for Minecraft — A Meta-Loader Architecture and Open-Service Model

**White Paper · Version 1.1 · September 2026**

> **What changed since v1.0:** a real, verified cross-loader transformation now runs
> end-to-end (Fabric server-side mod → loadable Paper artifact, with execution proof).
> The compatibility matrix is explicit. The transformation mechanism is documented.
> The transformation phases (pre-delivery vs. runtime) are disambiguated.

---

## Abstract

Minecraft's modding ecosystem is fragmented across more than a dozen incompatible loaders
and API generations. Every mod author must choose a target, and every server operator must
choose a stack — and the two choices rarely align. The RNK ecosystem inverts this model:
instead of asking mods to adapt to loaders, a processing layer adapts mods to loaders.

This paper describes a working, narrow proof of that thesis — a Fabric server-side mod
compiled against the Fabric API is rewritten at the bytecode level and executed as a
Paper artifact by an automatically generated entrypoint — and the architecture around it:
an open, auditable client stack (the Bridge and The Tync's open tier) plus hosted
transformation intelligence for breadth beyond the demonstrated boundary.

---

## 1. The Problem

A Minecraft mod is not portable. A Forge 1.20.1 mod cannot run on Fabric. A Paper plugin
cannot run on NeoForge. Each loader family defines its own class transformation pipeline,
event system, lifecycle hooks, and API surface. The result:

- **For mod authors:** porting cost per loader multiplies maintenance burden.
- **For server operators:** the loader choice locks the mod selection.
- **For the ecosystem:** duplicated effort and version fragmentation.

Prior approaches — multi-loader installers or API abstraction layers like SpongeAPI —
still require mods to be *written* against the abstraction. None adapt existing,
already-compiled mods across boundaries.

---

## 2. The Thesis: Adapt the Mod, Not the Author

RNK's core mechanism is **pre-delivery bytecode adaptation**: a processing layer reads a
compiled mod, rewrites its loader-specific surface, and emits an artifact that the target
loader can load natively. "Runtime injection" in this document refers to *delivery* of an
adapted artifact into a live server — not to classload-time transformation, which is a
separate, future mechanism (see §3.2).

```
┌───────────────────┐        ┌───────────────────────┐        ┌──────────────────┐
│   The Curator     │        │       The Tync        │        │    The Bridge    │
│  (hosted, closed) │───────▶│  (Java 21 engine,     │───────▶│  (open, local)   │
│                   │        │   hybrid open)        │        │                  │
│  sources and      │        │  PRE-DELIVERY         │        │  initiates all   │
│  prepares mods    │        │  ADAPTATION:          │        │  calls; receives │
│  for adaptation   │        │  rewrites mod JARs    │        │  adapted         │
│                   │        │  into target-loader   │        │  artifacts;      │
│                   │        │  artifacts            │        │  delivers them   │
└───────────────────┘        └───────────────────────┘        └──────────────────┘
```

All three tiers communicate over the same one-way contract: the Bridge (or any client)
invokes an engine call with a JSON payload and receives a JSON result.

---

## 3. Design Principles

### 3.1 One-Way Communication

All network flows are **initiated by the client**. Nothing connects back to the user's
machine: no listening ports, no reverse tunnels, no persistent sockets. Each call is
independent and stateless.

### 3.2 Two Transformation Phases, Stated Plainly

| Phase | Where | When | Status |
|---|---|---|---|
| **Adaptation** — rewrite mod bytecode for the target loader | The Tync | Before delivery, offline | **Working and verified** for the lifecycle boundary (§5) |
| **Delivery/Injection** — place the adapted artifact into a server | The Bridge | When the operator launches or while running | Working (recorded delivery today; agent-based live injection ships with the hosted tier) |
| **Classload-time weaving** — transform at `ClassLoader.defineClass` inside the live JVM | Tync agent inside the server JVM | At classload | Roadmap — required for APIs that need server-context rewrites |

Keeping adaptation pre-delivery is a deliberate v1 decision: it is auditable (the adapted
artifact is a plain JAR you can inspect before it ever touches a server), deterministic,
and testable without a running server — which is exactly what the execution-proof
harness in §5.3 exploits.

### 3.3 Black-Boxed Value, Open Contracts

The open tier contains a **real, working transformation** for one class of problems —
loader lifecycle-boundary adaptation — plus the full framework for adding more. What
stays hosted is *breadth*: the pattern corpora, mapping tables, and trained models that
extend coverage to content APIs, cross-version translation, and the long tail of API
surfaces. The rule: **the open stack must always demonstrate something real, and the
hosted tier must always be replaceable at the contract level.**

### 3.4 Everything Is Auditable

Every engine call is a fresh OS process with a hard timeout; every delivery writes a
local manifest (source, adaptation report, execution-proof flags, timestamps). An operator
can always answer "what was adapted, how, and what entered my server."

---

## 4. The Compatibility Matrix — What "Universal" Means Here

"Universal" is a direction, not a claim. Boundaries differ enormously in difficulty;
this is the honest state of each:

| Boundary | Difficulty | Status |
|---|---|---|
| **Fabric server entrypoint → Paper/Bukkit** | Lifecycle-boundary only: remap entrypoint interface, generate a `JavaPlugin` subclass, preserve initializer semantics | ✅ **Proven** — implemented, execution-verified in-harness, and validated on a real Paper 1.20.1 server (§5) |
| Fabric client entrypoint → server-side equivalent | Same mechanism, plus client-API stripping analysis | **Designed** — same framework, not yet exercised |
| Event-system translation (Fabric events → Bukkit events) | Requires per-event mapping tables | **Predicted** — hosted-tier corpus; no mapping rules exist yet |
| Content registration (blocks/items/worldgen) | Deep: registry semantics, datapacks, mappings | **Not claimed** (hosted tier) |
| **Cross-version** (e.g. 1.20 → 1.21) | Intermediary-name remapping tables per version pair | **Predicted** (hosted tier) — mechanism identified, no tables built |
| **Forge → anything** | FML's transformation pipeline is itself invasive; mods assume it | **Not claimed** (long-term) |

Status vocabulary: **Proven** = demonstrated end-to-end with automated tests. **Designed** =
mechanism specified in open code, not yet exercised. **Predicted** = credible path identified,
no implementation. **Not claimed** = out of scope today.

**Today's supported scope:** any Fabric 1.20.x server-side mod whose entrypoint implements
`DedicatedServerModInitializer` (or `ModInitializer`) and whose initializer path
(`onInitializeServer()` / `onInitialize()`) does not touch Fabric-only content APIs can be
adapted to a Paper server by the open engine. Adaptation is verified before delivery (§5.3).

**The boundary is measured.** Sampling the Modrinth population matching this scope
exactly (server-side-required, client-unsupported, Fabric, 1.20.1 — 3,782 mods), the top
50 by downloads were run through the open adapter with structural verification of each
adapted artifact (`npm run measure:boundary`, reproducible):

- **36 of 50 sampled mods (72%) fall inside the supported boundary** — each adapted with
  a valid shim, correct dispatch method, and intact provenance signature. Within the
  class-entrypoint subset (excluding the 11 data-pack-style mods that define a different
  boundary), the rate is **36/39 = 92%**.
- The 14 outs classify cleanly: 11 mods have no class entrypoint (data-pack-style content
  that registers no initializer class — a different boundary, not an adapter defect), 2 use
  class-file versions newer than the adapter's ASM parses, 1 had no 1.20.1 file in its
  version list.
- **Sampling caveat:** this is a popularity-weighted sample (top-by-downloads), not a
  random one; popular mods tend to be standard-shaped, so 72% is plausibly optimistic for
  the population. A uniformly random sample is future work.
- **Live-Paper execution over real mods.** The measurement does not stop at structure:
  adapted artifacts are booted on a real Paper 1.20.1 server
  (`npm run verify:paper:batch`). The demonstrated run booted **9 adapted artifacts —
  8 real mods plus the self-contained fixture as a control** — and every one **loaded
  cleanly**: Paper's plugin manager accepted all 9 with zero load failures. Reading the
  same run by the stricter enable rate: **2 of the 8 real mods enabled fully** (the
  control also enabled, 3 of 9 overall), and **6 of the 8 reached their own init code and
  failed there** with `NoClassDefFoundError` on Fabric intermediary names
  (`net.minecraft.class_*`) — precisely the boundary the hosted corpus exists to close,
  hit in the mod's logic rather than in RNK's output.
- **Scope of the number:** the boundary verdict is structural adaptability (the adapter
  produced a well-formed, signature-valid artifact); the batch run extends the adapter's
  claim to **"every artifact loads on real Paper; full enablement additionally requires
  the hosted mapping corpus"** — the load/enable split is stated rather than smoothed
  over.

The Bridge targets any server it can detect by server-JAR signature
(Paper/Spigot/Purpur/etc. today).

One boundary, honestly crossed and mechanically verified, is the claim this paper makes —
not universality.

---

## 5. The Tync (Hybrid Engine)

### 5.1 The Call Contract

```
java -cp the-tync-main.jar:dependency/* com.rnk.thetync.SimpleEngineRunner <engine> <jsonContext>
```

One JSON line in, one JSON line out; nested structures supported (Jackson). The engine
name dispatches through `SimpleEngineRunner`: the open tier's real transformation work
is `CrossLoaderAdapterEngine` (§5.2) with `ModDeliveryEngine` for delivery, while names
registered as proprietary stubs (e.g. `ValidationEngine`, `InjectionEngine`) return an
"unsupported" failure by design (§5.4). A `verifyBuildArtifacts`
preflight fails fast, with remediation instructions, when the engine build is missing.

### 5.2 What the Open Tier Actually Does — the Fabric → Paper Adapter

`CrossLoaderAdapter` performs a real bytecode transformation using ASM 9.7
(`org.ow2.asm`, the same library Fabric's own toolchain builds on):

1. **Parse.** `ClassReader` parses every class in the source mod JAR.
2. **Remap.** A `Remapper` (ASM commons) rewrites every internal name and every
   reference from Fabric entrypoint interfaces — `net/fabricmc/api/ModInitializer`,
   `net/fabricmc/api/DedicatedServerModInitializer` — to RNK shim interfaces
   (`com/rnk/shim/*`). The shims preserve each interface's real method shape —
   `ModInitializer.onInitialize()` and `DedicatedServerModInitializer.onInitializeServer()` —
   so remapped classes keep valid bytecode with no body rewriting needed.
3. **Generate.** ASM generates a Paper entrypoint — `com.rnk.adapted.GeneratedPaperEntrypoint`,
   a `JavaPlugin` subclass whose `onEnable()` logs, instantiates the remapped initializer,
   and invokes the matching initializer method (`onInitialize()` or `onInitializeServer()`,
   dispatched by which Fabric interface the original class implemented) — compiled with
   `COMPUTE_FRAMES` so stack frames are valid.
4. **Repackage.** The adapter emits a valid JAR: remapped classes, inlined shim
   interfaces, generated entrypoint, `plugin.yml` (`main: com.rnk.adapted.GeneratedPaperEntrypoint`),
   and a manifest marked `RNK-Adapted-From: fabric / RNK-Adapted-To: paper`, plus a JSON
   adaptation report (entrypoints found, classes rewritten, shims inlined, byte counts).
   The original `fabric.mod.json` is preserved under `rnk-preserved/` for traceability.

**Known limits, stated:** this boundary requires no field or method-body remapping, which
is why it is the right first proof — the interesting hard cases (content APIs, reflection
against Fabric-only classes, intermediary mappings) are precisely what the hosted corpus
exists to solve, and the adapter reports anything it cannot rewrite rather than guessing.

### 5.3 The Execution Proof — How We Know It's Real

`AdaptedArtifactVerifier` refuses to take the adaptation on faith:

1. It compiles a minimal Bukkit API stub from source at run time — mimicking the server
   providing the API. The stubbed surface is explicit and reported in every verify output
   (`bukkitStubSurface`): `JavaPlugin.getLogger()/getDataFolder()/getServer()`, the static
   `Bukkit` facade, `Server.getVersion()/getPluginManager()`,
   `PluginManager.registerEvents()` (no-op), and the `event.Listener` marker. A mod touching
   deeper Bukkit surface (worlds, registries, scheduled tasks) is beyond the stub harness —
   noted as such rather than silently faked.
2. It loads the adapted artifact in an isolated `URLClassLoader` over [stub, artifact].
3. It drives the exact lifecycle Paper would: instantiate the generated entrypoint,
   call `onEnable()`.
4. It asserts the proof chain:
   - the mod's **real code executed** (observable side effect set by the mod's own `onInitialize()`),
   - the initializer now implements the **RNK shim**, and the **Fabric interface is absent**
     from the classpath (the adaptation is semantic, not cosmetic),
   - manifest markers are present.

Exit code 0 means the adapted artifact ran the original mod logic. The harness runs
automatically in CI-style suites (`npm run test:crossloader`, 29 checks over **both**
entrypoint paths — server and client/main fixtures — each asserted to have taken its
expected remap path) after `receiveJar → adapt → verify → deliver` through the Bridge
pipeline.

The harness proves execution; a live-server script proves deployment. `npm run verify:paper`
(`test-fixtures/verify-on-real-paper.sh`) downloads a real Paper 1.20.1 server JAR, accepts
the EULA, installs the adapted plugin into `plugins/`, boots the server headless, and asserts
the mod's own log line appears in live server output before shutting it down. The strongest
claim in this paper is therefore **"runs on the target server,"** not merely "executes in a
harness."

### 5.4 What Is Open vs. What Is Hosted

| Component | Open | Hosted (black box) |
|---|---|---|
| Engine call contract + preflight | ✅ Stable, documented | — |
| Bridge ↔ hosted transport (HTTP + Bearer auth, TLS-enforced for remote hosts) | ✅ Real | — |
| **CrossLoaderAdapter framework + Fabric→Paper adapter** | ✅ **Real, verified code** | — |
| Execution-proof verifier (`AdaptedArtifactVerifier`) | ✅ Real | — |
| Fixture mods (both entrypoint paths) + reproducible build script | ✅ Real | — |
| **Curator interface (corpus-bundle contract, v0.1)** | ✅ **Specified** (`curator-api/`, JSON Schemas + conformance suite) | — |
| Bridge client, injection agent plumbing, delivery manifests | ✅ Real | — |
| Event/content/cross-version mapping corpora | — | ✅ Production tables |
| Broad multi-loader, multi-version coverage | Boundary defined (§4) | ✅ The service |
| The Curator (mod sourcing/curation) | Interface open; implementation & corpora hosted | ✅ The service |

The open adapter is a working member of a framework, not a skeleton: a new loader
boundary is a new `Remapper` mapping + entrypoint generator behind the same contract,
and §5.3's verifier applies unchanged.

### 5.5 Self-Hosting

```bash
cd the-tync && mvn clean package   # or ./mvnw once added
cd minecraft-bridge && npm run test:crossloader          # reproduces the proof
```

The Maven build regenerates its dependency set; the Bridge discovers the artifacts and
preflights them. A hosted engine replaces the local one via `RNK_TYNC_BASE_URL` +
`RNK_TYNC_AUTH_TOKEN` (or `--tync-url` / `--tync-token` flags) — no code changes. The
HTTP transport speaks the same JSON contract with Bearer-token auth, enforces the same
timeouts, and refuses plaintext HTTP to non-local hosts unless explicitly overridden.

---

## 6. Security Model

1. **No inbound connections.** All flows client-initiated.
2. **Stateless calls.** Fresh process per engine call; SIGKILL timeout enforcement.
3. **Payload minimality.** Calls carry transformation metadata only.
4. **Local audit trail.** Delivery + cross-loader manifests record every artifact.
5. **Fail-fast integrity.** Engine builds are preflighted before use.
6. **Inspect-before-trust.** Adaptation is pre-delivery: the artifact is a plain JAR an
   operator (or their tooling) can decompile and inspect before it loads.

---

## 7. Current Implementation Status

### Demonstrated end-to-end (real, verified, reproducible)

- **Fabric → Paper adaptation with execution proof, both entrypoint paths.** Real
  Fabric mods (source-checked fixtures: `test-fixtures/fabric-heartsync/` for
  `DedicatedServerModInitializer`, `test-fixtures/fabric-heartsync-client/` for the
  main `ModInitializer` path) are compiled, adapted via ASM, executed in isolated
  classloaders, and their original `onInitializeServer()` / `onInitialize()` logic is
  observed running — with each artifact asserted to have taken its expected remap path.
  Automated: `npm run test:crossloader` (29/29).
- **Live-server validation.** The adapted server-entrypoint plugin is booted on a real
  Paper 1.20.1 server: Bukkit's plugin manager loads it, dispatches `onEnable()`, and the
  mod's original `onInitializeServer()` executes on the live server thread. The script
  asserts the mod's log line in real server output (`npm run verify:paper`), and the same
  harness batches over **adapted artifacts from real sampled mods**
  (`npm run verify:paper:batch`; results in §4).
- **Full Bridge pipeline.** `receiveJar → adapt → verify → deliver → manifest`, with
  on-disk verification and self-cleaning runs.
- **Mod-license protection.** Every adapted artifact embeds the MIT components grant
  (`rnk/RNK-COMPONENTS-LICENSE.txt` + manifest attribute), and the verifier fails any
  artifact missing it (§10.1).
- **Test infrastructure.** 10 unit + 47 integration (real JVM spawns, timeout/broken-
  classpath/garbage-output paths; CrossLoaderAdapterEngine contract coverage including a
  no-entrypoint rejection path, a stale-JAR guard, HMAC signature/tamper-detection
  checks, and components-license presence/stripping checks; hosted-endpoint client tests
  against a mock server; and hosted-server contract tests running the full Bridge → HTTP
  → engine → HTTP chain) + 12 pipeline E2E + 19 delivery E2E (including the cross-loader
  audit step) +
  29 cross-loader checks — all green, lint-clean.
- **Reproducible offline builds.** Maven reactor builds are byte-identical across runs:
  fixed archive timestamps via `project.build.outputTimestamp` (Reproducible Builds
  convention), pinned jar-plugin version, no environment-varying manifest entries. Verified
  empirically with back-to-back `clean package` + `cmp`.

### Hosted-tier scope (not demonstrated by the open tier, by design)

- Event-system, content, and cross-version translation corpora (§4).
- Trained adaptive/predictive models; The Curator service.
- Agent-based live injection configured for arbitrary server JVMs.

### Not yet built

- Classload-time weaving agent (§3.2, roadmap).
- Hosted service hardening beyond token auth: TLS pinning, token issuance/rotation,
  per-call signing.

---

## 8. Why the Open Parts Stand Alone

For this project, the open components are not a client for a closed service — they are
independently valuable infrastructure:

1. **An adaptation framework with a working reference adapter.** CrossLoaderAdapter +
   the shim/entrypoint pattern is a reusable basis for further loader boundaries; the
   fixture and build script make the first boundary reproducible by anyone.
2. **A verification methodology.** The execution-proof harness (§5.3) is generally
   applicable to "does this artifact actually run in this container?" — useful for any
   plugin/mod pipeline, independent of RNK services.
3. **Operational runtime infrastructure.** Server auto-detection, inbox/delivery with
   manifests, timeout-isolated engine calls, build preflight — the unglamorous plumbing
   every mod-delivery tool needs, fully open.
4. **Open contracts — including the curation layer's.** The engine interface is a stable,
   documented process boundary that any conformant engine — open or commercial — can
   implement. The Curator's interface is open too (`curator-api/`, v0.1): the corpus
   bundle format a curator publishes and a Tync consumes is a versioned, schema-validated,
   dependency-free JSON contract. A third party can build a competing curator — different
   mining, different corpora, different quality bar — that the open Tync consumes without
   asking anyone's permission. The contracts are public, and the most defensible proof is
   that the closed service's *output* is exchangeable.

   Honest status: v0.1 shipped recently and has **no independent implementations yet** —
   the claim is about the contract's openness and conformance machinery, not adoption.
   The reference bundle and conformance suite in `curator-api/` are there so an external
   implementer's path is hours, not weeks.

The hosted tier earns its place by *coverage and quality* — corpora, models, breadth —
never by owning the contracts or the user's runtime.

---

## 9. Roadmap

1. **Near term:** hosted service hardening (TLS pinning, token issuance/rotation);
   live-Paper execution runs over the measured boundary set (§4); Curator conformance
   suite beyond the shipped examples; Fabric client-entrypoint boundary via the existing
   framework. *(Done: HMAC-signed adapted artifacts; boundary measurement; curator
   bundle contract v0.1.)*
2. **Mid term:** event-mapping corpus v1 behind the adapter; The Curator alpha;
   classload-time weaving agent for server-context rewrites.
3. **Long term:** adaptive learning loop over opt-in, anonymized adaptation telemetry;
   cross-version translation; multi-server management; keeping pace with new Java
   class-file versions (an ongoing ASM-upgrade obligation for the adapter — 2 of the 50
   sampled mods already exceeded ASM 9.7's parse range).

---

## 10. Licensing

- **Bridge and the open Tync tier** (including CrossLoaderAdapter and the verifier):
  open source under **GPL-3.0-only** (the full license text ships as `LICENSE` in both
  open repositories).
- **Hosted services** (corpora, models, Curator): proprietary, offered as a service.

GPL-3.0-only is a deliberate choice, not an accident: strong copyleft is what keeps
forks of the open tier equally open, which is the mechanism behind the open-core split.
It does mean proprietary panels and tooling cannot link the Bridge directly — the
supported integration path is the process boundary the ecosystem already standardizes
on: the HTTP `/engine` contract (local engine, or hosted engine with Bearer auth), and
the `curator-api/` bundle contract. Integrators get the same seam every other RNK
tier uses; they just cannot inline the code.

### 10.1 The Mod's License Is Never Touched

An adapted artifact is a **combined work**: the mod author's code plus RNK-originated
glue (shim interfaces, generated entrypoint). If that glue were GPL-3.0, distribution
of the artifact would carry GPL obligations onto the whole — including an MIT, Apache,
or all-rights-reserved mod. RNK licenses the glue **MIT** instead:

- Every adapted artifact embeds `rnk/RNK-COMPONENTS-LICENSE.txt`, an MIT grant covering
  exactly the RNK Components (`com/rnk/shim/*.class`, the generated entrypoint, the
  license file and manifest attributes), and declares `RNK-Components-License: MIT` in
  its manifest.
- Structural verification **fails** an artifact missing that grant, so it cannot be
  lost in the pipeline.
- The mod's own code is unaffected: it stays under its original terms, whatever they
  are. RNK never claims, changes, or restricts it.
- The adapter framework and the open Tync **remain GPL-3.0-only** — the MIT grant covers
  only the generated components that enter the artifact, the standard Classpath-style
  exception pattern applied at the component boundary.

For local-only adaptation no distribution occurs and no license question arises; with
the MIT glue, distribution is clean regardless. Modders keep their licenses. Full stop.

The dividing rule remains: **users must never have to trust a black box to stay in
control of their own servers** — and with a real, verified transformation in the open
tier, the open stack now proves the thesis itself, not just the plumbing around it.

---

## 11. Summary

The RNK ecosystem's claim is no longer only architectural. A Fabric server-side mod was
adapted — by open, inspectable code using standard tooling — into a Paper artifact that
provably executed its original logic. The boundary crossed is narrow and honestly
labeled; the framework, the proof methodology, and the operational stack around it are
real; and the path from one proven boundary to breadth is the hosted service this
architecture is built to deliver.
