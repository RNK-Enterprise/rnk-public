# Security Policy

RNK ingests third-party JARs, rewrites their bytecode, signs the result, and places
executable artifacts into Minecraft server environments. That pipeline is trusted with
running arbitrary code, so integrity problems here are real security problems.

## Supported Versions

There are no tagged releases yet; the supported version is `main`:

| Version       | Supported          |
| ------------- | ------------------ |
| `main` branch | :white_check_mark: |
| other refs    | :x:                |

Please test against a current checkout of `main` before reporting.

## Reporting a Vulnerability

**Do not open a public issue for anything exploitable.**

This repository has [private vulnerability reporting](https://github.com/RNK-Enterprise/rnk-public/security/advisories/new)
enabled — use the **"Report a vulnerability"** button on the repository's **Security**
tab. Reports stay private between you and the maintainers.

What to include: affected component, reproduction steps or a proof-of-concept, and any
adapted artifact or input JAR involved. If the report is accepted, we will credit you
in the fix unless you prefer to remain anonymous.

- **Acknowledgement:** within 7 days.
- **Status updates:** at least every 14 days until resolution.
- **Disclosure:** coordinated — the fix lands first, details publish afterward, on a
  timeline agreed with the reporter.

## Scope

**In scope:**

- **Artifact integrity and provenance** — anything that could let an adapted artifact
  pass verification without having been produced by the adapter: HMAC verification
  bypasses, tamper-detection gaps, manifest-marker forgery, or weaknesses in signing-key
  handling (`RNK_SIGNING_KEY`, `RNK_SIGNING_KEY_FILE`, the `~/.rnk/tync-signing.key`
  fallback).
- **The transformation path** — injection of unexpected content through mod JARs
  (manifests, `fabric.mod.json`, nested archives, crafted class files) into the
  `CrossLoaderAdapter`, the verifier, or the Bridge's receive/deliver pipeline.
- **Transport security** — violations of the Bridge's stated guarantees: the refusal of
  plaintext HTTP to non-loopback Tync endpoints, Bearer-token handling, and the
  `RNK_ALLOW_INSECURE_TYNC` override behaving outside its documented meaning.
- **Engine process isolation** — escapes from the documented one-way, per-call process
  model (`SimpleEngineRunner` / HTTP transport).
- **Untrusted-JSON parsing** — schema-bypass or parser abuse via the
  `curator-api/` conformance path.

**Out of scope:**

- Vulnerabilities in Minecraft, Paper, Fabric, or other upstream loaders — report
  those upstream.
- Issues requiring local shell access or a malicious local user (the tool runs with
  the operator's own privileges by design).
- Denial of service against hosted RNK services; no hosted service is described or
  distributed by this repository.
- Reports from automated scanners without a demonstrated impact path — send the
  reasoning, not just the banner.

## Automated Scanning

The repository runs CodeQL (Java, JavaScript, GitHub Actions workflows) on every push
and pull request to `main`, plus a weekly scheduled scan. Secret scanning and push
protection are enabled. Dependabot watches the Maven and npm dependency trees. If you
find something those missed, the private channel above is the right place.
