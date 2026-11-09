<!-- Fill each section. Thanks! -->

## What

<!-- One or two sentences. Explain in terms of user behaviour where
     possible ('AI panel can now resume a conversation across restart');
     fall back to component behaviour where not ('AIRepository persists
     per-backend wake names'). -->

## Why

<!-- Motivation, or the issue it closes. `Closes #N`. The problem the
     change solves, or the user ask that motivated it. If this is the
     first follow-up on a thread, link the root cause / the earlier PR. -->

## How

<!-- Approach if non-obvious. Can be short. -->

## Test plan

<!-- What you did to verify. Device, Cuttlefish, Compose preview, etc. -->

## Risk + rollback

<!-- What's the risk of this landing? For a pure-function + test PR this
     is often 'zero'. For a DataStore schema change: describe the
     migration path. For a change that touches the AI backend wire
     format: describe how a new version talks to an old build.

     If someone needs to revert this, is that a simple git revert, or
     is there state to clean up? -->

## Policy / ADR links

<!-- When the PR encodes a policy decision (which keys travel, which
     tools auto-run, what shape names are accepted), link the ADR.
     Policy claims in a PR body get out of date; ADRs are versioned. -->

---

- [ ] I ran `./gradlew :app:assembleDebug` locally and it succeeded.
- [ ] I tested the change on a headset or Cuttlefish instance.
- [ ] I updated docs if the change is user-visible or changes a public API.
- [ ] I added a `Co-Authored-By` trailer if this was pair-programmed.
- [ ] I'm okay with squash-merging.
