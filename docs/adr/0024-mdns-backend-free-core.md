# ADR 0024 — mDNS core is backend-free; backends bind per-platform

## Status
Accepted (2026-11-09).

## Context

The desktop companion and the headset both need to speak mDNS: the desktop announces, the headset resolves. Two options for sharing code:

1. One crate owning both the data shapes AND the actual mDNS backend (e.g. `mdns-sd`).
2. One crate for data shapes only; each platform pulls its own backend.

Pulling a Rust mDNS crate (and its transitive dependencies — socket2, nix on Unix, winapi on Windows) onto the Android side costs app size and build time for a feature the headset side only consumes, not publishes.

## Decision

`companion/crates/discovery` holds only:

- the DNS-SD service type (`_questclassic-companion._tcp.local.`),
- the default port (`48122`, mirrored from the protocol crate),
- TXT-record key names (≤15 chars each for Bonjour compatibility),
- `ResolvedCompanion` + its `from_txt` parser.

No actual mDNS backend. The desktop side (`src-tauri`) pulls `mdns-sd` and calls into this crate for the keys/types; the headset side (Kotlin JmDNS or NsdManager) reads the literal constants directly through a Kotlin mirror.

## Consequences

**Pros**
- One source of truth for the wire shape; shift it in one crate and both sides fail to compile against the old version.
- Backend choice is per-platform without touching shared code.
- Discovery crate is 6 unit tests, zero async, zero platform deps — trivially auditable.

**Cons**
- Two places to update for a port change (protocol crate + discovery crate). Mitigated by a test that pins the constant and a comment that calls out the lock-step.

## Alternatives considered

1. **One crate with the backend inline.** Rejected — forces every consumer (including the Kotlin side via a bridge) to depend on a Rust mDNS stack.
2. **`tokio`-based async in the shared crate.** Rejected — `ResolvedCompanion` is a data shape; no async surface needed; adding `tokio` here would force every consumer to compile it.
3. **Hardcoded port, no shared constant.** Rejected — port mismatches would be silent connect-refuseds, hard to debug.
