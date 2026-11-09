# ADR 0026 — HistoryCompactor never drops context silently

## Status
Accepted (2026-11-09).

## Context

Long chat histories hit the backend's context window and slow response time. Three ways to shrink:

1. **Truncate**: just drop the oldest N messages. Fast; loses earlier context silently.
2. **Rolling summary** with a fixed buffer. Keeps earlier context; adds complexity.
3. **Summarize-older + keep-recent**. One summary, prepended as `role=system`.

Option 1 is dangerous: the user asks "what did we decide earlier?" and the AI confidently makes something up because the earlier turn was truncated. Option 2 is good but needs bookkeeping around what's already been summarized. Option 3 is simpler and good enough.

## Decision

`HistoryCompactor(triggerCount, keepRecent)`:

- At `history.size >= triggerCount` (default 80), fire on the next send.
- Pull out the older `history.size - keepRecent` messages.
- Hand them to an **injected** suspend callback `summarize(older) -> String?`. The compactor doesn't know HOW to summarize — the callback is typically `backend.chat(systemPrompt = "Summarize these messages…", older)`.
- Prepend the summary as a `role=system` message.

Three defensive rules:

- If `summarize` returns `null` or an empty string, the compactor is a **no-op**. Better to send 100 messages than to silently drop context when the summary call failed.
- Constructor rejects `triggerCount <= keepRecent` and `keepRecent < 2` — nonsensical configurations fail at wire time, not at first compact.
- Order of recent messages is preserved.

## Consequences

**Pros**
- Earlier context survives as a summary the model can cite.
- 9 JUnit tests cover both the normal path and the three "don't silently lose context" paths.
- Injectable callback keeps the compactor pure; tests don't need the network.

**Cons**
- One extra model call every compact. That's cheap compared to sending the full history every turn.

## Alternatives considered

1. **Silent truncation.** Rejected per context above.
2. **Rolling summary with high-water mark bookkeeping.** Deferred — more state; the simple "summarize-and-replace" covers 95% of long threads.
3. **Client-side summarization with a local small model.** Deferred — requires a local model we haven't shipped.
