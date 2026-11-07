#!/usr/bin/env python3
"""Verify the Meta Quest 2 OTA against otacert.pem.

The AOSP A/B update_engine payload has TWO independent signatures (both over
SHA-256 digests, with RSA-2048 using otacert's public key):

  1. metadata signature  — covers [header + manifest]
     - digest = SHA-256 of the first (24 + manifest_size) bytes of payload.bin
     - the 267-byte RSA signature sits immediately after that prefix

  2. final payload signature — covers the ENTIRE payload (except its own trailer)
     - digest = SHA-256 over bytes [0 .. signatures_offset + sizeof(header-of-sig))
       where manifest.signatures_offset is relative to the start of the data
       blobs section (which begins at 24 + manifest_size + meta_sig_size)
     - the 267-byte RSA signature is at the very end of payload.bin

Both 267-byte blobs are themselves protobuf `Signatures` messages wrapping the
raw RSA signature at field 2 inside a `Signature` message (field 1 of the outer
Signatures message). We parse that out, then RSA-verify.
"""
from __future__ import annotations
import hashlib
import struct
import subprocess
import sys
from pathlib import Path


def read_varint(buf, i):
    shift, v = 0, 0
    while True:
        b = buf[i]; i += 1
        v |= (b & 0x7F) << shift
        if not (b & 0x80):
            return v, i
        shift += 7


def parse_signatures_pb(blob):
    """Extract the raw RSA signature bytes from a Signatures proto blob."""
    i = 0
    while i < len(blob):
        tag, i = read_varint(blob, i)
        field, wire = tag >> 3, tag & 7
        if wire == 2:
            ln, i = read_varint(blob, i)
            data = blob[i:i + ln]
            i += ln
            if field == 1:  # Signature submessage
                # parse the submessage
                j = 0
                sig_bytes = None
                while j < len(data):
                    t2, j = read_varint(data, j)
                    f2, w2 = t2 >> 3, t2 & 7
                    if w2 == 0:
                        _, j = read_varint(data, j)
                    elif w2 == 2:
                        ln2, j = read_varint(data, j)
                        sd = data[j:j + ln2]; j += ln2
                        if f2 == 2:  # the actual signature bytes
                            sig_bytes = sd
                if sig_bytes:
                    return sig_bytes
        elif wire == 0:
            _, i = read_varint(buf, i)
    return None


def rsa_verify(pubkey_pem_path, digest_hex, sig_bytes):
    """Use openssl to verify an RSA signature over an already-computed SHA-256."""
    # openssl pkeyutl -verify needs the raw DER-encoded SHA-256 digest? No: we use
    # `openssl dgst -verify -sha256 -signature sig -binary file` normally, but
    # here we only have the digest. Easier: write pubkey & use pkeyutl with
    # -pkeyopt digest:sha256 — openssl can do raw RSA verify with PKCS#1 v1.5.
    import tempfile, os
    sig_path = tempfile.NamedTemporaryFile(delete=False, suffix=".sig").name
    dgst_path = tempfile.NamedTemporaryFile(delete=False, suffix=".dgst").name
    try:
        with open(sig_path, "wb") as f:
            f.write(sig_bytes)
        with open(dgst_path, "wb") as f:
            f.write(bytes.fromhex(digest_hex))
        # Extract public key from the cert
        pk_path = tempfile.NamedTemporaryFile(delete=False, suffix=".pem").name
        subprocess.check_call(
            ["openssl", "x509", "-in", pubkey_pem_path, "-noout", "-pubkey"],
            stdout=open(pk_path, "w"),
        )
        res = subprocess.run(
            ["openssl", "pkeyutl", "-verify", "-pubin", "-inkey", pk_path,
             "-in", dgst_path, "-sigfile", sig_path,
             "-pkeyopt", "digest:sha256"],
            capture_output=True, text=True,
        )
        return res.returncode == 0, res.stdout + res.stderr
    finally:
        for p in [sig_path, dgst_path, pk_path]:
            try: os.unlink(p)
            except Exception: pass


def main(payload_path, otacert_path):
    with open(payload_path, "rb") as f:
        hdr = f.read(20)
        magic, version, manifest_size = struct.unpack(">4sQQ", hdr)
        (meta_sig_size,) = struct.unpack(">I", f.read(4))
        assert magic == b"CrAU", "not a CrAU payload"

        # 1) Metadata signature: SHA-256 over bytes [0 .. 24+manifest_size)
        f.seek(0)
        prefix = f.read(24 + manifest_size)
        metadata_digest = hashlib.sha256(prefix).hexdigest()
        meta_sig_blob = f.read(meta_sig_size)
        meta_sig = parse_signatures_pb(meta_sig_blob)
        print(f"[1] metadata signature (RSA over SHA-256):")
        print(f"      header+manifest bytes = {len(prefix):,}")
        print(f"      SHA-256(digest)       = {metadata_digest}")
        print(f"      raw RSA sig length    = {len(meta_sig) if meta_sig else 'parse-fail'}")

        # To find the final signature, we need manifest.signatures_offset and
        # signatures_size. We already computed them in payload_summary.py:
        #   signatures_offset = 1102976859 (relative to data blobs start)
        #   signatures_size   = 267
        # data_blobs_start = 24 + manifest_size + meta_sig_size
        data_blobs_start = 24 + manifest_size + meta_sig_size
        # Parse the manifest just enough to extract fields 4 and 5 (signatures_offset / _size)
        f.seek(24)
        manifest = f.read(manifest_size)
        sigs_off, sigs_sz = None, None
        i = 0
        while i < len(manifest):
            tag, i = read_varint(manifest, i)
            field, wire = tag >> 3, tag & 7
            if wire == 0:
                v, i = read_varint(manifest, i)
                if field == 4: sigs_off = v
                elif field == 5: sigs_sz = v
            elif wire == 2:
                ln, i = read_varint(manifest, i)
                i += ln
            elif wire == 1:
                i += 8
            elif wire == 5:
                i += 4

        print(f"\n    manifest.signatures_offset = {sigs_off:,} (within data blobs)")
        print(f"    manifest.signatures_size   = {sigs_sz}")

        # 2) Final signature: SHA-256 over header+manifest (skip metadata signature)
        #    then over the data section up to the final signature blob.
        #    The metadata signature bytes are NOT included because they're not
        #    written to disk during install.
        final_sig_start = data_blobs_start + sigs_off  # absolute offset of final sig blob
        h = hashlib.sha256()
        # Part 1: header + manifest  [0 .. 24+manifest_size)
        f.seek(0)
        remaining = 24 + manifest_size
        while remaining > 0:
            chunk = f.read(min(1 << 20, remaining))
            if not chunk: break
            h.update(chunk); remaining -= len(chunk)
        # Part 2: data section  [data_blobs_start .. final_sig_start)
        f.seek(data_blobs_start)
        remaining = final_sig_start - data_blobs_start
        while remaining > 0:
            chunk = f.read(min(1 << 20, remaining))
            if not chunk: break
            h.update(chunk); remaining -= len(chunk)
        final_digest = h.hexdigest()
        final_sig_blob = f.read(sigs_sz)
        final_sig = parse_signatures_pb(final_sig_blob)
        print(f"\n[2] final payload signature (RSA over SHA-256):")
        print(f"      signed bytes          = {final_sig_start:,}")
        print(f"      SHA-256(digest)       = {final_digest}")
        print(f"      raw RSA sig length    = {len(final_sig) if final_sig else 'parse-fail'}")

    # Verify each with openssl
    print("\n=== verifying against otacert.pem ===")
    ok1, msg1 = rsa_verify(otacert_path, metadata_digest, meta_sig)
    print(f"  [1] metadata signature: {'VALID ✅' if ok1 else 'INVALID ❌'}  {msg1.strip()}")
    ok2, msg2 = rsa_verify(otacert_path, final_digest, final_sig)
    print(f"  [2] final    signature: {'VALID ✅' if ok2 else 'INVALID ❌'}  {msg2.strip()}")
    return 0 if (ok1 and ok2) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
