# Compose UI — what we use and what we don't

Jetpack Compose is the only UI toolkit in the project. No XML layouts, no Fragments beyond what Compose needs under the hood, no view binding, no databinding. Rationale in [ADR 0002](../adr/0002-use-jetpack-compose.md).

## What we use

- **`setContent { }`** in `MainActivity` as the single entry point to the UI.
- **Material3** (`androidx.compose.material3`) for `Button`, `OutlinedTextField`, `Text`, `MaterialTheme`.
- **Material icons extended** (`androidx.compose.material.icons.outlined.*`) for tile icons.
- **Compose Foundation** for layout + gestures: `Box`, `Row`, `Column`, `LazyColumn`, `LazyVerticalGrid`, `Modifier.clickable`, etc.
- **Compose Runtime** state: `remember`, `mutableStateOf`, `mutableIntStateOf`, `mutableStateListOf`, `LaunchedEffect`, `rememberCoroutineScope`, `derivedStateOf` where relevant.
- **Activity Compose** for `rememberLauncherForActivityResult`.
- **Navigation** — we do it ourselves with a `HomeScreen` enum + `when` dispatch (`NostalgicHomeScreen.kt`). No `Navigation Compose` for v0.1.0; we'll consider it when the panel count passes ~15.

## What we don't use

- **XML layouts.** Zero layout files under `res/layout`.
- **View Binding / Data Binding.** Not enabled.
- **Fragments.** Zero direct usage; Compose uses them internally under `ComponentActivity` but we never write one.
- **Compose Preview `@Preview`** on most components — the whole app runs on the headset, not on previews. We'd add previews if we started hitting Studio for UI iteration.
- **Fancy Compose libraries**: no Accompanist (deprecated anyway), no Coil (no images beyond app icons via `BitmapDrawable`), no Compose Charts.

## Our reusable Composables

All in `ui/nostalgic/NostalgicComponents.kt`:

- **`GlassPanel(modifier, onClick?, accent) { content }`** — the primary surface card with the dark navy gradient and teal accent border. Every panel uses it.
- **`ChipLabel(text, color)`** — small pill-shaped status chip.
- **`StatRow(label, value, ok)`** — key/value row with a green/red indicator dot.
- **`PanelHeader(title, subtitle, chip?)`** — the "label + title + optional chip" masthead at the top of each panel.
- **`TileButton(icon, title, description, onClick)`** — the 2×N home-grid tile.

In `ui/panels/LauncherPanel.kt`:

- **`PanelClose(title, subtitle, onClose)`** — close affordance at the top of every panel. (Lives in LauncherPanel.kt because that's the first panel that needed it; refactoring out into NostalgicComponents is tracked informally.)

## Composable naming

- Top-level panels are `*Panel`.
- Private helpers inside a panel are lowercase local functions or `@Composable` named `AppTile`, `AIConfigForm`, `AIField`, `ClockWidget`, etc.
- Everything in `ui.nostalgic` is public to the module so panels can reuse.

## State management

- **Local UI state** (`remember { mutableStateOf(…) }`) lives in the Composable that owns it.
- **Repository-backed state** flows in through `AppContainer` (which is in turn remembered at the `MainActivity` level, passed down as a param).
- **No ViewModels** anywhere in v0.1.0. `rememberCoroutineScope` + `LaunchedEffect` cover everything. We'll add `viewModel()` when a panel's state has to survive config changes (e.g. after real AR integration).

## Theming

See [architecture/theming.md](theming.md). Short version:

- Force dark scheme always — this is a VR shell app; no light mode makes sense inside a headset.
- Palette lives in `ui/theme/Theme.kt` as `QCColors`.
- Typography uses only system fonts (`FontFamily.SansSerif` / `Monospace`) — no font downloads, no Google Fonts call at runtime.

## Known rough edges

- `NetworkPingWidget` in `BuildToolsPanel.kt` uses a hand-rolled `CoroutineScope(Dispatchers.IO).launch` — should move to `rememberCoroutineScope` for proper cancellation on nav-away. Tracked as a cleanup PR candidate.
- `AIAssistantPanel`'s input field doesn't show a send-on-enter — Quest keyboards don't send IME actions reliably and we haven't wired an alternative. Logging ticket worth filing.

---

_Last reviewed against v0.1.0._
