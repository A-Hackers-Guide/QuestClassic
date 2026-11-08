# PR process

Fork, branch, commit, push, open PR, address review, merge.

## Branch names

`feat/thing-you're-adding`, `fix/thing-you're-fixing`, `docs/...`, `chore/...`, `refactor/...`. Not `patch-1` or `main` on your fork.

## Commit messages

Imperative mood ("feat: add widget" not "feat: added widget"). Optional scope. Description sometimes helpful; the diff usually says what.

```
feat(ai): add Gemini backend

Implements AIBackend against the Gemini v1beta API. User supplies
their own Google AI Studio key in the Configure form.
```

Signed-off / co-authored-by trailers welcome.

## Opening the PR

Title = one-line summary (what the changelog line would say).

Body = the four-section standard:

- **What** — plain English.
- **Why** — the motivation / the issue it closes.
- **How** — the approach, if non-obvious.
- **Test plan** — what you did to verify.

If the PR touches a user-facing panel, include a short description of the before/after. Screenshots welcome.

## Review

- Solo maintainers right now, so "approved" is often "merged by author after green CI."
- CI must be green (both Build and, if the PR touches shipped behavior, any test that lands in [issue #12](https://github.com/A-Hackers-Guide/QuestClassic/issues/12)).
- If there are no reviewers, a 24-hour hold is a reasonable self-imposed delay to catch obvious issues.

## Merging

- **Squash merge** is the default. One PR = one commit on `main`.
- Don't merge on a red CI unless the breakage is unrelated and tracked.
- Delete the branch after merge.

## After merging

- Any issue referenced by `Closes #N` auto-closes.
- Release notes auto-generate from merged PR titles when we next tag `v*`.

## If you can't finish a PR

- Mark it a Draft (`gh pr ready -u`).
- Add a note. Someone may pick it up; no pressure.

---

_Last reviewed against v0.1.0._
