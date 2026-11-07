# AR mode

The "Open AR Home" button in the About panel starts `ARHomeActivity`, which renders a semicircular hand menu on a Compose canvas. Today that renders flat (because the real passthrough / hand-tracking integration needs the Meta Spatial SDK, which is a large native dependency we add in a follow-up).

## What's in the first build

- An `ARHomeActivity` with the right manifest flags (`singleTask`, `configChanges`, `resizeableActivity=false` so the Quest doesn't stuff us into a 2D panel).
- A Compose `SemicircleHandMenu` that lays out 6 tiles on a 90° arc from −45° to +45°, with a tick-based hover animation so you can see what the final AR interaction looks like.
- Quest manifest tags: `com.oculus.intent.category.VR`, `oculus.software.handtracking` (not required), `com.oculus.feature.PASSTHROUGH` (not required). Non-Quest Android devices run the 2D preview.

## What lands in the AR mode PR

1. Add the Meta Spatial SDK dependency to `app/build.gradle.kts` (large native lib; bumps APK size by ~15 MB).
2. Convert `ARHomeActivity` to extend `com.meta.spatial.core.SpatialActivity` and set up a passthrough environment.
3. Attach the `SemicircleHandMenu` to a hand pose (left or right — user-configurable in the About panel).
4. Wire a raycast from the opposite hand's pointer into the arc; on "pinch" gesture, launch the tapped app via `AppListRepository.launch`.
5. Draw a subtle "recall" gesture: flipping the palm toward you dismisses the menu; flipping it back summons it.

## OpenXR fallback (no Spatial SDK)

Minority path for devices that have OpenXR but not the Spatial SDK. We can render through pure OpenXR + Vulkan if the SDK path is blocked — it's more code (~1 kLOC) but zero external deps. Tracked as a separate follow-up PR.

## Why the menu is only 90°

Oriented so you don't have to turn your wrist more than quarter-rotation to reach any tile. Larger arcs (180° or full-circle) feel physically awkward on a Quest 2's tracking volume around the hand. 90° is the sweet spot across most Horizon social-app studies (Meta published some of this in their 2023 interaction research).
