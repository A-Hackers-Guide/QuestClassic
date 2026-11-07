---
title: "Panel: Package usage stats"
labels: [enhancement, panel]
---
Android exposes `UsageStatsManager` with aggregate launch/time stats per package. On Quest this is interesting because it shows which apps you actually used across the retro UI + HorizonOS.

### Needed

- `PACKAGE_USAGE_STATS` permission (special permission; user grants it in system Settings after the app prompts via `ACTION_USAGE_ACCESS_SETTINGS`).
- New panel `ui/panels/UsageStatsPanel.kt`.
- Show: top 20 apps by time-in-foreground over 7d, with little bars.

### Done when

- [ ] Permission request flow works (nice copy, deep-link to the exact settings screen).
- [ ] Shows real stats on a Quest with usage data.
- [ ] Gracefully states "no data" on a freshly-installed device.
