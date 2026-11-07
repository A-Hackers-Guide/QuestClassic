package io.github.questclassic.app.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipInputStream

/**
 * Native Kotlin port of `tools/verify_payload.py`. Verifies both signatures inside an Android A/B
 * update_engine `payload.bin` (metadata signature + final payload signature) against an OTA's
 * `otacert.pem`.
 *
 * Reproduces exactly what the Meta Quest bootloader does during OTA verification:
 *   1. metadata signature covers bytes [0 .. 24 + manifest_size)       — SHA-256, RSA-2048
 *   2. final signature    covers bytes [0..24+manifest_size) ∪ [data_blobs_start .. final_sig_at)
 *                                                                      — SHA-256, RSA-2048
 *      (the metadata signature bytes are skipped — they aren't persisted to disk during install)
 *
 * Give it an OTA .zip from Downloads; it extracts payload.bin + otacert.pem + payload_properties
 * from the ZIP's central directory on the fly (no full extraction of the 1+ GB payload needed).
 */
class PayloadVerifier(private val context: Context) {

    data class Report(
        val archiveName: String,
        val payloadSize: Long,
        val manifestSize: Long,
        val metaSigSize: Int,
        val signaturesOffset: Long,
        val signaturesSize: Int,
        val metadataDigestHex: String,
        val finalDigestHex: String,
        val metadataSignatureValid: Boolean,
        val finalSignatureValid: Boolean,
        val certSubject: String,
        val certIssuer: String,
        val certNotAfter: String,
        val certSha256Fingerprint: String,
    ) {
        val allGood: Boolean get() = metadataSignatureValid && finalSignatureValid
    }

    suspend fun verify(archiveUri: Uri): Result<Report> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val name = resolver.query(archiveUri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME).coerceAtLeast(0)
                if (c.moveToFirst()) c.getString(idx) else "update.zip"
            } ?: "update.zip"

            // Extract the small files we need from the zip (streamed); keep payload.bin as a
            // random-access temp file so we can seek over its sections without loading all 1 GB.
            var otacertPem: ByteArray? = null
            val payloadFile = java.io.File.createTempFile("payload-", ".bin", context.cacheDir).apply { deleteOnExit() }
            try {
                resolver.openInputStream(archiveUri)?.use { raw ->
                    ZipInputStream(raw).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            when {
                                entry.name.endsWith("/payload.bin") -> {
                                    payloadFile.outputStream().use { out -> zip.copyTo(out) }
                                }
                                entry.name.endsWith("/META-INF/com/android/otacert") -> {
                                    otacertPem = zip.readBytes()
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
                require(otacertPem != null) { "otacert not found in archive" }
                require(payloadFile.length() > 24) { "payload.bin missing or truncated" }

                val cert = loadPemCert(otacertPem!!)
                val publicKey = cert.publicKey

                // Header parse — big-endian
                java.io.RandomAccessFile(payloadFile, "r").use { f ->
                    val magic = ByteArray(4).also { f.readFully(it) }
                    require(magic.contentEquals("CrAU".toByteArray())) { "not a CrAU payload" }
                    val version = f.readLong()
                    require(version >= 2) { "unsupported payload version $version" }
                    val manifestSize = f.readLong()
                    val metaSigSize = f.readInt()
                    require(manifestSize in 1..(1L shl 24)) { "insane manifest_size" }
                    require(metaSigSize in 1..65536) { "insane meta_sig_size" }

                    // --- metadata signature digest: SHA-256 over bytes [0 .. 24 + manifest_size)
                    val metadataDigest = run {
                        val md = MessageDigest.getInstance("SHA-256")
                        f.seek(0)
                        streamToDigest(f, 24 + manifestSize, md)
                        md.digest()
                    }
                    // read the metadata signature blob
                    val metaSigBlob = ByteArray(metaSigSize).also { f.readFully(it) }
                    val metaRawSig = parseSignaturesBlob(metaSigBlob)
                        ?: error("couldn't parse metadata signatures blob")

                    // --- parse manifest enough to find signatures_offset + signatures_size
                    val manifest = ByteArray(manifestSize.toInt()).also { f.seek(24); f.readFully(it) }
                    val (sigsOff, sigsSz) = parseManifestSigs(manifest)
                        ?: error("manifest missing signatures_offset / _size")

                    val dataBlobsStart = 24L + manifestSize + metaSigSize
                    val finalSigAt = dataBlobsStart + sigsOff

                    // --- final signature digest: SHA-256 over [0..24+msz) ∪ [data_blobs_start..final_sig_at)
                    val finalDigest = run {
                        val md = MessageDigest.getInstance("SHA-256")
                        f.seek(0)
                        streamToDigest(f, 24 + manifestSize, md)
                        f.seek(dataBlobsStart)
                        streamToDigest(f, finalSigAt - dataBlobsStart, md)
                        md.digest()
                    }
                    f.seek(finalSigAt)
                    val finalSigBlob = ByteArray(sigsSz).also { f.readFully(it) }
                    val finalRawSig = parseSignaturesBlob(finalSigBlob)
                        ?: error("couldn't parse final signatures blob")

                    val metaValid = rsaVerifySha256(publicKey, metadataDigest, metaRawSig)
                    val finalValid = rsaVerifySha256(publicKey, finalDigest, finalRawSig)

                    Report(
                        archiveName = name,
                        payloadSize = payloadFile.length(),
                        manifestSize = manifestSize,
                        metaSigSize = metaSigSize,
                        signaturesOffset = sigsOff,
                        signaturesSize = sigsSz,
                        metadataDigestHex = metadataDigest.toHex(),
                        finalDigestHex = finalDigest.toHex(),
                        metadataSignatureValid = metaValid,
                        finalSignatureValid = finalValid,
                        certSubject = cert.subjectX500Principal.name,
                        certIssuer = cert.issuerX500Principal.name,
                        certNotAfter = cert.notAfter.toString(),
                        certSha256Fingerprint = sha256Fingerprint(cert),
                    )
                }
            } finally {
                payloadFile.delete()
            }
        }
    }

    // ------- helpers -------

    private fun streamToDigest(f: java.io.RandomAccessFile, bytes: Long, md: MessageDigest) {
        val buf = ByteArray(1 shl 20) // 1 MiB
        var remaining = bytes
        while (remaining > 0) {
            val n = f.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
            if (n <= 0) break
            md.update(buf, 0, n)
            remaining -= n
        }
    }

    private fun rsaVerifySha256(publicKey: java.security.PublicKey, digest: ByteArray, signature: ByteArray): Boolean {
        return runCatching {
            // We have a precomputed SHA-256 digest + a PKCS#1 v1.5 signature over DigestInfo(SHA-256).
            // Java's Signature API with "NONEwithRSA" + the full DigestInfo prefix is the standard
            // way to verify a raw digest.
            val digestInfoPrefix = byteArrayOf(
                0x30, 0x31, 0x30, 0x0d, 0x06, 0x09, 0x60.toByte(), 0x86.toByte(), 0x48, 0x01,
                0x65, 0x03, 0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20,
            )
            val toVerify = digestInfoPrefix + digest
            val sig = Signature.getInstance("NONEwithRSA")
            sig.initVerify(publicKey)
            sig.update(toVerify)
            sig.verify(signature)
        }.getOrDefault(false)
    }

    private fun loadPemCert(pem: ByteArray): X509Certificate {
        val input: InputStream = pem.inputStream()
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(input) as X509Certificate
    }

    private fun sha256Fingerprint(cert: X509Certificate): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(cert.encoded).joinToString(":") { "%02X".format(it) }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    /**
     * Parse an update_engine `Signatures` protobuf message and extract the raw RSA signature bytes
     * (field 2 inside the first Signature submessage).
     */
    private fun parseSignaturesBlob(blob: ByteArray): ByteArray? {
        var i = 0
        while (i < blob.size) {
            val (tag, newI) = readVarint(blob, i); i = newI
            val field = tag.toInt() shr 3
            val wire = tag.toInt() and 7
            if (wire == 2) {
                val (ln, next) = readVarint(blob, i); i = next
                val sub = blob.copyOfRange(i, i + ln.toInt())
                i += ln.toInt()
                if (field == 1) {
                    var j = 0
                    while (j < sub.size) {
                        val (t2, nj) = readVarint(sub, j); j = nj
                        val f2 = t2.toInt() shr 3
                        val w2 = t2.toInt() and 7
                        if (w2 == 0) { j = readVarint(sub, j).second }
                        else if (w2 == 2) {
                            val (ln2, nj2) = readVarint(sub, j); j = nj2
                            val d = sub.copyOfRange(j, j + ln2.toInt()); j += ln2.toInt()
                            if (f2 == 2) return d
                        } else break
                    }
                }
            } else if (wire == 0) {
                i = readVarint(blob, i).second
            } else break
        }
        return null
    }

    private fun parseManifestSigs(manifest: ByteArray): Pair<Long, Int>? {
        var i = 0
        var sigsOff: Long? = null
        var sigsSz: Int? = null
        while (i < manifest.size) {
            val (tag, newI) = readVarint(manifest, i); i = newI
            val field = tag.toInt() shr 3
            val wire = tag.toInt() and 7
            when (wire) {
                0 -> {
                    val (v, next) = readVarint(manifest, i); i = next
                    if (field == 4) sigsOff = v
                    else if (field == 5) sigsSz = v.toInt()
                }
                1 -> i += 8
                2 -> {
                    val (ln, next) = readVarint(manifest, i); i = next
                    i += ln.toInt()
                }
                5 -> i += 4
                else -> return null
            }
            if (sigsOff != null && sigsSz != null) return sigsOff!! to sigsSz!!
        }
        return if (sigsOff != null && sigsSz != null) sigsOff!! to sigsSz!! else null
    }

    /** Read a protobuf varint. Returns (value, newIndex). */
    private fun readVarint(buf: ByteArray, start: Int): Pair<Long, Int> {
        var shift = 0
        var value = 0L
        var i = start
        while (true) {
            val b = buf[i].toInt() and 0xFF
            i += 1
            value = value or ((b.toLong() and 0x7F) shl shift)
            if (b and 0x80 == 0) return value to i
            shift += 7
            if (shift > 70) error("varint too long")
        }
    }
}
