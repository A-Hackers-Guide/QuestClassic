# OTA format

Everything that's in a Meta Quest 2 OTA .zip, byte-level and structurally. Based on AOSP's A/B update_engine `CrAU v2` format; Meta uses it unchanged.

## ZIP layout

```
q2_<build>.zip
├── q2_<build>/
│   ├── META-INF/com/android/
│   │   ├── metadata                    text pre/post-condition
│   │   ├── metadata.pb                 same, as protobuf
│   │   └── otacert                     PEM X.509, OTA signing cert
│   ├── apex_info.pb                    list of APEX modules in target
│   ├── care_map.pb                     per-partition blocks-in-use + fingerprints
│   ├── payload.bin                     ~1.1 GB CrAU payload
│   └── payload_properties.txt          SHA-256 + size of payload + its metadata prefix
└── __MACOSX/                           AppleDouble metadata (someone opened it on a Mac)
```

The `__MACOSX` prefix shows this specific ZIP was repacked on a Mac at some point; not part of the original Meta pipeline.

## `payload.bin` header (first 24 bytes)

| Offset | Bytes | Meaning | Value on this OTA |
|---:|---:|---|---|
| 0 | 4 | magic | `CrAU` |
| 4 | 8 | version (uint64 BE) | 2 |
| 12 | 8 | manifest_size | 86,663 |
| 20 | 4 | metadata_signature_size (v2+) | 267 |

Next 86,663 bytes = the `DeltaArchiveManifest` protobuf.
Next 267 bytes = the metadata Signatures protobuf.
Next ~1.1 GB = data blobs.
Final 267 bytes = the final Signatures protobuf.

## `DeltaArchiveManifest` (abridged)

AOSP's `update_metadata.proto`:

| Field # | Name | Why we care |
|---|---|---|
| 3 | `block_size` | 4096 on this device |
| 4 | `signatures_offset` | 1,102,976,859 (bytes from data_blobs_start) |
| 5 | `signatures_size` | 267 |
| 12 | `minor_version` | 0 (full OTA, no delta ops) |
| 13 | `partitions` (repeated) | 25 entries on this OTA |
| 14 | `max_timestamp` | 1,793,760,311 = anti-rollback floor |
| 15 | `dynamic_partition_metadata` | super cap 6.44 GB, group `kona_dynamic_partitions` |
| 17 | `apex_info` (repeated) | 0 here; APEX info lives in sidecar `apex_info.pb` |
| 18 | `security_patch_level` | `2026-09-06` |

Each `PartitionUpdate` has:

- `partition_name` (string)
- `new_partition_info { size, hash }`
- `operations` (repeated `InstallOperation`)

Operation types in our build (full OTA, so no diff ops):

| Type | Count | Compression |
|---|---:|---|
| REPLACE_XZ | 1439 | xz |
| REPLACE_BZ | 52 | bz2 |
| REPLACE | 16 | raw |

25 partitions × 1507 ops × 3.13 GB logical → 1.10 GB on-disk = 35% compression.

## Metadata signature

Standard CrAU-v2:

- SHA-256 over bytes `[0 .. 24 + manifest_size)` = first 86,687 bytes.
- RSA-2048 signature in a `Signatures` protobuf, 267 bytes total.

```
Signatures {
  signatures: [Signature {
    version: deprecated
    data: <256 bytes RSA sig>
    unpadded_signature_size: 256
  }]
}
```

## Final payload signature

The one that trips up every first-time implementer. AOSP hashes:

```
[0 .. 24 + manifest_size) ∪ [data_blobs_start .. final_sig_at)
```

That is: **header + manifest** AND THEN **data section up to the final signature blob** — the 267 bytes of metadata signature in between are **skipped**. Why: those bytes aren't persisted to disk during install; the device computes the hash over what it writes, not over what it discards.

Reproduce in Python:

```python
h = hashlib.sha256()
h.update(payload[:24 + manifest_size])
h.update(payload[data_blobs_start:final_sig_at])
expected_hash = h.digest()
```

Reproduce in Kotlin (what we do in `PayloadVerifier`):

```kotlin
val md = MessageDigest.getInstance("SHA-256")
// Part 1: header + manifest
f.seek(0); streamToDigest(f, 24 + manifestSize, md)
// Part 2: data section up to the sig
f.seek(dataBlobsStart); streamToDigest(f, finalSigStart - dataBlobsStart, md)
val digest = md.digest()
```

## Sidecar files

- **`apex_info.pb`** — list of APEX modules + versions in the target image. On this build: 30 APEXes, almost all pinned to the Android 14 QPR module-release train `340090000`.
- **`care_map.pb`** — per-partition "blocks in use" map + per-partition fingerprints (where the internal build-host leak lives).
- **`metadata.pb`** — binary form of the `metadata` text file.
- **`otacert`** — PEM X.509 signing cert.
- **`payload_properties.txt`** — `FILE_HASH=…`, `FILE_SIZE=…`, `METADATA_HASH=…`, `METADATA_SIZE=…`. Useful for cheap integrity checks before even parsing the payload.

## See also

- [reverse-engineering/avb-chain.md](avb-chain.md) — how this fits into Verified Boot.
- [User guide — Signature Verifier panel](../user-guide/signature-verifier.md) — on-device UX.
- `tools/verify_payload.py` / `app/.../PayloadVerifier.kt` — two implementations of the signature-check logic.

---

_Last reviewed against the Nov 2026 Quest 2 OTA._
