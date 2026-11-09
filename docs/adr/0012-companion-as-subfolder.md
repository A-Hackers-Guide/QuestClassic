# ADR 0012 — Companion desktop app lives in a subfolder of this repo

## Status
Accepted (2026-11-09).

## Context

The virtual-monitor feature needs a desktop companion app (Rust + Tauri 2) that runs on macOS / Windows / Linux / FreeBSD. It owns per-OS virtual-display drivers, input injection, and the WebRTC video pipeline.

Options for where it lives:
- Subfolder (`companion/`) of this repo.
- Separate repo (`QuestClassic-Companion`).

## Decision

**Subfolder.** `companion/` sits alongside `app/`, with its own `Cargo.toml` workspace, Tauri shell, protocol crate, docs, and CI workflow.

## Consequences

- One repo, one issue tracker, one release pipeline → fewer places for a PR to drift out of sync between the headset side and the desktop side.
- `companion/crates/protocol/` is the single source of truth for the wire format; both ends consume it (the Kotlin side via a stub generator in a later milestone).
- The CI workflow (`.github/workflows/companion.yml`) fires only on paths under `companion/` so changes to the Android app don't rebuild the Rust workspace and vice versa.
- Users browsing the repo see the whole system at once; good for discoverability, mildly longer `git clone` for Android-only contributors.

## Alternatives considered

1. **Separate `QuestClassic-Companion` repo.** Rejected — the wire-protocol crate would need to be published to a registry or vendored into each repo, both of which complicate PRs that touch both sides. Two trackers, two CI configs, twice the set of maintainers to keep straight.
2. **Monorepo with Gradle composite build.** Rejected — Gradle's composite build story for Rust workspaces is weak; mixing them means every Kotlin PR runs a Rust build and vice versa.
3. **Separate Tauri app in `apps/companion/`** inside a QuestClassic monorepo. Rejected as a naming thing — `apps/` implies multiple user-facing apps built from a shared codebase; the companion is its own independent product.
