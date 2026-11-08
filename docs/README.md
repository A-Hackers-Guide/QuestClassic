# QuestClassic documentation

Everything about the project, organized so each piece lives exactly one place and every other place links to it.

## First-time reading path

1. [Getting Started · Install](getting-started/install.md) — how to put this on your Quest in five minutes.
2. [User Guide · Launcher](user-guide/launcher.md) — the main screen, what each tile does.
3. [Architecture · Overview](architecture/overview.md) — the one-page shape of the code.
4. [FAQ](faq.md) — what people ask us in week one.

## Layout

```
docs/
├── getting-started/     install, dev mode, first launch, troubleshooting
├── user-guide/          one file per shipping panel
├── architecture/        how the code is organised
├── reference/           every manifest permission, build flavor, API facet
├── contributing/        how to send a PR that gets merged
├── security/            threat model, reporting, signing, dependency policy
├── adr/                 architecture decision records (one per decision)
├── reverse-engineering/ the OTA analysis that birthed the app
└── roadmap/             per-release planning
```

## Index

### Getting started

- [Install the APK on your Quest](getting-started/install.md)
- [Enable Developer Mode on your Meta account](getting-started/enable-developer-mode.md)
- [First-launch walkthrough](getting-started/first-launch.md)
- [Troubleshooting](getting-started/troubleshooting.md)

### User guide — one page per panel

- [Launcher](user-guide/launcher.md)
- [Diagnostics](user-guide/diagnostics.md)
- [AI Companion](user-guide/ai-companion.md)
- [Build Tools](user-guide/build-tools.md)
- [Updates](user-guide/updates.md)
- [About / System](user-guide/about.md)
- [Signature Verifier](user-guide/signature-verifier.md)
- [System Internals](user-guide/system-internals.md)
- [Meta Hosts Audit](user-guide/meta-hosts-audit.md)
- [AR Home (preview)](user-guide/ar-home.md)

### Architecture

- [Overview](architecture/overview.md)
- [Module map](architecture/module-map.md)
- [Jetpack Compose usage](architecture/compose-ui.md)
- [DI container](architecture/di-container.md)
- [Coroutines & threading](architecture/coroutines.md)
- [On-device storage](architecture/storage.md)
- [Theming & nostalgic look](architecture/theming.md)

### Reference

- [Buzz-pattern catalog](reference/buzz-patterns.md)
- [Manifest permissions](reference/permissions.md)
- [AndroidManifest line-by-line](reference/manifest.md)
- [Build flavors & signing](reference/build-flavors.md)
- [ProGuard / R8 rules](reference/proguard.md)
- [Icons palette](reference/icons.md)
- API map by package:
  - [`core`](reference/api/core.md)
  - [`ai`](reference/api/ai.md)
  - [`os`](reference/api/os.md)
  - [`ui`](reference/api/ui.md)
  - [`update`](reference/api/update.md)

### Contributing

- [Getting started as a contributor](contributing/getting-started.md)
- [Coding standards](contributing/coding-standards.md)
- [PR process](contributing/pr-process.md)
- [Testing](contributing/testing.md)
- [Release process](contributing/release-process.md)
- [Good first issues](contributing/good-first-issues.md)
- [GitHub achievements, honestly earned](contributing/achievements.md)

### Security

- [Threat model](security/model.md)
- [Reporting a vulnerability](security/reporting.md)
- [Known limits — things this app can't protect against](security/known-limits.md)
- [Dependency policy](security/dependencies.md)
- [Code signing](security/signing.md)

### Architecture Decision Records

Decisions we made and why, in the moment, so future contributors don't have to reverse-engineer intent.

- [0001 — Record architecture decisions](adr/0001-record-architecture-decisions.md)
- [0002 — Use Jetpack Compose for all UI](adr/0002-use-jetpack-compose.md)
- [0003 — Hand-wired DI, no runtime DI library](adr/0003-hand-wired-di.md)
- [0004 — No telemetry, ever](adr/0004-no-telemetry.md)
- [0005 — GitHub Releases as the update channel](adr/0005-github-releases-as-update-channel.md)
- [0006 — material-icons-extended as a dependency](adr/0006-material-icons-extended.md)
- [0007 — `minSdk = 29` to match Quest 2's first API level](adr/0007-min-sdk-29.md)

### Reverse-engineering notes

Where the design intuition came from — the Meta OTA analysis that preceded this app.

- [Overview](reverse-engineering/overview.md)
- [OTA format](reverse-engineering/ota-format.md)
- [AVB chain of trust](reverse-engineering/avb-chain.md)
- [Meta auth flow](reverse-engineering/meta-auth-flow.md)
- [Hosts glossary](reverse-engineering/hosts-glossary.md)

### Roadmap

- [v0.1.0 — shipped](roadmap/v0.1.0.md)
- [v0.2.0 — next](roadmap/v0.2.0.md)
- [v1.0.0 — the north star](roadmap/v1.0.0.md)

### FAQ

- [FAQ](faq.md)

## Writing conventions

- **One file per concept.** If you find yourself writing "see also…" to the same page three times, that page should be split or linked from the index instead.
- **Code blocks** are triple-backtick fenced with a language tag so GitHub + mkdocs highlight them.
- **Internal links** are relative paths from the current file — nothing absolute.
- **Line length** is wrap-at-will — prose, not code — because Markdown re-wraps on render and short lines show up as awkward in some viewers.
- **No emoji in headings.** Rendering is spotty on some platforms.
- **Every doc has a "last reviewed" footer** when it covers something version-dependent. Stub docs that haven't been reviewed yet say so in italics at the top.

## Building the docs as a site

The docs ship as plain Markdown; if you want them as a navigable site, [mkdocs-material](https://squidfunk.github.io/mkdocs-material/) will render the same files unchanged:

```bash
pip install mkdocs-material
mkdocs serve        # live preview at http://127.0.0.1:8000
mkdocs build        # static site under site/
```

A minimal `mkdocs.yml` ships in the repo root. We don't publish to GitHub Pages yet — flip the switch in Settings → Pages when the time comes.
