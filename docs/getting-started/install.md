# Install QuestClassic on your Meta Quest

Five-minute path from a fresh headset to the app running in your Library.

## What you need

- A Meta Quest 2, Quest Pro, Quest 3S, or Quest 3 that's set up and signed in.
- A PC with `adb` installed (any OS; `brew install android-platform-tools` on macOS, `apt install adb` on Debian/Ubuntu, [SDK Platform Tools zip](https://developer.android.com/tools/releases/platform-tools) on Windows).
- A USB-C cable that supports data (not just charging).
- A Meta Developer account — free, takes 2 minutes. See [enable-developer-mode.md](enable-developer-mode.md).

## Step 1 — Enable Developer Mode

If you've never put a sideloaded app on your headset: follow [enable-developer-mode.md](enable-developer-mode.md), then come back here. This is a one-time thing per account.

## Step 2 — Enable USB debugging on the headset

With the headset on and awake:

1. Open **Settings → System → Developer**.
2. Turn on **USB Connection Dialog**.
3. Turn on **ADB**.

## Step 3 — Plug in and authorize

Plug the USB-C cable into your PC and then into the headset. Put the headset on — you'll see a dialog asking if you want to **Allow USB debugging** from this computer. Tick **Always allow** and accept.

Confirm the connection from your PC:

```bash
adb devices
```

You should see one line that ends with `device` (not `unauthorized`).

## Step 4 — Download the APK

From the latest [GitHub Release](https://github.com/A-Hackers-Guide/QuestClassic/releases/latest), download `app-release-unsigned.apk` (or `app-debug.apk` if you built it yourself; see [contributing/getting-started.md](../contributing/getting-started.md)).

## Step 5 — Install

```bash
adb install app-release-unsigned.apk
```

Expected output ends with `Success`. If you've installed a previous version:

```bash
adb install -r app-release-unsigned.apk      # keeps existing data
adb install -r -d app-release-unsigned.apk   # allow version-code downgrade
```

## Step 6 — Open it

On the headset: **Library → Apps → ▾ dropdown → Unknown Sources → QuestClassic**. First launch takes about a second to come up.

## Troubleshooting

Something not working? [getting-started/troubleshooting.md](troubleshooting.md).

## Uninstalling

- From the headset: Library → Unknown Sources → QuestClassic → ⋮ → Uninstall.
- From ADB: `adb uninstall io.github.questclassic.app`.

No system state is changed; uninstall is complete.

---

_Last reviewed against QuestClassic v0.1.0._
