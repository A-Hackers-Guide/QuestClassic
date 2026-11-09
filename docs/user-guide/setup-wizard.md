# Setup wizard

The first time QuestClassic opens, the Setup wizard walks you through five short steps.

It's not a license screen or a checklist of permissions. It's a chance to **see what works on this device before the main shell opens**, and to optionally wire an AI assistant you can address by name.

## The five steps

1. **Welcome.** Short intro to what QuestClassic does. No telemetry, no accounts. Click **Start**.
2. **Capabilities.** Explains what the app does on a *stock* Quest build (launcher, build-tools, AI, meta-hosts audit) vs. what needs root (passthrough camera control, firmware dump, home-menu swap). Click **Continue** when you've read it.
3. **AI.** Optionally paste an API key for Claude / Gemini / OpenAI, or point the app at an Ollama instance. You can also **Skip for now** — the AI panel will stay open and remind you later.
4. **Finish.** Setup complete; the main shell loads.
5. **Done.** The wizard stops showing. If you ever want to re-run it, open **About → Replay setup**.

## About the AI step

Each AI backend (Claude, Gemini, OpenAI, Ollama) keeps its own wake name and its own API key. Switching provider later in Settings preserves both. The default wake names are *Classic*, *Nova*, *Sage*, and *Local* respectively — you can change any of them to whatever you want to say out loud.

Keys are stored in app-private DataStore only. They never travel in any export — not in the Build Tools diagnostic bundle, not in the config backup JSON. If you share one of those files, you share settings and layout, not credentials. (See [ADR 0017](../adr/0017-config-backup-excludes-keys.md) for why.)

## Why linear, not a menu

The capability briefing is prerequisite reading for the AI step (you need to know what works on stock before you decide whether a key is worth the trouble). A menu that unlocks all three steps at once lets you make that decision without the context. The pipeline enforces it.

The state machine refuses non-adjacent jumps, backward moves, and self-loops. The only way back is **About → Replay setup**, which resets to BLANK. See [ADR 0018](../adr/0018-first-run-linear-pipeline.md).
