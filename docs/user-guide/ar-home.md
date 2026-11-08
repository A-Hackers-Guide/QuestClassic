# AR Home (preview)

An experimental activity that draws the 90° semicircular hand menu we want in AR. Right now it renders flat (2D Compose canvas) with a tick-based hover animation; the real passthrough + hand-tracking integration lands in a follow-up PR (see [issue #9](https://github.com/A-Hackers-Guide/QuestClassic/issues/9)).

## Design

Six tiles laid out on a 90° arc from −45° to +45°, origin anchored to where the user's palm will be in AR mode. Hover animation ticks through tiles at 1.4 s so you can see what the final interaction will feel like before it actually tracks hands.

```
        tile₀
       /
  tile₁
   \
    anchor (palm)
   /
  tile₂
       \
        tile₅
```

## Why 90°

- Research on wrist-anchored menus (Meta published some in 2023) shows 90° is the sweet spot across adult comfortable wrist-rotation range.
- Larger arcs force uncomfortable supination; smaller crowd the tiles.
- You can reach every tile with the opposite hand's pointer without re-orienting your head.

## Roadmap to a real AR mode

### Path A — Meta Spatial SDK (recommended)

1. Add `com.meta.spatial:spatial-core-android` + `spatial-scene-android` to `app/build.gradle.kts`. APK size bumps ~15 MB.
2. `ARHomeActivity extends SpatialActivity`; request passthrough on scene start.
3. Attach a `Panel` for `SemicircleHandMenu`; anchor it to the user's dominant hand pose. User-configurable in About.
4. Raycast from the opposite hand's pointer into the arc. On pinch, launch the hovered tile via `AppListRepository.launch`.
5. Palm-toward-face dismisses; palm-away summons.

### Path B — raw OpenXR + Vulkan (fallback)

1. Add `libopenxr_loader.so` as a runtime dep (already on the headset at `/apex/com.meta.xr/priv-app/VrDriver/VrDriver.apk!/lib/arm64-v8a/libopenxr_forwardloader.so`).
2. Vulkan context from `android.graphics.Surface` + `XR_KHR_vulkan_enable2`.
3. ~1 kLOC of C++/Kotlin-native interop for rendering + input.

Path A is where we'll go first; Path B is the fallback if Meta Spatial SDK terms become restrictive.

## Why the current preview matters

It locks in the geometry and interaction semantics before the big-dep cost. If the hover animation looks awkward at this stage, we fix it with cheap Compose iteration — not after shipping 15 MB of SDK.

## How to try it

- Open **About / System → Open AR Home**.
- On a Quest, it opens as a separate 2D-panel activity (no passthrough yet).
- On a regular Android phone/emulator, you get the same 2D preview.

## Related

- [issue #9](https://github.com/A-Hackers-Guide/QuestClassic/issues/9) — the real integration PR.
- [reference/api/ui.md § ARHomeActivity](../reference/api/ui.md#arhomeactivity) — the current code.

---

_Last reviewed against v0.1.0._
