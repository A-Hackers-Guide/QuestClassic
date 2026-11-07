---
title: Roadmap
labels: [meta, roadmap]
---
Tracking issue for the near-term shape of QuestClassic. Each bullet becomes its own issue when picked up.

### Panels

- [ ] [AR Home — real OpenXR + passthrough + hand-tracking integration](../plans/issues/05-openxr-ar-integration.md)
- [ ] Package Usage Stats panel (surface Android's `UsageStatsManager` for Quest apps the user has launched)
- [ ] Audio Session visualizer (list active AudioRecord/AudioTrack sessions)
- [ ] Hand-tracking visualizer (use OpenXR's `XR_EXT_hand_tracking` to draw the skeleton)
- [ ] Capabilities inspector (read what Meta's `CapabilitiesDatabase` says the current user can do on each app, where accessible)

### Build Tools widgets

- [ ] Persistent Quick note (DataStore-backed, with export)
- [ ] GPU frame time (Choreographer frame-callback graph)
- [ ] Wi-Fi RSSI + channel
- [ ] Controller battery per-side

### AI backends

- [ ] Google Gemini (good-first-issue)
- [ ] Mistral (good-first-issue)
- [ ] Groq (good-first-issue)
- [ ] Local llama.cpp via REST (optional LAN)

### Infrastructure

- [ ] Signed releases via GitHub Secrets (document the keystore-setup flow end-to-end)
- [ ] Unit tests for `PayloadVerifier` using the fixtures from `tools/`
- [ ] F-Droid packaging
