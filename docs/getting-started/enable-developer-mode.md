# Enable Developer Mode on your Meta account

Meta gates sideloading behind a "Developer Mode" flag. It's a one-time flip on your account, free, and doesn't void your warranty, revoke your subscription, or anything else theatrical.

## What you'll do

Register (or join) a developer organization on Meta's dashboard, flip the toggle on your account, then enable it on the headset itself from the Meta mobile app.

## Steps

1. On a PC or phone browser, open [developer.meta.com/manage](https://developer.meta.com/manage) and sign in with the same Meta account you use on the headset.
2. If prompted, create a new organization. Pick any name — it's just an internal ID — and accept the developer terms.
3. Open the **Meta Horizon** app (used to be "Oculus app") on your phone. The phone needs to be paired with the headset already.
4. Tap **Menu → Devices → (your headset) → Headset Settings → Developer Mode**.
5. Flip it on.

## Verify

Put the headset on. Go to **Settings → System → Developer**. You should see the developer menu — if you don't, Meta hasn't rolled the flag through yet. Give it a few minutes, maybe a reboot of the headset, then check again.

## What this changes on your account

- You can install APKs over ADB (what we use here).
- You can run `adb logcat` and read your own device's logs.
- You get access to a couple of "Developer" toggles in the headset settings (passthrough debugging, forced permissions dialogs, etc.).

## What this doesn't change

- No bootloader unlock, no root, no AVB bypass.
- No change to purchases, subscriptions, warranty, or support eligibility.
- No visible sign to Meta's Store that you're a developer — you buy games the same way.

## Opting back out

Turn it off in the same phone-app menu. Headset-side developer menu disappears a reboot later.

---

_Last reviewed against Meta Horizon mobile app v242 and the developer.meta.com dashboard, Nov 2026. Meta renames things — if the exact menu paths drift, the concept hasn't moved._
