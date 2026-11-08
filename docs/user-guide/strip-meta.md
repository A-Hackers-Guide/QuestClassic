# Strip Meta panel

Dry-run plan + root-gated action for debloating a Quest 2 of Meta-origin packages.

## What you see

A card per Meta-origin package, tagged with the planned verb:

- **REMOVE** (green) — uninstall at user 0. Safe to remove; does not block boot.
- **DISABLE** (amber) — `pm disable-user`; removable in theory but used by system intents, so disabling keeps intents resolvable.
- **LEAVE** (red) — removing it blanks the compositor, tracking, or launcher. The app will not propose removing these.

Each entry shows the exact shell command that would run. Even on a stock headset the plan is useful as documentation.

## Root gating

The top of the panel displays a `RootGateCard` describing what QuestClassic detected about the device's root state. See [ADR 0009](../adr/0009-root-gating-philosophy.md) for why we detect-but-never-exploit.

On stock retail firmware, the destructive action is a no-op: you can read the plan and run the commands yourself via ADB if you choose.

## Trimmed set (default)

| Package | Verb | Reason |
|---|---|---|
| `com.oculus.horizon` | REMOVE | Social; no boot dependency |
| `com.oculus.store` | REMOVE | Storefront; not needed for sideload-only |
| `com.oculus.browser` | REMOVE | Replaceable browser |
| `com.oculus.systemactivities` | DISABLE | Deep-link handler; disable keeps intents resolvable |
| `com.oculus.systemux` | DISABLE | Menus/popups; need a UI replacement first |
| `com.oculus.bugreporter` | REMOVE | Telemetry glue |
| `com.oculus.gazevision` | REMOVE | Gaze-targeted hints |
| `com.oculus.assistant` | REMOVE | Voice SDK runtime |
| `com.oculus.focus` | REMOVE | Entertainment app |
| `com.oculus.shell` | LEAVE | Launcher — replace before removing |
| `com.oculus.vrruntimeservice` | LEAVE | VR compositor |
| `com.oculus.vrdaemonservice` | LEAVE | HMD pose |

## Related

- [Firmware dump panel](firmware-dump.md)
- [Home menu swapper](home-menu-swapper.md)
- [ADR 0009 — Root gating philosophy](../adr/0009-root-gating-philosophy.md)
