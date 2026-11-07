---
title: "AR Home — wire up OpenXR + passthrough + hand-tracking"
labels: [enhancement, big, ar]
---
Today `ARHomeActivity` renders the 90° semicircle on a flat Compose canvas. To make it a real AR mode we need:

### Approach A (recommended) — Meta Spatial SDK

- Add `com.meta.spatial:spatial-core-android` + `spatial-scene-android` to `app/build.gradle.kts` (bumps APK size ~15 MB).
- Have `ARHomeActivity` extend `SpatialActivity`; create a passthrough-enabled scene.
- Attach a `Panel` for `SemicircleHandMenu` and anchor it to a hand pose (left or right — user-configurable in About).
- Raycast from the opposite hand; on pinch, launch the hovered tile via `AppListRepository.launch`.
- Palm-toward-face: dismiss; palm-away: summon.

### Approach B (fallback) — raw OpenXR + Vulkan

- Add `libopenxr_loader.so` as a runtime dep (already on the headset at `/apex/com.meta.xr/…/libopenxr_forwardloader.so`).
- Vulkan context from `android.graphics.Surface` + `XR_KHR_vulkan_enable2`.
- Write ~1 kLOC of C++/Kotlin-native-interop to render + input.

### Done when

- [ ] AR Home runs in passthrough on a Quest 2+ with hand-tracking on.
- [ ] Tapping a tile launches the panel's associated app.
- [ ] Documented in `docs/ar_mode.md`.
- [ ] Safe fallback if passthrough or hand-tracking isn't available (today's flat preview stays as the default).
