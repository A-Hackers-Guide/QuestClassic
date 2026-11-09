<!--
Thanks for contributing. Keep this template short — reviewers look at the
code; this is where WHY lives.
-->

## What this PR does

<!-- One or two sentences. Explain in terms of user behaviour where possible
     ('AI panel can now resume a conversation across restart'); fall back
     to component behaviour where not ('AIRepository persists per-backend
     wake names'). -->

## Why

<!-- The problem the change solves, or the user ask that motivated it.
     If this is the first follow-up on a thread, link the root cause /
     the earlier PR; if it's a response to a review comment, link that. -->

## How this PR is tested

- [ ] Unit tests (what do they cover?)
- [ ] CI build passed on this branch
- [ ] Manual run on a real headset (describe what you did and what you saw)
- [ ] N/A — docs or workflow only

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
