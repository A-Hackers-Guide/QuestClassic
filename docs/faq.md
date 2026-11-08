# FAQ

## Does this root my Quest?

No. It doesn't modify any system file, doesn't require USB debugging after the first install, and can't elevate past what Android gives any sideloaded app on your device. It's a normal APK. Uninstalling it leaves the system identical to how it was.

## Can it remove Facebook/Meta integration?

No — and any app that claims to on a locked-bootloader Quest 2 is lying or malware. The Meta apps live on a dm-verity-signed partition; removing them changes the partition's Merkle tree, which breaks `vbmeta_system` verification, and the bootloader refuses to continue. We explain the full chain in [reverse-engineering/avb-chain.md](reverse-engineering/avb-chain.md).

What QuestClassic *does* do is give you a visible map of the Meta online stack and let you verify your own OTAs offline — knowledge, not removal.

## Does the self-updater talk to Meta?

No. It polls one host — `api.github.com` — and downloads release APKs from `github.com`. That's the entire network footprint of the self-updater. The AI Companion panel talks to whatever provider YOU configured (and only if you configured one).

Full list of network endpoints the app can reach is in [security/model.md](security/model.md).

## Will it survive Meta's OTAs?

Yes. Meta's A/B OTAs don't wipe user-installed APKs; QuestClassic + its DataStore persist through every HorizonOS update. The one exception: a factory reset wipes user data and you'd need to reinstall. See [architecture/storage.md](architecture/storage.md) for storage details.

## Why doesn't the "three-lines boot menu" idea work?

That's a bootloader-level interaction. QuestClassic is an Android app — `init` finishes booting the OS before the first app runs. We never see the boot key combo and can't intercept it. The bootloader is itself signed, so even flashing a replacement is impossible on a locked device. See [reverse-engineering/avb-chain.md](reverse-engineering/avb-chain.md).

## Can I write my own panels?

Yes. [contributing/getting-started.md](contributing/getting-started.md) walks through adding a panel in ~30 lines. Each existing panel under `app/src/main/kotlin/io/github/questclassic/app/ui/panels/` is a self-contained example.

## Why 90° for the AR hand menu?

Research on wrist-anchored menus — Meta published some of this in 2023 — shows 90° is the sweet spot across most adults' comfortable wrist-rotation range. Larger arcs force uncomfortable supination; smaller ones crowd the tiles. See [user-guide/ar-home.md](user-guide/ar-home.md).

## Does it work on Quest 3 / Quest Pro?

The supported-devices allow-list in `AndroidManifest.xml` includes `hollywood` (Quest 2), `monterey` (Quest Pro), `panther` (Quest 3S), `eureka` (Quest 3), `seacliff` (reserved). It should work; Quest 3-specific features (depth sensor, Quest 3 controllers) aren't surfaced yet but nothing in the current app depends on Quest-2-specific hardware either.

## Is this legal?

Modifying your own device, writing apps for it, reading its OS from userspace APIs, and publishing APKs to GitHub — yes, universally legal. Reverse-engineering for interoperability is explicitly permitted under DMCA §1201(f) in the US and under similar clauses in EU/UK/AU law. We do not distribute Meta's copyrighted binaries or decompiled source; see [the README's "Legal / IP posture"](../README.md#legal--ip-posture).

## What if Meta changes their OTA format?

The Signature Verifier reads the CrAU header version and falls back gracefully; if Meta moved to a new format, the panel would say "unsupported version" rather than silently producing wrong results. We'd then write a new verifier against the new format. The reverse-engineering notes are in this repo precisely so that's reproducible.

## Can I use a Meta account to sign in via QuestClassic?

No — QuestClassic doesn't do account auth. All it does is help you see the Meta auth flow (via the Meta Hosts Audit + System Internals panels). If an app ever asks you for your Meta password, that is NOT one of ours. The AI Companion panel asks for an AI-provider API key (Anthropic / OpenAI / your Ollama URL) — that's it.

## How do I earn the GitHub achievements?

See [contributing/achievements.md](contributing/achievements.md). Everything legitimate; no fake accounts, no star rings.

## I found a security issue, who do I tell?

See [security/reporting.md](security/reporting.md). Short version: don't open a public issue; use GitHub's private vulnerability reporting flow on the repo.
