#!/usr/bin/env python3
"""Pretty-print Android boot-image header and AVB vbmeta descriptors."""
from __future__ import annotations
import struct, sys


def parse_boot(path):
    with open(path, "rb") as f:
        h = f.read(4096)
    if h[:8] != b"ANDROID!":
        return f"{path}: not ANDROID! boot image"

    # Try Android boot v3/v4 first (header_version at offset 40)
    header_version, = struct.unpack_from("<I", h, 40)
    out = [f"{path}:  magic=ANDROID!  header_version={header_version}"]
    if header_version >= 3:
        # v3/v4: kernel_size, ramdisk_size, os_version, header_size, reserved[4], header_version, cmdline[1536]
        ksz, rsz = struct.unpack_from("<II", h, 8)
        osver, hdr_sz = struct.unpack_from("<II", h, 16)
        cmdline = h[44:44 + 1536].rstrip(b"\x00").decode("latin-1")
        out.append(f"  kernel_size   = {ksz:,}")
        out.append(f"  ramdisk_size  = {rsz:,}")
        out.append(f"  os_version    = 0x{osver:08x} "
                   f"({(osver >> 25) & 0x7F}.{(osver >> 18) & 0x7F}.{(osver >> 11) & 0x7F}, "
                   f"spl={((osver >> 4) & 0x7F) + 2000}-{osver & 0xF:02d})")
        out.append(f"  header_size   = {hdr_sz}")
        out.append(f"  cmdline       = {cmdline!r}")
    else:
        # v0..v2: kernel_size, kernel_addr, ramdisk_size, ramdisk_addr, second_size, second_addr,
        # tags_addr, page_size, header_version, os_version, name[16], cmdline[512], id[32], extra_cmdline[1024]
        (ksz, kaddr, rsz, raddr, ssz, saddr, tags_addr,
         page, hv, osver) = struct.unpack_from("<IIIIIIIIII", h, 8)
        name = h[48:48+16].rstrip(b"\x00").decode("latin-1", "replace")
        cmdline = h[64:64+512].rstrip(b"\x00").decode("latin-1", "replace")
        extra = h[576+32:576+32+1024].rstrip(b"\x00").decode("latin-1", "replace")
        out.append(f"  kernel_size   = {ksz:,}  @ 0x{kaddr:08x}")
        out.append(f"  ramdisk_size  = {rsz:,}  @ 0x{raddr:08x}")
        out.append(f"  second_size   = {ssz:,}  @ 0x{saddr:08x}")
        out.append(f"  tags_addr     = 0x{tags_addr:08x}")
        out.append(f"  page_size     = {page}")
        out.append(f"  name          = {name!r}")
        out.append(f"  os_version    = 0x{osver:08x} "
                   f"({(osver >> 25) & 0x7F}.{(osver >> 18) & 0x7F}.{(osver >> 11) & 0x7F}, "
                   f"spl={((osver >> 4) & 0x7F) + 2000}-{osver & 0xF:02d})")
        out.append(f"  cmdline       = {cmdline!r}")
        if extra:
            out.append(f"  extra_cmdline = {extra!r}")
    return "\n".join(out)


# AVB vbmeta format: see external/avb/libavb/avb_vbmeta_image.h
# Header: 256 bytes
#   magic[4]               = "AVB0"
#   required_libavb_major  u32 BE
#   required_libavb_minor  u32 BE
#   authentication_data_block_size  u64 BE
#   auxiliary_data_block_size       u64 BE
#   algorithm_type                  u32 BE   (0=none, 1=SHA256_RSA2048, ... )
#   hash_offset, hash_size          u64 BE
#   signature_offset, signature_size  u64 BE
#   public_key_offset, public_key_size  u64 BE
#   public_key_metadata_offset, public_key_metadata_size  u64 BE
#   descriptors_offset, descriptors_size u64 BE
#   rollback_index                  u64 BE
#   flags                           u32 BE
#   rollback_index_location         u32 BE
#   release_string[48]
#   reserved[80]
AVB_ALG = {
    0: "NONE",
    1: "SHA256_RSA2048",
    2: "SHA256_RSA4096",
    3: "SHA256_RSA8192",
    4: "SHA512_RSA2048",
    5: "SHA512_RSA4096",
    6: "SHA512_RSA8192",
}

AVB_DESC = {
    0: "property",
    1: "hashtree",
    2: "hash",
    3: "kernel_cmdline",
    4: "chain_partition",
}


def parse_vbmeta(path):
    with open(path, "rb") as f:
        data = f.read()
    if data[:4] != b"AVB0":
        return f"{path}: not an AVB vbmeta image"
    (magic, maj, minr, auth_sz, aux_sz, alg, h_off, h_sz, sig_off, sig_sz,
     pk_off, pk_sz, pkm_off, pkm_sz, desc_off, desc_sz, rb_index, flags,
     rb_loc) = struct.unpack(">4sIIQQIQQQQQQQQQQQII", data[:128])
    release = data[176:224].rstrip(b"\x00").decode("latin-1", "replace")
    out = [f"{path}:  magic=AVB0  libavb {maj}.{minr}  alg={AVB_ALG.get(alg, alg)}"]
    out.append(f"  auth_block={auth_sz}  aux_block={aux_sz}")
    out.append(f"  rollback_index={rb_index}  rollback_index_location={rb_loc}  flags=0x{flags:08x}")
    out.append(f"  release_string={release!r}")
    # descriptors live in aux block starting at (256 + auth + desc_off)
    aux_start = 256 + auth_sz
    descriptors = data[aux_start + desc_off:aux_start + desc_off + desc_sz]
    out.append(f"  descriptors ({desc_sz} bytes):")
    i = 0
    while i + 16 <= len(descriptors):
        tag, nbf = struct.unpack(">QQ", descriptors[i:i+16])
        body = descriptors[i+16:i+16+nbf]
        kind = AVB_DESC.get(tag, f"tag{tag}")
        if tag == 0 and len(body) >= 16:
            # property: key_num_bytes u64 BE, value_num_bytes u64 BE, then key+NUL+value+NUL+pad
            kn, vn = struct.unpack(">QQ", body[:16])
            key = body[16:16+kn].decode("latin-1", "replace")
            val = body[16+kn+1:16+kn+1+vn].decode("latin-1", "replace")
            out.append(f"    property: {key}={val!r}")
        elif tag == 1 and len(body) >= 164:
            # AvbHashtreeDescriptor: fixed part 164 bytes (incl. 60B reserved), then name+salt+digest
            (dm_ver, img_size, tree_off, tree_sz, data_blk, hash_blk,
             fec_num_roots, fec_off, fec_sz) = struct.unpack(">IQQQIIIQQ", body[:56])
            hash_alg = body[56:88].rstrip(b"\x00").decode("latin-1", "replace")
            pn_len, salt_len, digest_len, flags_h = struct.unpack(">IIII", body[88:104])
            name_off = 164  # after 60B reserved
            partition = body[name_off:name_off+pn_len].decode("latin-1", "replace")
            salt = body[name_off+pn_len:name_off+pn_len+salt_len].hex()
            digest = body[name_off+pn_len+salt_len:name_off+pn_len+salt_len+digest_len].hex()
            out.append(f"    hashtree({partition}): size={img_size:,}  alg={hash_alg}  "
                       f"digest={digest[:16]}…  salt={salt[:16]}…  dm-verity v{dm_ver}")
        elif tag == 2 and len(body) >= 116:
            # AvbHashDescriptor: fixed part 116 bytes (incl. 60B reserved), then name+salt+digest
            img_size, = struct.unpack(">Q", body[:8])
            hash_alg = body[8:40].rstrip(b"\x00").decode("latin-1", "replace")
            pn_len, salt_len, digest_len, flags_h = struct.unpack(">IIII", body[40:56])
            name_off = 116  # after 60B reserved
            partition = body[name_off:name_off+pn_len].decode("latin-1", "replace")
            salt = body[name_off+pn_len:name_off+pn_len+salt_len].hex()
            digest = body[name_off+pn_len+salt_len:name_off+pn_len+salt_len+digest_len].hex()
            out.append(f"    hash({partition}): size={img_size:,}  alg={hash_alg}  "
                       f"digest={digest[:32]}…")
        elif tag == 3 and len(body) >= 8:
            flags_c, cl_len = struct.unpack(">II", body[:8])
            cl = body[8:8+cl_len].decode("latin-1", "replace")
            out.append(f"    kernel_cmdline (flags=0x{flags_c:x}): {cl!r}")
        elif tag == 4 and len(body) >= 76:
            rb_loc, pn_len, pk_len = struct.unpack(">III", body[:12])
            partition = body[76:76+pn_len].decode("latin-1", "replace")
            out.append(f"    chain_partition({partition}): rb_loc={rb_loc}  "
                       f"public_key_len={pk_len}")
        else:
            out.append(f"    {kind}: nbf={nbf}  (unparsed body {len(body)}B)")
        # descriptors are padded to multiples of 8
        step = 16 + nbf
        step = (step + 7) & ~7
        i += step
    return "\n".join(out)


if __name__ == "__main__":
    for p in sys.argv[1:]:
        if p.endswith("boot.img") or p.endswith("recovery.img"):
            print(parse_boot(p))
        else:
            print(parse_vbmeta(p))
        print()
