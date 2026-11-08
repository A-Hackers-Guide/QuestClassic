# API — `ui` package

Composables you'd want to call from a new panel. See [architecture/compose-ui.md](../../architecture/compose-ui.md) for the usage overview.

## `ui/theme/Theme.kt`

```kotlin
@Composable
fun QuestClassicTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
)

object QCColors {
    val Space: Color     // #07122A
    val Deep: Color      // #0E1E3F
    val Panel: Color     // #13274F
    val Edge: Color      // #1E3666
    val Teal: Color      // #34E5E7
    val Amber: Color     // #FFB347
    val Soft: Color      // #D7E2FF
    val Dim: Color       // #7F91B9
    val Warn: Color      // #FF6E6E
    val OK: Color        // #66E0A3
}
```

See [architecture/theming.md](../../architecture/theming.md) for the palette story.

## `ui/nostalgic/NostalgicComponents.kt`

Reusable pieces used across every panel.

```kotlin
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color = QCColors.Teal,
    content: @Composable () -> Unit,
)

@Composable
fun ChipLabel(text: String, color: Color = QCColors.Teal)

@Composable
fun PanelHeader(title: String, subtitle: String? = null, chip: String? = null)

@Composable
fun StatRow(label: String, value: String, ok: Boolean = true)

@Composable
fun TileButton(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
)
```

**Usage patterns:**

- `GlassPanel` — primary surface. Pick `accent` to signal state (Teal=default, Amber=caveat, OK=success, Warn=failure).
- `StatRow` — the key/value row with a status dot; use in a `Column` inside a `GlassPanel`.
- `TileButton` — only used on the home grid.

## `ui/panels/*.kt`

Each panel is a file: `LauncherPanel.kt`, `DiagnosticsPanel.kt`, `AIAssistantPanel.kt`, `BuildToolsPanel.kt`, `UpdatePanel.kt`, `SettingsPanel.kt`, `SignatureVerifierPanel.kt`, `SystemInternalsPanel.kt`, `MetaHostsAuditPanel.kt`.

Signature convention:

```kotlin
@Composable
fun MyPanel(container: AppContainer, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PanelClose(title = "My Panel", subtitle = "…", onClose = onClose)
        Spacer(Modifier.height(16.dp))
        // body
    }
}
```

`PanelClose` lives in `LauncherPanel.kt` (first file that needed it); it's module-visible and callable from any panel.

Register your panel:

1. Add to `HomeScreen` enum in `NostalgicHomeScreen.kt`.
2. Wire a `when` branch in the dispatch.
3. Add a `HomeTile` entry to `HomeTile.ALL`.

That's it. See [contributing/getting-started.md](../../contributing/getting-started.md) for the full add-a-panel walkthrough.

## `ar/ARHomeActivity.kt`

```kotlin
class ARHomeActivity : ComponentActivity()

@Composable
fun SemicircleHandMenu()
```

Currently a flat Compose preview. Real passthrough integration roadmap in [user-guide/ar-home.md](../../user-guide/ar-home.md).

---

_Last reviewed against v0.1.0._
