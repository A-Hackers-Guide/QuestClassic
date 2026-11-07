# Repo bootstrap plans

Everything needed to publish this repo to GitHub and open the full set of PRs and issues in one command.

## One-shot publish

```bash
cd QuestClassic
bash .github/plans/bootstrap.sh
```

The script (`bootstrap.sh`) uses `gh` with your active auth (you're logged in as `A-Hackers-Guide` per `gh auth status`) to:

1. Create the public repo `A-Hackers-Guide/QuestClassic` and push `main`.
2. Push each feature branch under `feat/*`.
3. Open each PR with the pre-written body under `prs/*.md`.
4. Squash-merge each PR into `main` (good for `YOLO` + `Pull Shark` GitHub achievements).
5. Open every planned issue from `issues/*.md` with labels.
6. Tag `v0.1.0` — triggers `.github/workflows/release.yml` to build + publish a release APK.

## What's where

```
.github/plans/
├── bootstrap.sh          one-shot publish
├── README.md             this file
├── prs/
│   ├── 01-signature-verifier.md
│   ├── 02-system-internals.md
│   ├── 03-widget-implementations.md
│   └── 04-meta-hosts-audit.md
└── issues/
    ├── 01-roadmap.md         (big meta tracking issue)
    ├── 02-add-ai-backend.md  (good first issue)
    ├── 03-add-buzz-pattern.md (good first issue)
    ├── 04-add-widget.md       (good first issue)
    ├── 05-openxr-ar-integration.md
    ├── 06-package-usage-stats-panel.md
    ├── 07-signed-release-keystore.md
    └── 08-tests.md
```

## If you want to do it by hand

Each PR body is a self-contained Markdown file, ready to paste into GitHub's PR form. Each issue file has YAML-ish frontmatter (`title`, `labels`) + the body.

The four feature branches are already committed locally:

```
feat/signature-verifier
feat/system-internals
feat/widget-implementations
feat/meta-hosts-audit
```

all cut from `main` and each with a single, signed commit (semantic message + `Co-Authored-By` trailer).

## After it's up

- The Release workflow also patches `SelfUpdater.kt` at build time to point at `A-Hackers-Guide/QuestClassic`, so the APK that lands in the release can self-update from the same repo.
- The debug APK is unsigned-but-still-installable via `adb install`. To get a signed release, set up the four signing secrets per `issues/07-signed-release-keystore.md`.
