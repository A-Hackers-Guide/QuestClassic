# Good first issues

Pick one; comment "I'll take this"; open a PR.

## Live right now

- **[#6 — Add a new AI backend (Gemini / Mistral / Groq)](https://github.com/A-Hackers-Guide/QuestClassic/issues/6)** — one new file + one line in `AIRepository`. Ship-rateable in an evening.
- **[#7 — Add a new diagnostic buzz pattern](https://github.com/A-Hackers-Guide/QuestClassic/issues/7)** — one `BuzzPattern` constant + table row + wire-up. Scope is tiny.
- **[#8 — Add a new Build Tools widget](https://github.com/A-Hackers-Guide/QuestClassic/issues/8)** — any widget from the "to land" list in [the Build Tools user guide](../user-guide/build-tools.md). Scope is tiny.

## What makes a good first issue

- Scoped to a few files.
- Visible outcome — the user sees something change.
- Doesn't require Quest hardware to validate (AOSP Cuttlefish or any Android device works).
- No undocumented dependency on another incomplete feature.

## Not a good first issue

- OpenXR integration ([#9](https://github.com/A-Hackers-Guide/QuestClassic/issues/9)). Big. Load-bearing. Save for someone with Quest-dev experience.
- Signing keystore setup ([#11](https://github.com/A-Hackers-Guide/QuestClassic/issues/11)) — infrastructure-level, needs access to the repo's secrets.

## How to find more

Filter the issue list by label `good first issue`:

```
https://github.com/A-Hackers-Guide/QuestClassic/issues?q=is%3Aopen+label%3A%22good+first+issue%22
```

If the list is empty, that's a sign you should pick a `roadmap` bullet and split off a focused issue.

---

_Last reviewed against v0.1.0._
