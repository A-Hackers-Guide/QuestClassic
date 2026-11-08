# First launch

What happens the first time you open QuestClassic on the headset, and what you can do in the next 10 minutes.

## What you see

A dark navy background with a big "SHELF" masthead and a 2×4 grid of tiles:

| Tile | What it opens |
|---|---|
| Launcher | Every app on the headset in a retro grid |
| Diagnostics | Runs 6 self-tests and buzzes the controllers on any fail |
| AI Companion | Setup form the first time, chat UI after |
| Build Tools | Programmable HUD |
| Updates | Checks GitHub for a newer QuestClassic |
| About / System | Device details, the experimental AR Home launcher |
| Signature Verifier | Pick an OTA .zip, verify its Meta signatures |
| System Internals | Live view of HorizonOS quirks |
| Meta Hosts Audit | Resolve + ping all 24 Meta hosts |

Tap any tile; tap "Close" (top right of the panel) to return.

## Suggested first lap

1. **About / System** — confirm the app can see your device correctly. If this says "Non-Quest Android device" you're probably running it on a regular Android phone for development (that's fine).

2. **Diagnostics** — press "Re-run" once. You'll hear the controllers buzz at the end — three short pulses if everything passed, a specific pattern if anything failed. Then try the **Build Tools → Haptic test** widget to feel every pattern deliberately.

3. **System Internals** — scroll through. The amber-tinted sections ("Security patch gap", "Build-host leak") are findings we surfaced during reverse-engineering. On a Quest 2 running the 2026 builds, the vendor SPL gap is typically ~8 years.

4. **Meta Hosts Audit** — tap and watch the categorized list populate. If "Internal" hosts are NOT reachable, that's correct and expected. If any of them resolve, that's a surprise worth investigating.

5. **AI Companion** — first launch shows a config form. Give your assistant a name, pick a backend, paste an API key you already have, write a system prompt. Save → chat.

6. **Updates** — hit it once. Should say "You're on the latest."

## Giving permissions

Depending on the panels you use, Android may ask for permissions:

- **Install unknown apps** — needed once for the Updates panel's "Install" button to work. Grant to QuestClassic in Settings → Apps → Install unknown apps → QuestClassic.
- **Query all packages** — declared in the manifest; granted automatically on install.
- **Internet, network state, vibrate, wake lock** — normal permissions, no prompt.

Explicit list in [reference/permissions.md](../reference/permissions.md).

## Where state lives

- AI Companion config (name, backend, API key, system prompt) → DataStore at `/data/data/io.github.questclassic.app/datastore/ai_config.preferences_pb`. Excluded from Android cloud backup by `data_extraction_rules.xml`.
- Nothing else is persisted between launches in v0.1.0. Quick note is intentionally ephemeral.

## Next steps

- [User guide](../user-guide/launcher.md) — read about each panel in depth.
- [contributing/getting-started.md](../contributing/getting-started.md) — if you want to write your own panel.
- [Troubleshooting](troubleshooting.md) — if something isn't right.

---

_Last reviewed against v0.1.0._
