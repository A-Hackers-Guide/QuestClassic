#!/usr/bin/env python3
"""Extract raw partition images from an A/B update_engine payload.bin.

Supports REPLACE (0), REPLACE_BZ (1), REPLACE_XZ (8), ZERO (6), DISCARD (7) —
enough for a *full* OTA. Would need extra ops for incremental updates.

Zero-deps: uses only stdlib (bz2, lzma, hashlib).
"""
from __future__ import annotations
import bz2, hashlib, lzma, os, struct, sys, time


# ---- generic protobuf wire decoding (same as payload_summary.py) ----

def _varint(buf, i):
    shift, v = 0, 0
    while True:
        b = buf[i]; i += 1
        v |= (b & 0x7F) << shift
        if not (b & 0x80):
            return v, i
        shift += 7


def _fields(buf):
    i = 0
    while i < len(buf):
        tag, i = _varint(buf, i)
        f, w = tag >> 3, tag & 7
        if w == 0:
            v, i = _varint(buf, i); yield f, 0, v
        elif w == 1:
            v = struct.unpack_from("<Q", buf, i)[0]; i += 8; yield f, 1, v
        elif w == 2:
            ln, i = _varint(buf, i)
            yield f, 2, bytes(buf[i:i + ln]); i += ln
        elif w == 5:
            v = struct.unpack_from("<I", buf, i)[0]; i += 4; yield f, 5, v
        else:
            raise ValueError(f"wire type {w} at {i}")


def _msg(buf):
    out = {}
    for f, _, v in _fields(buf):
        out.setdefault(f, []).append(v)
    return out


def _one(m, k):
    return m.get(k, [None])[0]


def _many(m, k):
    return m.get(k, [])


# ---- payload extraction ----

OP_REPLACE, OP_REPLACE_BZ, OP_ZERO, OP_DISCARD, OP_REPLACE_XZ = 0, 1, 6, 7, 8


def _extents(raw_list):
    """Extent is a message {1: start_block, 2: num_blocks}. Returns list of (start, count)."""
    out = []
    for raw in raw_list:
        e = _msg(raw)
        out.append((_one(e, 1) or 0, _one(e, 2) or 0))
    return out


def _fmt_bytes(n):
    for unit in ("B", "KiB", "MiB", "GiB"):
        if n < 1024:
            return f"{n:.1f}{unit}"
        n /= 1024
    return f"{n:.1f}TiB"


def extract(payload_path, out_dir, only=None):
    only_set = set(only) if only else None
    os.makedirs(out_dir, exist_ok=True)

    with open(payload_path, "rb") as f:
        header = f.read(20)
        magic, version, manifest_size = struct.unpack(">4sQQ", header)
        if magic != b"CrAU":
            raise ValueError(f"not a CrAU payload: {magic!r}")
        meta_sig_size, = struct.unpack(">I", f.read(4))
        manifest = f.read(manifest_size)
        _ = f.read(meta_sig_size)  # skip metadata signature
        data_blobs_start = 24 + manifest_size + meta_sig_size  # 24 = 20 header + 4 msig-size
        dam = _msg(manifest)
        block_size = _one(dam, 3) or 4096
        parts = _many(dam, 13)

        print(f"payload: {payload_path}")
        print(f"CrAU v{version}, manifest {manifest_size:,}B, meta-sig {meta_sig_size}B, "
              f"data starts at {data_blobs_start:,}, block {block_size}")
        print(f"{len(parts)} partitions in manifest; extracting "
              f"{'all' if not only else ', '.join(sorted(only_set))}")
        print()
        print(f"{'partition':16s}  {'size':>12s}  {'ops':>5s}  {'time':>7s}  {'sha256 ok':>10s}")

        for p_raw in parts:
            p = _msg(p_raw)
            name = _one(p, 1).decode()
            if only_set and name not in only_set:
                continue

            ni_raw = _one(p, 7)
            ni = _msg(ni_raw) if ni_raw else {}
            new_size = _one(ni, 1) or 0
            expected_hash = _one(ni, 2)  # bytes, SHA-256 of the *final partition image*

            out_path = os.path.join(out_dir, f"{name}.img")
            t0 = time.time()

            # Allocate the output file at the exact expected size.
            with open(out_path, "wb") as out:
                out.truncate(new_size)
                for op_raw in _many(p, 8):
                    o = _msg(op_raw)
                    op_type = _one(o, 1) or 0
                    data_offset = _one(o, 2) or 0
                    data_length = _one(o, 3) or 0
                    dst = _extents(_many(o, 6))

                    # Load this op's data blob from payload.bin
                    blob = b""
                    if data_length:
                        f.seek(data_blobs_start + data_offset)
                        blob = f.read(data_length)

                    if op_type == OP_REPLACE:
                        decompressed = blob
                    elif op_type == OP_REPLACE_BZ:
                        decompressed = bz2.decompress(blob)
                    elif op_type == OP_REPLACE_XZ:
                        decompressed = lzma.decompress(blob)
                    elif op_type == OP_ZERO:
                        # Zero the destination extents; file is already zero-filled by truncate.
                        decompressed = None
                    elif op_type == OP_DISCARD:
                        # Mark blocks unused; nothing to write for a plain image dump.
                        decompressed = None
                    else:
                        raise NotImplementedError(
                            f"{name}: unsupported op type {op_type} — this extractor only "
                            "handles full-OTA ops")

                    if decompressed is None:
                        continue

                    # Spread decompressed data across dst_extents (in order).
                    cursor = 0
                    for start_block, num_blocks in dst:
                        byte_off = start_block * block_size
                        byte_len = num_blocks * block_size
                        chunk = decompressed[cursor:cursor + byte_len]
                        if len(chunk) != byte_len:
                            # The last extent of the last op may be short of a full block when
                            # new_size isn't block-aligned; pad with zeros to the extent length.
                            chunk = chunk + b"\x00" * (byte_len - len(chunk))
                        out.seek(byte_off)
                        out.write(chunk)
                        cursor += byte_len

            # Verify SHA-256 of the extracted image against the manifest's expected hash.
            h = hashlib.sha256()
            with open(out_path, "rb") as g:
                for chunk in iter(lambda: g.read(4 << 20), b""):
                    h.update(chunk)
            actual = h.digest()
            ok = "yes" if expected_hash and actual == expected_hash else (
                "NO" if expected_hash else "n/a")
            dt = time.time() - t0
            print(f"{name:16s}  {_fmt_bytes(new_size):>12s}  "
                  f"{len(_many(p, 8)):>5d}  {dt:>6.2f}s  {ok:>10s}")

    print("\ndone.")


if __name__ == "__main__":
    args = sys.argv[1:]
    if len(args) < 2:
        print("usage: payload_extract.py <payload.bin> <out_dir> [partition...]", file=sys.stderr)
        sys.exit(2)
    extract(args[0], args[1], only=args[2:] or None)
