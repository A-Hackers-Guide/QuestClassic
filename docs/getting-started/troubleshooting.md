# Troubleshooting

In order of "most common first."

## `adb devices` says `unauthorized`

You didn't accept the USB-debugging prompt on the headset. Put the headset on, you'll see it immediately — tap **Always allow from this computer**, then **OK**. Re-run `adb devices`; should say `device`.

## `adb devices` is empty

- Cable isn't a data cable. Try another USB-C cable (not the charger-only one that came with your headset).
- USB debugging isn't enabled on the headset — walk back through [install.md](install.md) step 2.
- Developer mode isn't on for your account — [enable-developer-mode.md](enable-developer-mode.md).
- adb server is confused — `adb kill-server && adb start-server` fixes it 80 % of the time.

## `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

You have a previous QuestClassic installed with a different signing key. Reinstall over it:

```bash
adb uninstall io.github.questclassic.app
adb install app-release-unsigned.apk
```

You'll lose your DataStore (AI config). That's fine on a first-install drift; annoying later when we start publishing *signed* releases alongside the unsigned ones. Once signed releases are stable (see [issue #11](https://github.com/A-Hackers-Guide/QuestClassic/issues/11)), pick signed-or-unsigned-and-stick-with-it.

## `INSTALL_FAILED_VERSION_DOWNGRADE`

You're trying to install an older version over a newer one. Add `-d`:

```bash
adb install -r -d app-release-unsigned.apk
```

## App installs but doesn't appear in the headset Library

Look in **Library → Apps → ▾ dropdown → Unknown Sources**. Meta hides sideloaded apps there by default.

## App opens but the UI is 2D, not immersive

Correct. QuestClassic v0.1.0 is a 2D panel app that runs inside your Horizon home. The AR Home activity is the one experimental immersive surface; the rest live in the standard Quest 2D-panel window. The nostalgic retro look is intentional — early Oculus Home was 2D panels too.

## Diagnostics buzzed but I don't know what the pattern meant

Count the beats: short = dot, long = dash. Match against [reference/buzz-patterns.md](../reference/buzz-patterns.md). Common ones:

- `. . .` (three short) — self-test passed.
- `- .` (dash, dot) — battery below 10 %.
- `- . - .` — no validated network connection.

## Signature Verifier says "insane manifest_size" / "unsupported payload version"

The file you picked isn't a Meta OTA, or it's a newer OTA format than we've tested. Verify the file is a `.zip`, contains `META-INF/com/android/otacert` and `payload.bin`, and came from Meta's own update servers.

## AI Companion returns an API error

- **401** — your key is wrong or expired.
- **429** — you hit a rate limit; wait a minute.
- **5xx** — provider's having a bad day; retry.
- The Ollama backend's base URL is wrong — it defaults to `http://10.0.0.1:11434`; change it in the setup form to whatever hosts your Ollama.

## Meta Hosts Audit says most things are unreachable

- The headset is offline — check Wi-Fi in Quest settings.
- Your network blocks ICMP (some corporate / school networks do). DNS resolution may still work; the "resolved" count being nonzero is the real health signal.

## Update panel never offers a newer version

Right-side of the issue: either you ARE on the latest, or the self-updater can't reach `api.github.com`. Open the Meta Hosts Audit panel — if `scontent.xx.fbcdn.net` is unreachable too, you're probably offline.

## "Install" button in Updates does nothing

Grant QuestClassic the "Install unknown apps" permission: Settings → Apps → Install unknown apps → QuestClassic → **Allow**. The first update request ought to pop a system prompt; if it didn't, do it manually.

## Something else

Open an issue using the [bug-report template](https://github.com/A-Hackers-Guide/QuestClassic/issues/new/choose) — it asks for your headset model, HorizonOS build (visible in System Internals panel), QuestClassic version, and any buzz patterns that fired.
