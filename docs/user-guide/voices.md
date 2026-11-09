# Voices

User-manageable catalog of TTS voices the AI companion can speak as. Two kinds:

- **Built-in** — from the Android platform TTS engine (`TextToSpeech.getVoices()`). No training; you pick locale + engine + voice name.
- **Cloned** — your own voice, trained from recorded samples. Synthesised on-device only — samples never leave the headset.

## Clone training state machine

```
QUEUED ─► RECORDING ─► TRAINING ─► READY
   │          │             │
   └──► FAILED ──► QUEUED (retry)
```

Legal transitions (enforced by `VoiceRegistry.canTransition`):

| From | Allowed to |
|---|---|
| `QUEUED` | `RECORDING` · `FAILED` |
| `RECORDING` | `TRAINING` · `FAILED` |
| `TRAINING` | `READY` · `FAILED` |
| `READY` | `READY` |
| `FAILED` | `QUEUED` |

The clone pipeline (future PR) calls `canTransition(from, to)` before `advanceTraining(id, to)` so bogus moves like "already READY, drop to TRAINING" are rejected with a clear message instead of silently corrupting state.

## Limits

- **32-voice cap.** `startClone` above cap throws; `registerBuiltIn` upserts within the cap.
- **Samples stay on-device.** `samplePath` points under app-private storage (`/data/data/io.github.questclassic.app/files/voices/…`).

## CRUD

```kotlin
val voices = container.voices

suspend fun examples() {
    // Platform voice
    voices.registerBuiltIn(Voice(
        id = "en-us-sarah",
        displayName = "Sarah",
        kind = VoiceKind.BUILT_IN,
        engine = "com.google.android.tts",
        voiceName = "en-us-x-sfg#female_1-local",
    ))

    // Start cloning
    val v = voices.startClone(id = "me", displayName = "My voice")
    // Pipeline records samples, trains, advances:
    voices.advanceTraining("me", TrainingState.RECORDING)
    voices.advanceTraining("me", TrainingState.TRAINING)
    voices.advanceTraining("me", TrainingState.READY)

    voices.toggleEnabled("me", enabled = false)   // user hides without deleting
    voices.rename("me", "Me (studio take)")
    voices.remove("me")
}
```

## Privacy posture

- Cloned voice samples live under app-private storage. Nothing writes them off-device.
- Platform TTS engines may call home (e.g. Google TTS downloads voices). We disclose this on the setup form and recommend the local TTS engine from F-Droid if you want strict offline.
- Cloned voice synthesis runs on-device via the clone pipeline (future PR) using a small on-device model. No cloud voice service is used.

## Where state lives

`DataStore (Preferences)` at:

```
/data/data/io.github.questclassic.app/datastore/voice_registry.preferences_pb
```

One row `voices` with the JSON-encoded list.
