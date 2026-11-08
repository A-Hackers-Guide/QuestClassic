# Boot Animation panel

Swap the Meta startup animation for a nostalgic one (or revert).

## How Android boot animation works

A specially-named zip at `/system/media/bootanimation.zip`. Inside: a `desc.txt` that names play speed + frame directories, plus PNGs in each directory. The AOSP `bootanimation` binary parses desc.txt and replays frames in order on the primary display at boot.

QuestClassic's swap writes a new zip over that path (after `mount -o remount,rw /system`), keeps a backup of the original at `/data/local/tmp/bootanimation.meta.zip`, then remounts read-only.

## Bundled replacements (all MIT-licensed, clean-room art)

- **Classic Oculus Rift CV1 homage** — swirling teal orb
- **Terminal Boot** — ANSI boot log, green on black
- **QuestClassic Cyan** — minimal cyan QC mark, 1.5s loop
- **Dust Particle Field** — drifting dust, no text

## Root gating

`/system/media/` is read-only on sealed stock. On rooted builds the swap is one remount + copy + chmod. The panel shows the exact commands before running them. See [ADR 0009](../adr/0009-root-gating-philosophy.md).

## Revert

Picking "Revert to Meta original" writes the backup at `/data/local/tmp/bootanimation.meta.zip` back over the system file. The backup is created on the first swap and preserved thereafter.

## Why the backup

Meta's bootanimation isn't distributable by us — the frames are Meta IP. The backup is created from the user's own device and kept on the user's own device.

## Related

- [Home menu swapper](home-menu-swapper.md)
