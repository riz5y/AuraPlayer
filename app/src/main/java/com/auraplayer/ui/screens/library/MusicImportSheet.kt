package com.auraplayer.ui.screens.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.domain.repository.SongRepository
import com.auraplayer.ui.components.BounceButton
import com.auraplayer.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicImportSheet(
    songRepository: SongRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var showWifiGuide by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isImporting = true
            scope.launch {
                val count = songRepository.importSongsFromUris(uris)
                isImporting = false
                Toast.makeText(
                    context,
                    if (count > 0) "Successfully imported $count songs to Library!" else "No valid audio files found",
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
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6366F1), Accent))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudUpload,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Import Music to Aura",
                style = MaterialTheme.typography.displayMedium,
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "100% Offline & Private · Google Play Compliant",
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (isImporting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Accent)
                    Spacer(Modifier.height(12.dp))
                    Text("Indexing audio files & tags…", color = WhiteMuted)
                }
            } else {
                // Option 1: File Picker (SAF)
                ImportOptionCard(
                    icon = Icons.Rounded.FolderOpen,
                    title = "Browse Device & SD Card",
                    subtitle = "Select MP3, FLAC, WAV, AAC files via Android Document Picker",
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "audio/*",
                                "audio/mpeg",
                                "audio/mp4",
                                "audio/flac",
                                "audio/x-wav",
                                "audio/ogg"
                            )
                        )
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Option 2: Wi-Fi / PC Transfer Info
                ImportOptionCard(
                    icon = Icons.Rounded.Devices,
                    title = "PC / Wi-Fi & Cloud Transfer",
                    subtitle = "How to drag & drop audio from your computer or Google Drive",
                    onClick = {
                        showWifiGuide = !showWifiGuide
                    }
                )

                if (showWifiGuide) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Quick Wi-Fi & PC Setup:",
                                color = White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "1. Connect your phone via USB or local Wi-Fi share.\n" +
                                "2. Drag music files into the phone's 'Music' or 'Download' folder.\n" +
                                "3. Or download tracks from your private Google Drive.\n" +
                                "4. Aura will automatically detect and play them in studio quality!",
                                color = WhiteMuted,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            BounceButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Close",
                        color = White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = WhiteDim, fontSize = 12.sp, lineHeight = 16.sp)
            }

            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = WhiteDim,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
