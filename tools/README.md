# QuestClassic analysis tools

Zero-dependency Python 3 scripts that read Android A/B `update_engine` payloads and the sidecar files inside a Meta Quest OTA. We wrote these from the AOSP `update_metadata.proto` wire spec — they do not use `google.protobuf`, `protoc`, or any other third-party library.

## Scripts

| Script | What it does |
|---|---|
| `pbdump.py` | Generic raw-protobuf dumper. Equivalent to `protoc --decode_raw` but 0-dep. |
| `payload_summary.py` | Parses `payload.bin`: header, dynamic-partition metadata, every partition's size + op count + op-type histogram. |
| `payload_extract.py` | Walks the manifest's install operations and writes each partition out as a raw `.img`, then SHA-256-verifies against the manifest's `new_partition_info.hash`. |
| `inspect_images.py` | Decodes the Android `boot.img` v0–v4 header and the AVB `vbmeta`/`vbmeta_system`/`vbmeta_vendor` descriptors (property, hash, hashtree, chain-partition, kernel-cmdline). |
| `verify_payload.py` | Verifies both signatures (metadata + final) inside `payload.bin` against the OTA's `otacert.pem` using OpenSSL. Reproduces exactly what the Quest bootloader checks. |

## Usage

```bash
python3 pbdump.py path/to/some.pb                        # dump a .pb
python3 payload_summary.py payload.bin                   # list partitions + ops
python3 payload_extract.py payload.bin out/              # dump all partitions
python3 payload_extract.py payload.bin out/ boot vbmeta  # or specific partitions
python3 inspect_images.py boot.img vbmeta.img            # decode headers
python3 verify_payload.py payload.bin otacert.pem        # verify signatures
```

All scripts take only the file arguments shown above and print to stdout. No hidden state.

## Requirements

- Python 3.9+ (standard library only — `struct`, `hashlib`, `bz2`, `lzma`).
- `openssl` on `$PATH` for `verify_payload.py` (OpenSSL 3.x).

## What they do **not** do

- They don't produce a signed OTA — only Meta can do that (see the top-level `README.md`'s hard-constraints section).
- They don't modify any file. Every operation is read-only.
- They don't phone home. All I/O is on the files you pass in.
