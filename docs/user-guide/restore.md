# Functionality Restore panel

Option 9 from the design brief: the "post / hardware check" escalation ladder.

## The ladder

1. **Ask** — "Do you notice any problems?" with a 30s timer.
2. **Attempt fix** — on no-answer or yes:
   - Recalibrate tracking origin
   - Re-bind controllers
   - Re-query displays
3. **Run self-tests** — if the fix attempt doesn't reveal the problem, run the full diagnostics engine.
4. **Display error** — if a failing check is found, show its name + value on screen.
5. **Fallback text** — if the main display surface is unreachable (compositor dead), show the error code on a plain fallback surface.
6. **Buzz** — if nothing can be rendered, buzz the controllers and headset in the pattern tied to the finding.

## Buzz pattern lookup

Each diagnostic code maps to a canonical pattern in [`docs/reference/buzz-patterns.md`](../reference/buzz-patterns.md). Users can decode a fault by feeling the pattern and matching it in the public repo.

## Why 30 seconds

A user who is trying the headset for the first time — or has just had one go wrong — needs time to look, feel, and consider before pressing. Short enough to respect their time, long enough not to race past them.

## Related

- [Diagnostics panel](diagnostics.md)
- [Buzz patterns reference](../reference/buzz-patterns.md)
