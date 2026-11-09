# QuestClassic Companion — wire protocol

Over `ws://<host>:48122/ws` by default (configurable via `companion.config.toml`). TLS on `wss://` is planned but not required on the local-only path.

All frames are JSON, one frame per websocket message. The `op` field discriminates.

## Quest → Companion

### `HELLO`

First message after socket open. The companion replies with `WELCOME` before accepting anything else.

```jsonc
{
  "op": "HELLO",
  "version": 1,
  "headsetId": "uuid-v4",
  "headsetModel": "Quest 2",
  "displays": 2,            // how many virtual monitors the headset is asking for
  "capabilities": [
    "h264",                 // video codecs it can produce
    "h265",
    "opus"                  // audio codecs it can consume
  ]
}
```

### `FRAME`

One encoded video frame. `displayId` matches the display the companion allocated in its `DISPLAY_READY`.

```jsonc
{
  "op": "FRAME",
  "displayId": 0,
  "ts": 1731080000123,     // Quest's monotonic ms timestamp
  "keyframe": false,
  "codec": "h264",
  "payload": "<base64 encoded NAL units>"
}
```

### `INPUT`

A single user-input event. Discriminated further by `kind`.

```jsonc
// Mouse move — coords are in the virtual display's pixel space
{ "op": "INPUT", "kind": "mouse_move", "displayId": 0, "x": 640, "y": 360 }

// Mouse button
{ "op": "INPUT", "kind": "mouse_button", "button": "left", "down": true }

// Mouse wheel (both axes)
{ "op": "INPUT", "kind": "mouse_wheel", "dx": 0, "dy": -1 }

// Keyboard
{ "op": "INPUT", "kind": "key", "scancode": 30, "down": true }

// Multi-touch (post-M7)
{ "op": "INPUT", "kind": "touch", "pointerId": 0, "x": 100, "y": 200, "phase": "down" }
```

### `GOODBYE`

Clean shutdown. Companion tears down displays + flushes pending events.

```jsonc
{ "op": "GOODBYE", "reason": "user quit" }
```

## Companion → Quest

### `WELCOME`

Response to `HELLO`. Advertises how many displays the host can actually allocate and which input kinds it can inject.

```jsonc
{
  "op": "WELCOME",
  "companionId": "uuid-v4",
  "hostOs": "macos" | "windows" | "linux" | "freebsd",
  "hostOsVersion": "14.6",
  "maxDisplays": 4,
  "osCaps": ["mouse", "keyboard", "wheel", "touch"]
}
```

### `DISPLAY_READY`

Companion has created a virtual display and is ready to receive `FRAME`s for it.

```jsonc
{
  "op": "DISPLAY_READY",
  "displayId": 0,
  "width": 1920,
  "height": 1080,
  "refreshHz": 60,
  "scale": 1.0,
  "colorSpace": "srgb"
}
```

### `DISPLAY_GONE`

The host side removed a display (user unplugged, driver reset, etc.).

```jsonc
{ "op": "DISPLAY_GONE", "displayId": 0, "reason": "driver-reset" }
```

### `CURSOR`

Host-side cursor position echoes back to the Quest for the on-headset reticle.

```jsonc
{ "op": "CURSOR", "displayId": 0, "x": 500, "y": 300, "visible": true }
```

### `ERROR`

Non-fatal error with a human-readable message.

```jsonc
{ "op": "ERROR", "code": "no_accessibility", "detail": "macOS Accessibility permission not granted; input injection disabled." }
```

## Protocol versioning

`HELLO.version` is a uint. The companion refuses connections with a `version` it doesn't understand; the Quest should downgrade to the highest version the companion accepted. We bump the version only when a field changes meaning or disappears.

## Why websocket instead of QUIC / WebRTC for control

Websocket is already open on both sides (Quest has OkHttp + WebSocket; Tauri has Axum). It carries the handshake + input + metadata cheaply. The actual video payload moves to WebRTC in M4, under a separate connection negotiated through this websocket.
