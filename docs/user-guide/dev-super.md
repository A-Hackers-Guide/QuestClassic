# Developer Super-Mode panel

Lifts QuestClassic (and developer-mode settings in general) to capabilities Android normally hides behind extra friction.

## Toggles

Each toggle names the exact system property or Settings key it flips. Toggle categories:

- **SETTINGS** — `settings put ...`; `android.permission.WRITE_SECURE_SETTINGS` needed.
- **PROP** — `setprop ...`; needs root or signature permission.
- **PKG** — package-level grants (`pm grant`, `appops set`).

## Representative set

- Allow sideload from any source (loosens package-verifier)
- Full ADB over Wi-Fi without re-pair
- Keep screen awake while charging
- Enable layout bounds (overlay layout grid)
- Allow mock location providers (OpenXR anchor spoofing in tests)
- Lift SELinux to permissive (temporary; reverse after)
- Grant QC `WRITE_SECURE_SETTINGS`
- Grant QC `WRITE_SETTINGS`
- Grant QC `MANAGE_EXTERNAL_STORAGE`
- Disable Meta's device-reset enforcement nag

## Root gating

PROP toggles need root (or signature-level). PKG toggles need either root or ADB with `pm grant` — that's a user-performable step. SETTINGS toggles work with `WRITE_SECURE_SETTINGS` already granted (which you get via ADB without root).

So the panel is tiered: PKG + SETTINGS work on any developer-enabled device, PROP toggles need root.

## Why some of these are dangerous

- `setenforce 0` breaks the retail security policy. Reverse it after you're done debugging.
- `MANAGE_EXTERNAL_STORAGE` lets QC walk any directory; keep it on only when picking firmware files.
- `WRITE_SECURE_SETTINGS` lets QC flip any Settings.Global key — not just ours.

## Related

- [ADR 0009 — Root gating philosophy](../adr/0009-root-gating-philosophy.md)
