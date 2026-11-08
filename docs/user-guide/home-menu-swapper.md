# Home Menu Swapper panel

Pick what launches when you take the headset off standby.

## How Android resolves "home"

The intent `android.intent.category.HOME` is resolved to an Activity. On AOSP Android, first long-press on "home" picks a default; on retail Quest 2 the resolver is hard-wired to `com.oculus.shell`.

With root, `cmd package set-home-activity <pkg>/<Activity>` changes the resolution. QuestClassic registers itself as a HOME resolver in its manifest.

## Candidates

- **QuestClassic nostalgic home** — this app's own launcher surface
- **QuestClassic AR home** — straight into passthrough with the semicircle hand menu
- **Meta VrShell** — the stock shell (requires network)
- **Quill (if installed)** — "always-on canvas" setup
- **Any sideloaded HOME-category activity** — discovered dynamically via `PackageManager.queryIntentActivities`

## Root gating

Setting the default home activity requires root (to write to `/data/system/users/0/home_activity.xml`). Without root the panel shows the plan; you can run `cmd package set-home-activity ...` via ADB as root.

## Related

- [Boot animation panel](boot-animation.md)
- [Strip Meta panel](strip-meta.md)
- [ADR 0009 — Root gating philosophy](../adr/0009-root-gating-philosophy.md)
