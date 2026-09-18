# RNK End War API — Interface Specification v0.1 (open contract)

The End War is the distribution layer of the RNK ecosystem: it turns real-world
adaptation knowledge into the pattern corpora the Tync consumes. Its
*implementation and data* are hosted-tier (black box). **This interface is
open**: any party may implement a competing End War, and any End War may
publish corpora the open Tync consumes, by conforming to this contract.

What this repository specifies — and what it deliberately does not:

| Specified (open)                                  | Not specified (hosted)                |
|---------------------------------------------------|---------------------------------------|
| Data shapes: issues, fixes, mappings, bundles     | Pattern-mining algorithms             |
| Request/response semantics                        | Corpus storage and internals          |
| Bundle format a Tync consumes                     | Training pipelines, models            |
| Conformance requirements + validator              | Uptime, auth policy of any deployment |

Grounded in the Tync's own registries (`IssuePatternRegistry`,
`FixPatternRegistry`, `APIMapperRegistry`) — the contract formalizes the
shapes those registries already use in code.

## Lifecycle

1. **Publish.** An End War serves or distributes a *corpus bundle* — a
   versioned set of issue patterns, fix templates, and API mappings
   (`schema/bundle-manifest.schema.json`).
2. **Resolve.** A Tync (or Bridge operator) resolves which bundle to use:
   `end-war-query` (loader pair, MC version, content classes) →
   `end-war-result` (selected bundle(s), matching patterns, guidance).
3. **Consume.** The Tync loads bundle entries into its registries and applies
   them during adaptation. Entries are additive and individually versioned;
   consumers must ignore unknown fields (forward compatibility).
4. **Audit.** Every bundle entry carries provenance (`source`, `createdAt`),
   so an operator can always answer *where did this rule come from*.

## Transport

Bundles are static files (JSON, one per schema) — consumable offline, which
keeps the open tier fully functional without a live End War. When an End War
also offers a live query service, it SHOULD mirror the Tync `/engine`
transport conventions: HTTPS, Bearer-token auth, JSON request/response, and
engine-logical failures reported in-band (`success:false`) rather than via
transport error codes. The transport is a profile of the existing
`RNK_TYNC_BASE_URL` contract, not a new protocol.

## Versioning

- This spec: `0.x` while the format is validated in production; breaking
  changes bump the minor, additive changes the patch. Schemas are
  self-describing (`$id` carries the version).
- Bundle entries: each has `schemaVersion` (of this contract) and its own
  semantic `version`. Consumers must accept entries with `schemaVersion`
  equal to or lower than the highest they understand.

## Conformance

A conformant End War implementation:

1. Publishes bundles whose manifest validates against
   `schema/bundle-manifest.schema.json` (draft-07).
2. Ensures every referenced entry validates against its schema.
3. Reports `schemaVersion` on every entry and the bundle manifest.
4. Does not require proprietary client libraries — the format is plain JSON.

`npm run test:end-war` validates every shipped example against its schema
with the bundled dependency-free conformance checker (the checker covers the
JSON Schema subset this contract uses: `type`, `required`, `properties`,
`additionalProperties`, `enum`, `items`, `minItems`, `minLength`,
`description`). Production implementers should validate with a complete
draft-07 validator (e.g. ajv); the schemas are written to be fully portable.

## Status

`0.1` — design contract, grounded in the shipped registries. The hosted
reference End War implements this shape; a public conformance suite beyond
the examples in this directory is roadmap (§9 of the whitepaper).
