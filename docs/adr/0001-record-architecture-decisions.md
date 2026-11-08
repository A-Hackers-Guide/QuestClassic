# ADR 0001 — Record architecture decisions

- **Status**: Accepted
- **Date**: 2026-11-07

## Context

We want future contributors to know why we made the technical choices we made, so they don't spend time reversing-engineer intent or re-litigating settled decisions.

## Decision

We adopt Michael Nygard's "Architecture Decision Records" convention. Each non-trivial technical decision gets a numbered ADR in `docs/adr/`. ADRs are immutable — if we change our mind later, we write a new ADR that supersedes the old one, we don't edit the old one.

## Format

```
Title · status · date
Context — what forced this decision
Decision — what we decided
Consequences — what this makes easier + harder
```

Short. No padding. If an ADR needs more than two screens, it's probably actually two decisions.

## Consequences

- +: readers can trace "why does the DI container look like this?" to [ADR 0003](0003-hand-wired-di.md) rather than guessing.
- +: a fresh maintainer can read `docs/adr/*.md` and know the whole set of commitments the project has made.
- -: writing an ADR adds overhead to a PR that makes a significant decision. We accept this.
