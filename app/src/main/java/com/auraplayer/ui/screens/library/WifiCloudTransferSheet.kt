package com.auraplayer.ui.screens.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.core.transfer.WifiTransferServer
import com.auraplayer.domain.repository.SongRepository
import com.auraplayer.ui.components.BounceButton
import com.auraplayer.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiCloudTransferSheet(
    wifiServer: WifiTransferServer,
    songRepository: SongRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serverUrl by wifiServer.statusFlow.collectAsState()

    val cloudPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val count = songRepository.importSongsFromUris(uris)
                Toast.makeText(
                    context,
                    if (count > 0) "Imported $count songs from cloud!" else "No audio files selected",
                    Toast.LENGTH_SHORT
                ).show()
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WhiteSubtle) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6366F1), Accent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Devices,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "PC & Cloud Transfer Hub",
                style = MaterialTheme.typography.displayMedium,
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Transfer music files wirelessly from any PC, Mac, or Google Drive",
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // ── Section 1: Google Drive & Cloud ───────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.CloudDownload, null, tint = Accent, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive & Cloud Drives", color = White, fontWeight = FontWeight.SemiBold)
                            Text("Pick songs stored in your personal cloud accounts", color = WhiteDim, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            cloudPickerLauncher.launch(
                                arrayOf("audio/*", "application/ogg", "application/octet-stream")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.FolderOpen, null, tint = White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Google Drive / Files", color = White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Section 2: Wi-Fi PC Drag & Drop Server ────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (serverUrl != null) Color(0xFF1E293B) else SurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Wifi,
                            null,
                            tint = if (serverUrl != null) Color(0xFF10B981) else Accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (serverUrl != null) "Wi-Fi Server Running" else "PC Web Drag & Drop",
                                color = White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (serverUrl != null) "Active on your local Wi-Fi" else "Upload songs from your computer browser",
                                color = WhiteDim,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (serverUrl != null) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = serverUrl ?: "",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                BounceButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Aura Server", serverUrl))
                                        Toast.makeText(context, "Address copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, null, tint = White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "👉 Type this address in Chrome, Edge, or Safari on your PC to drag & drop music directly onto this phone.",
                            color = WhiteMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (serverUrl == null) {
                                val url = wifiServer.startServer { savedFile ->
                                    // Notify MediaScanner
                                    MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(savedFile.absolutePath),
                                        null
                                    ) { _, uri ->
                                        scope.launch {
                                            if (uri != null) {
                                                songRepository.importSongsFromUris(listOf(uri))
                                            } else {
                                                songRepository.syncWithMediaStore()
                                            }
                                        }
                                    }
                                }
                                if (url == null) {
                                    Toast.makeText(context, "Connect to Wi-Fi first to start transfer", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                wifiServer.stopServer()
                                Toast.makeText(context, "Wi-Fi transfer stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (serverUrl != null) Color(0xFFDC2626) else Accent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (serverUrl != null) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (serverUrl != null) "Stop Wi-Fi Transfer" else "Start Wi-Fi PC Server",
                            color = White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            BounceButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", color = White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
