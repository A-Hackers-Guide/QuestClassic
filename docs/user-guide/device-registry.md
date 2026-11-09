# Device Registry

Metadata-only catalog of the devices your QuestClassic talks to: paired desktops (companion hosts), Ollama servers, smart-home bridges, media servers, game servers.

## What's stored

Per device:

- `id` — unique ID (UUID in production)
- `displayName` — the name you see
- `kind` — one of: `COMPANION_DESKTOP`, `OLLAMA_SERVER`, `SMART_HOME_BRIDGE`, `GAME_SERVER`, `MEDIA_SERVER`, `OTHER`
- `address` — IP, mDNS name, or Bluetooth MAC
- `trusted` — explicit user OK (pairing step)
- `lastSeenEpochMs` — last successful ping

## What's NOT stored

- **Credentials.** API keys, passwords, tokens live under each feature's own credential store (e.g. `ai_config` for the AI companion's provider keys). The registry is pure metadata.
- **Protocol state.** Whether a device is currently connected, what streams are open, latencies — that's per-feature runtime state, not registry state.

## Limits

- **64-device cap**: the maximum tracked at once. `add()` above the cap throws so UI stays responsive.
- **Add-of-existing-id is an update**: the registry de-dupes by `id`. Calling `add(d)` when `d.id` is already present overwrites the row rather than making a duplicate. Separate `rename` / `setTrusted` / `touch` methods exist for narrower updates.

## Where it lives

`DataStore (Preferences)` at:

```
/data/data/io.github.questclassic.app/datastore/device_registry.preferences_pb
```

One row, `devices`, containing the JSON-encoded list. Excluded from Android's cloud backup by `data_extraction_rules.xml`.

## API — short form

```kotlin
val registry = container.devices

suspend fun examples() {
    registry.add(Device(id = "nova-desktop", displayName = "Living Room Mac",
                        kind = DeviceKind.COMPANION_DESKTOP, address = "nova.local"))
    registry.setTrusted("nova-desktop", true)
    registry.touch("nova-desktop", epochMs = System.currentTimeMillis())
    registry.rename("nova-desktop", "Big Mac")
    val all = registry.load()
    registry.remove("nova-desktop")
}
```

## Future surfaces

- **Virtual Monitors** will consume `COMPANION_DESKTOP` entries during mDNS discovery (companion M1).
- **AI Companion** will consume `OLLAMA_SERVER` entries to drive a devices-picker in the Ollama-backend setup form.
- **Programmable AI** will let your agent enumerate every trusted device via a safe read-only API.
