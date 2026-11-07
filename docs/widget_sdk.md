# Widget SDK (Build Tools HUD)

The Build Tools panel is a programmable HUD built from "widgets." Each widget is a tiny Kotlin class with one `@Composable render()` function.

## Adding a widget

1. Open `ui/panels/BuildToolsPanel.kt`.
2. Add a new entry to the `Widget` enum:
   ```kotlin
   MY_WIDGET("My widget", @Composable { MyWidget() }),
   ```
3. Write the `@Composable fun MyWidget()` body somewhere in the same file. Example:
   ```kotlin
   @Composable
   private fun BatteryWidget() {
       val ctx = LocalContext.current
       val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
       var pct by remember { mutableIntStateOf(0) }
       LaunchedEffect(Unit) {
           while (true) {
               pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
               delay(5000)
           }
       }
       Text("Battery $pct%", color = QCColors.Soft)
   }
   ```
4. Open a PR. Keep each widget `<= 60` lines; if it needs more, promote it to its own file under `ui/panels/widgets/`.

## Widget rules

- **No background services.** A widget renders when its tile is on-screen and stops when it leaves. If you need periodic work, use `LaunchedEffect` + `delay`, scoped to the Composable.
- **No networking outside `OkHttpClient`.** Use `container.http` so retry/timeout behavior is consistent.
- **No permissions the main app doesn't already request.** If your widget needs a new permission, land a PR adding it to the manifest first (with a rationale).
- **No analytics.** Ever.

## Pre-built widgets

| Name | What it shows |
|---|---|
| Clock | Live weekday + HH:MM:SS |
| Fingerprint | `android.os.Build.FINGERPRINT` |
| Free memory | *(stub — to land)* live `MemoryInfo.availMem` |
| Network ping | *(stub — to land)* round-trip to a user-chosen URL |
| Quick note | *(stub — to land)* ephemeral scratch pad, stored in DataStore |
| Haptic test | *(stub — to land)* fire arbitrary buzz patterns from the HUD |

Each "to land" is a good first PR. Opening them against this repo helps on the GitHub Pull Shark achievement — see `achievements.md`.
