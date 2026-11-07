#!/usr/bin/env python3
"""Parse an Android A/B update_engine payload.bin and summarize it.

Zero-dep: pure Python, implements just enough protobuf wire parsing and uses
public field numbers from AOSP's update_metadata.proto (DeltaArchiveManifest,
PartitionUpdate, InstallOperation, Extent, PartitionInfo, DynamicPartitionMetadata).
"""
from __future__ import annotations
import struct, sys, os


# --- generic wire decode -----------------------------------------------------

def read_varint(buf, i):
    shift, v = 0, 0
    while True:
        b = buf[i]; i += 1
        v |= (b & 0x7F) << shift
        if not (b & 0x80):
            return v, i
        shift += 7


def iter_fields(buf):
    i = 0
    while i < len(buf):
        tag, i = read_varint(buf, i)
        field = tag >> 3
        wire = tag & 7
        if wire == 0:
            v, i = read_varint(buf, i); yield field, 0, v
        elif wire == 1:
            v = struct.unpack_from("<Q", buf, i)[0]; i += 8; yield field, 1, v
        elif wire == 2:
            ln, i = read_varint(buf, i)
            yield field, 2, bytes(buf[i:i + ln]); i += ln
        elif wire == 5:
            v = struct.unpack_from("<I", buf, i)[0]; i += 4; yield field, 5, v
        else:
            raise ValueError(f"unknown wire type {wire} at {i}")


def parse_message(buf):
    out = {}
    for f, w, v in iter_fields(buf):
        out.setdefault(f, []).append(v)
    return out


def one(m, k):
    return m.get(k, [None])[0]


def many(m, k):
    return m.get(k, [])


# --- InstallOperation.Type mapping (from AOSP update_metadata.proto) ----------
OP_TYPES = {
    0: "REPLACE",
    1: "REPLACE_BZ",
    2: "MOVE",          # deprecated
    3: "BSDIFF",        # deprecated
    4: "SOURCE_COPY",
    5: "SOURCE_BSDIFF",
    6: "ZERO",
    7: "DISCARD",
    8: "REPLACE_XZ",
    9: "PUFFDIFF",
    10: "BROTLI_BSDIFF",
    11: "ZUCCHINI",
    12: "LZ4DIFF_BSDIFF",
    13: "LZ4DIFF_PUFFDIFF",
}


def sum_extents(ext_bytes_list):
    """extent = {1: start_block, 2: num_blocks}"""
    total_blocks = 0
    for raw in ext_bytes_list:
        e = parse_message(raw)
        total_blocks += one(e, 2) or 0
    return total_blocks


def summarize(payload_path):
    with open(payload_path, "rb") as f:
        hdr = f.read(20)
        magic, version, manifest_size = struct.unpack(">4sQQ", hdr)
        meta_sig_size, = struct.unpack(">I", f.read(4))
        manifest = f.read(manifest_size)
    assert magic == b"CrAU", f"bad magic: {magic!r}"
    print(f"payload magic/version: {magic.decode()} v{version}")
    print(f"manifest size:         {manifest_size:,}")
    print(f"metadata signature:    {meta_sig_size} bytes")
    print(f"manifest ends at:      {24+4+manifest_size:,}  (metadata sig follows)")
    print()

    dam = parse_message(manifest)
    block_size = one(dam, 3) or 4096
    print(f"block_size:                   {block_size}")
    print(f"signatures_offset (in blob):  {one(dam, 4)}")
    print(f"signatures_size:              {one(dam, 5)} bytes")
    # DeltaArchiveManifest field numbers from AOSP update_metadata.proto:
    #   3 block_size, 4 signatures_offset, 5 signatures_size, 12 minor_version,
    #   13 partitions, 14 max_timestamp, 15 dynamic_partition_metadata,
    #   16 partial_update, 17 apex_info, 18 security_patch_level
    print(f"minor_version:                {one(dam, 12)}")
    print(f"max_timestamp:                {one(dam, 14)}")
    print(f"partial_update:               {one(dam, 16)}")
    spl = one(dam, 18)
    if isinstance(spl, (bytes, bytearray)):
        try:
            spl = spl.decode()
        except Exception:
            pass
    print(f"security_patch_level:         {spl}")
    print(f"apex_info count:              {len(many(dam, 17))}")
    print()

    dpm_raw = one(dam, 15)
    if dpm_raw:
        dpm = parse_message(dpm_raw)
        print("--- dynamic_partition_metadata ---")
        # fields: 1 repeated DynamicPartitionGroup, 2 snapshot_enabled, 3 vabc_enabled,
        # 4 vabc_compression_param, 5 cow_version, 6 vabc_feature_set
        print(f"  snapshot_enabled:  {one(dpm, 2)}")
        print(f"  vabc_enabled:      {one(dpm, 3)}")
        print(f"  vabc_compression:  {one(dpm, 4)!r}")
        print(f"  cow_version:       {one(dpm, 5)}")
        for grp_raw in many(dpm, 1):
            grp = parse_message(grp_raw)
            name = one(grp, 1)
            size = one(grp, 2)
            parts = many(grp, 3)
            try:
                name_s = name.decode() if isinstance(name, (bytes, bytearray)) else name
            except Exception:
                name_s = name
            part_names = []
            for p in parts:
                if isinstance(p, (bytes, bytearray)):
                    try:
                        part_names.append(p.decode())
                    except Exception:
                        part_names.append(repr(p))
            print(f"  group {name_s!r:20s}  max={size:>14,}  parts={part_names}")
        print()

    # partitions (field 13 is repeated PartitionUpdate)
    parts = many(dam, 13)
    print(f"--- partitions ({len(parts)}) ---")
    print(f"{'name':16s}  {'new_size':>14s}  {'ops':>5s}  {'data_bytes':>14s}  {'op_types'}")
    totals = {"new_size": 0, "ops": 0, "data_bytes": 0}
    type_hist = {}
    for p_raw in parts:
        p = parse_message(p_raw)
        name = one(p, 1).decode() if one(p, 1) else "?"
        new_info_raw = one(p, 7)
        new_size = 0
        new_hash = None
        if new_info_raw:
            ni = parse_message(new_info_raw)
            new_size = one(ni, 1) or 0
            new_hash = one(ni, 2)
        ops = many(p, 8)
        data_bytes = 0
        local_types = {}
        for op_raw in ops:
            o = parse_message(op_raw)
            t = one(o, 1) or 0
            ds = one(o, 3) or 0
            data_bytes += ds
            local_types[t] = local_types.get(t, 0) + 1
            type_hist[t] = type_hist.get(t, 0) + 1
        types_s = ",".join(f"{OP_TYPES.get(t, t)}×{c}" for t, c in sorted(local_types.items()))
        print(f"{name:16s}  {new_size:>14,}  {len(ops):>5d}  {data_bytes:>14,}  {types_s}")
        totals["new_size"] += new_size
        totals["ops"] += len(ops)
        totals["data_bytes"] += data_bytes
    print(f"{'TOTAL':16s}  {totals['new_size']:>14,}  {totals['ops']:>5d}  {totals['data_bytes']:>14,}")
    print()
    print("--- op-type histogram ---")
    for t, c in sorted(type_hist.items(), key=lambda kv: -kv[1]):
        print(f"  {OP_TYPES.get(t, t):20s} {c}")


if __name__ == "__main__":
    summarize(sys.argv[1])
