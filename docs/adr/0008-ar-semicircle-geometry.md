# ADR 0008 — AR semicircle hand menu uses a 90° arc, not a radial ring

Status: Accepted · 2026-11-07

## Context

The Quest hand-tracking API delivers 26 joint poses per hand at ~60 Hz. The question is how to lay out an "app menu" around the hand so the user can select with the other hand without first learning a gesture vocabulary.

Three options we considered:

1. **Radial ring** — a full 360° circle around the palm. Looks slick in demos; miserable for selection because half the slots sit *inside* your arm.
2. **Vertical stack** — a floating column above the palm. Easy, but it eats a lot of vertical FOV and the farthest slot is 40° above neutral gaze.
3. **90° arc** — a quarter-circle fan pointing away from the body, across a horizontal plane. Natural ergonomics, no "inside the arm" slots, all slots in the comfortable 20° cone around the gaze normal.

## Decision

We ship the 90° semicircle, swept from −45° to +45° around the hand-pointer's world-space forward vector, at a radius of 15 cm from the palm center.

### Geometry details

- **Pivot**: midpoint of the thumb MCP and pinky MCP joints (the "palm center" pseudo-joint).
- **Arc plane**: the plane normal to the vector between palm-center and the user's head position, so the arc always faces the user as the hand rotates.
- **Slots**: up to 8, evenly distributed; 6 is the default (menu length).
- **Slot collider**: a 2.5 cm sphere; selection fires when the *other* hand's index-fingertip enters the sphere and the other hand is in a "pointing" pose (fingers 2-5 curled > 60°).
- **Dismissal**: palm rotates away from the user by more than 60°, or the user explicitly touches a "close" slot at the arc endpoint.

### What this ADR is NOT committing to

- The AR passthrough plumbing (still in `issue #9`).
- The hand-tracking driver choice (Meta XR SDK vs. OpenXR direct).
- The 2D fallback render — already in `ARSemicirclePanel.kt`.

## Consequences

- Flat version renders inside the nostalgic home surface as a design preview.
- World-space version is the work of `issue #9` and ADR 0010 (TBD, driver choice).
- Tests can live against the pure geometry calculation (take palm + head pose, return 8 slot world positions) without needing a headset.

## Alternatives rejected

- **270° ring**: looked "cooler" but rejected because sideways slots require head rotation to see.
- **Icon-grid in air**: fine for 2D but doesn't match the "hand as menu" intent of the design brief.
