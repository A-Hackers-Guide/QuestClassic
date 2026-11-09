# Virtual monitor

Wirelessly streams desktop monitors from a paired computer into the headset as floating panels you can resize and move. **Status: scaffolded (M0)**; actual streaming requires the companion app (M3+ on its roadmap).

## What you see today

A slider showing an estimate of how many monitors this headset can realistically stream, based on your device's SoC class. The estimate is conservative by design — see [ADR 0021](../adr/0021-virtual-monitor-realistic-ceilings.md) for the numbers.

Tier ceilings (realistic / hard cap):

| Device                        | Realistic | Hard cap |
| ----------------------------- | --------- | -------- |
| Quest 1 / Go / Rift S         | 1         | 2        |
| Quest 2 / Quest Pro           | 3         | 4        |
| Quest 3 / Quest 3S            | 6         | 8        |
| Unknown-newer                 | 8         | 12       |

## Why not 20–100 like the brief asked?

Because no Quest-class SoC ships a video decoder path that keeps that many 1080p60 streams alive in parallel. Letting the slider go to 100 would quietly drop frames — which looks like a bug to you and is the app's fault for offering the setting. See [ADR 0021](../adr/0021-virtual-monitor-realistic-ceilings.md).

## When the actual streaming lands

The desktop companion sitting at `companion/` needs to:

- **M0** — bootable Rust daemon (done).
- **M1** — WebSocket frame transport (defined in `companion/PROTOCOL.md`).
- **M2** — mDNS discovery (shared types shipped, backend binding per-platform pending).
- **M3** — one-monitor WebRTC pipeline (next on the roadmap).
- **M4+** — multi-monitor + cursor + resize.

Follow `companion/ROADMAP.md` for the current milestone.
