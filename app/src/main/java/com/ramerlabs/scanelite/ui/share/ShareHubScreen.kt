package com.ramerlabs.scanelite.ui.share

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramerlabs.scanelite.domain.ExportFormat
import com.ramerlabs.scanelite.ui.components.RamerLabsCredits
import com.ramerlabs.scanelite.ui.session.SessionViewModel
import com.ramerlabs.scanelite.ui.theme.SeBgElevated
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeEmerald
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ShareHubScreen(
    sessionViewModel: SessionViewModel,
    onDone: () -> Unit
) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    fun shareTo(packageName: String?, actionLabel: String, email: Boolean = false) {
        scope.launch {
            busy = true
            error = null
            try {
                sessionViewModel.finalizeDocument()
                val uri = sessionViewModel.createShareUri()
                val mime = if (state.exportFormat == ExportFormat.Pdf) "application/pdf" else "image/jpeg"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (email) {
                        putExtra(Intent.EXTRA_SUBJECT, "ScanElite — ${state.documentTitle}")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Scanned with ScanElite by RamerLabs · https://ramerlabs.com"
                        )
                    }
                    if (packageName != null) setPackage(packageName)
                }
                context.startActivity(Intent.createChooser(intent, actionLabel))
            } catch (e: Exception) {
                error = e.message ?: "Share failed"
            } finally {
                busy = false
            }
        }
    }

    fun saveToAlbum() {
        scope.launch {
            busy = true
            error = null
            try {
                val name = sessionViewModel.saveToGallery()
                val where = if (state.exportFormat == ExportFormat.Pdf) {
                    "Downloads/ScanElite"
                } else {
                    "Pictures/ScanElite"
                }
                savedMessage = "Saved \"$name\" to your phone album ($where)."
            } catch (e: Exception) {
                error = e.message ?: "Could not save to album"
            } finally {
                busy = false
            }
        }
    }

    if (savedMessage != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Saved successfully") },
            text = { Text(savedMessage ?: "") },
            confirmButton = {
                TextButton(
                    onClick = {
                        savedMessage = null
                        sessionViewModel.resetSession()
                        onDone()
                    }
                ) {
                    Text("OK", color = SeGold, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
            .padding(20.dp)
    ) {
        Text("Share Hub", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SeTextPrimary)
        Text(state.documentTitle, color = SeGold, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Text("Export format", color = SeTextSecondary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatPill("JPEG", state.exportFormat == ExportFormat.Jpeg) {
                sessionViewModel.setExportFormat(ExportFormat.Jpeg)
            }
            FormatPill("PDF", state.exportFormat == ExportFormat.Pdf) {
                sessionViewModel.setExportFormat(ExportFormat.Pdf)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { saveToAlbum() },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SeEmerald, contentColor = SeBgPrimary)
        ) {
            Text("Save to phone album", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Or share", color = SeTextSecondary, modifier = Modifier.padding(bottom = 8.dp))

        ShareTarget("WhatsApp") { shareTo("com.whatsapp", "WhatsApp") }
        ShareTarget("Telegram") { shareTo("org.telegram.messenger", "Telegram") }
        ShareTarget("Facebook") { shareTo("com.facebook.katana", "Facebook") }
        ShareTarget("Email") { shareTo(null, "Email", email = true) }

        if (busy) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SeGold)
            }
        }
        error?.let {
            Text(it, color = SeTextSecondary, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    sessionViewModel.finalizeDocument()
                    sessionViewModel.resetSession()
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SeBgElevated, contentColor = SeTextPrimary)
        ) {
            Text("Done without saving")
        }
        Spacer(modifier = Modifier.height(16.dp))
        RamerLabsCredits(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FormatPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, if (selected) SeGold else SeTextSecondary, RoundedCornerShape(999.dp))
            .background(if (selected) SeGold.copy(alpha = 0.2f) else SeBgElevated, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (selected) SeGold else SeTextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShareTarget(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(SeBgElevated, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = SeTextPrimary)
    }
}
