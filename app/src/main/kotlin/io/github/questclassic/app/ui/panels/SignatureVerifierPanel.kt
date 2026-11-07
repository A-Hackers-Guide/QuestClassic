package io.github.questclassic.app.ui.panels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.questclassic.app.core.AppContainer
import io.github.questclassic.app.core.PayloadVerifier
import io.github.questclassic.app.ui.nostalgic.GlassPanel
import io.github.questclassic.app.ui.nostalgic.StatRow
import io.github.questclassic.app.ui.theme.QCColors
import kotlinx.coroutines.launch

/**
 * UI for verifying a Meta OTA .zip from Downloads against its own otacert.pem — same two
 * signatures the Quest bootloader checks at install time.
 *
 * You tap "Pick OTA", choose the .zip, the app computes both SHA-256 digests and RSA-verifies
 * them. All on-device; no network, no upload.
 */
@Composable
fun SignatureVerifierPanel(container: AppContainer, onClose: () -> Unit) {
    var report by remember { mutableStateOf<PayloadVerifier.Report?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true; report = null; error = null
            container.payloadVerifier.verify(uri)
                .onSuccess { report = it }
                .onFailure { error = it.message ?: it::class.simpleName }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        PanelClose(
            title = "Signature Verifier",
            subtitle = report?.archiveName ?: if (busy) "Verifying…" else "Pick an OTA .zip to check",
            onClose = onClose,
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                enabled = !busy,
                onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = QCColors.Teal, contentColor = QCColors.Space),
            ) { Text(if (busy) "Verifying…" else "Pick OTA") }
        }
        Spacer(Modifier.height(16.dp))

        error?.let { err ->
            GlassPanel(modifier = Modifier.fillMaxWidth(), accent = QCColors.Warn) {
                Column {
                    Text("FAILED", style = MaterialTheme.typography.labelLarge, color = QCColors.Warn)
                    Spacer(Modifier.height(6.dp))
                    Text(err, style = MaterialTheme.typography.bodyMedium, color = QCColors.Soft)
                }
            }
        }

        report?.let { r ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = if (r.allGood) QCColors.OK else QCColors.Warn,
                    ) {
                        Column {
                            Text(
                                if (r.allGood) "SIGNATURES VALID" else "ONE OR BOTH SIGNATURES FAILED",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (r.allGood) QCColors.OK else QCColors.Warn,
                            )
                            Spacer(Modifier.height(6.dp))
                            StatRow("metadata signature", if (r.metadataSignatureValid) "✓ valid" else "✗ invalid", r.metadataSignatureValid)
                            StatRow("final signature", if (r.finalSignatureValid) "✓ valid" else "✗ invalid", r.finalSignatureValid)
                        }
                    }
                }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("PAYLOAD HEADER", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                            Spacer(Modifier.height(6.dp))
                            StatRow("payload size", "${r.payloadSize}")
                            StatRow("manifest size", "${r.manifestSize}")
                            StatRow("metadata sig size", "${r.metaSigSize}")
                            StatRow("signatures offset", "${r.signaturesOffset}")
                            StatRow("signatures size", "${r.signaturesSize}")
                        }
                    }
                }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("DIGESTS", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                            Spacer(Modifier.height(6.dp))
                            Text("metadata SHA-256", style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                            Text(r.metadataDigestHex, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
                            Spacer(Modifier.height(8.dp))
                            Text("final SHA-256", style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                            Text(r.finalDigestHex, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
                        }
                    }
                }
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("OTA CERT", style = MaterialTheme.typography.labelLarge, color = QCColors.Teal)
                            Spacer(Modifier.height(6.dp))
                            StatRow("subject", r.certSubject)
                            StatRow("issuer", r.certIssuer)
                            StatRow("not after", r.certNotAfter)
                            Spacer(Modifier.height(8.dp))
                            Text("SHA-256 fingerprint", style = MaterialTheme.typography.labelLarge, color = QCColors.Dim)
                            Text(r.certSha256Fingerprint, style = MaterialTheme.typography.labelMedium, color = QCColors.Soft)
                        }
                    }
                }
            }
        }
    }
}
