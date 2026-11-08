# Docs site (GitHub Pages)

The repo's docs render as a static site at `https://a-hackers-guide.github.io/QuestClassic/`.

## How it's built

- Source: everything under `docs/` + the root `README.md`, with the nav in `mkdocs.yml`.
- Theme: `mkdocs-material` 9.5.44 (pinned in `.github/workflows/docs.yml`).
- Trigger: push to `main` that touches `docs/`, `README.md`, `mkdocs.yml`, or the workflow itself.
- Workflow: `.github/workflows/docs.yml` — two jobs, `build` then `deploy`.
- Deploy target: GitHub Pages, env `github-pages`.

## First-time repo setup

1. Repo → Settings → Pages → Source → "GitHub Actions".
2. That's it — the next push to `main` under `docs/` triggers a build and deploy.

## Local preview

```bash
pip install mkdocs-material==9.5.44
make serve-docs
# open http://127.0.0.1:8000
```

Or `mkdocs serve` directly.

## Strict mode

The workflow builds with `--strict`, so any:

- broken internal link,
- missing nav entry,
- Markdown that produces a warning

fails the build. That's deliberate; the docs are supposed to stay internally consistent.

## Adding a new page

1. Write the `.md` file somewhere under `docs/`.
2. Add it to `mkdocs.yml`'s `nav:` tree at the right spot.
3. Open a PR; CI will verify the link graph and the deploy preview.

## Known pain points

- **Changing `mkdocs.yml`'s theme palette breaks Pages cache for ~2 min.** Hard-refresh once if colors look stale.
- **Pages deploy needs the repo to have Pages enabled.** The first-time setup step above is manual (GitHub won't let the workflow enable it for you).
