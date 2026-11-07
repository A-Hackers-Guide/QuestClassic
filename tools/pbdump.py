#!/usr/bin/env python3
"""Minimal raw-protobuf dumper (like `protoc --decode_raw`) with no deps."""
from __future__ import annotations
import struct, sys


def read_varint(buf, i):
    shift = 0
    v = 0
    while True:
        if i >= len(buf):
            raise ValueError("truncated varint")
        b = buf[i]
        i += 1
        v |= (b & 0x7F) << shift
        if not (b & 0x80):
            return v, i
        shift += 7
        if shift > 70:
            raise ValueError("varint too long")


def zigzag(n):
    return (n >> 1) ^ -(n & 1)


def looks_like_text(b):
    if not b:
        return False
    try:
        s = b.decode("utf-8")
    except UnicodeDecodeError:
        return False
    return all(0x20 <= ord(c) < 0x7F or c in "\t\n\r" for c in s)


def try_decode_message(buf):
    out = []
    i = 0
    while i < len(buf):
        try:
            tag, i = read_varint(buf, i)
        except Exception:
            return None
        field = tag >> 3
        wire = tag & 7
        if wire == 0:
            try:
                v, i = read_varint(buf, i)
            except Exception:
                return None
            out.append((field, "varint", v))
        elif wire == 1:
            if i + 8 > len(buf):
                return None
            v = struct.unpack("<Q", buf[i:i + 8])[0]
            i += 8
            out.append((field, "fixed64", v))
        elif wire == 2:
            try:
                length, i = read_varint(buf, i)
            except Exception:
                return None
            if i + length > len(buf):
                return None
            data = buf[i:i + length]
            i += length
            out.append((field, "bytes", data))
        elif wire == 5:
            if i + 4 > len(buf):
                return None
            v = struct.unpack("<I", buf[i:i + 4])[0]
            i += 4
            out.append((field, "fixed32", v))
        else:
            return None
    return out


def render(buf, depth=0, lines=None):
    if lines is None:
        lines = []
    msg = try_decode_message(buf)
    if msg is None:
        return None
    pad = "  " * depth
    for field, kind, val in msg:
        if kind == "varint":
            lines.append(f"{pad}{field}: {val}  (zigzag: {zigzag(val)})")
        elif kind == "fixed64":
            try:
                flt = struct.unpack("<d", struct.pack("<Q", val))[0]
            except Exception:
                flt = None
            lines.append(f"{pad}{field}: 0x{val:016x} (u64={val}, i64={struct.unpack('<q', struct.pack('<Q', val))[0]}, f64={flt})")
        elif kind == "fixed32":
            flt = struct.unpack("<f", struct.pack("<I", val))[0]
            lines.append(f"{pad}{field}: 0x{val:08x} (u32={val}, i32={struct.unpack('<i', struct.pack('<I', val))[0]}, f32={flt})")
        elif kind == "bytes":
            sub = try_decode_message(val)
            printed = False
            if sub is not None and len(val) > 0:
                lines.append(f"{pad}{field} {{  # len={len(val)}")
                render(val, depth + 1, lines)
                lines.append(f"{pad}}}")
                printed = True
            elif looks_like_text(val):
                try:
                    s = val.decode("utf-8")
                    lines.append(f"{pad}{field}: {s!r}  (len={len(val)})")
                    printed = True
                except UnicodeDecodeError:
                    pass
            if not printed:
                hx = val[:64].hex()
                more = "" if len(val) <= 64 else f"... (+{len(val)-64} more)"
                lines.append(f"{pad}{field}: 0x{hx}{more}  (len={len(val)})")
    return lines


def main():
    if len(sys.argv) < 2:
        print("usage: pbdump.py <file> [offset] [length]", file=sys.stderr)
        sys.exit(2)
    path = sys.argv[1]
    offset = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    length = int(sys.argv[3]) if len(sys.argv) > 3 else None
    with open(path, "rb") as f:
        f.seek(offset)
        buf = f.read() if length is None else f.read(length)
    lines = render(buf)
    if lines is None:
        print("(not a valid protobuf wire-format message)", file=sys.stderr)
        sys.exit(1)
    print("\n".join(lines))


if __name__ == "__main__":
    main()
