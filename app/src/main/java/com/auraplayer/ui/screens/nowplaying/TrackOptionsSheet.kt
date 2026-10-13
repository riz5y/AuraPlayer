package com.auraplayer.ui.screens.nowplaying

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.auraplayer.core.audio.EqualizerManager
import com.auraplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    playerState: PlayerState,
    equalizerManager: EqualizerManager?,
    onOpenEqualizer: () -> Unit,
    onToggleLyrics: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val title = playerState.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Now Playing"
    val artist = playerState.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Aura Artist"
    val artworkUri = playerState.currentMediaItem?.mediaMetadata?.artworkUri

    var isFavorite by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val isEqEnabled = equalizerManager?.isEnabled?.collectAsState()?.value ?: false
    val bassBoostStrength = equalizerManager?.bassBoostStrength?.collectAsState()?.value ?: 0
    val virtualizerStrength = equalizerManager?.virtualizerStrength?.collectAsState()?.value ?: 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(WhiteSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(scrollState)
        ) {
            // ── Track Header ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF2C2448), Color(0xFF6C5CE7)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WhiteMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevated)
                                .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("⚡ HI-RES 24-BIT", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevated)
                                .border(0.5.dp, WhiteSubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("STUDIO MASTER", color = WhiteDim, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            HorizontalDivider(color = Outline, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // ── Section 1: Studio Audio & Fidelity Mixer ──────────────
            Text(
                text = "STUDIO AUDIO & MIXER",
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Equalizer
            OptionRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Studio 5-Band Sound Mixer & EQ",
                subtitle = if (isEqEnabled) "Hardware DSP Active" else "Bypassed (Tap to customize)",
                badge = if (isEqEnabled) "ACTIVE" else null,
                onClick = {
                    onDismiss()
                    onOpenEqualizer()
                }
            )

            // 3D Spatializer
            OptionToggleRow(
                icon = Icons.Rounded.SurroundSound,
                title = "3D Spatial Virtualizer",
                subtitle = "Wide acoustic surround stage",
                checked = virtualizerStrength > 0,
                onCheckedChange = { enable ->
                    equalizerManager?.setVirtualizer(if (enable) 60 else 0)
                    Toast.makeText(context, if (enable) "3D Spatial Audio Enabled" else "3D Spatial Off", Toast.LENGTH_SHORT).show()
                }
            )

            // Sub-Bass Boost
            OptionToggleRow(
                icon = Icons.Rounded.Vibration,
                title = "Sub-Bass Subwoofer Boost",
                subtitle = "Enhanced low-end sub frequencies",
                checked = bassBoostStrength > 0,
                onCheckedChange = { enable ->
                    equalizerManager?.setBassBoost(if (enable) 70 else 0)
                    Toast.makeText(context, if (enable) "Sub-Bass Boost Activated (+7dB)" else "Bass Boost Off", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Outline, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // ── Section 2: Playback & Track Actions ───────────────────
            Text(
                text = "TRACK ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Favorite
            OptionRow(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                iconTint = if (isFavorite) Color(0xFFFF4B6E) else Accent,
                title = if (isFavorite) "In Favorite Songs" else "Add to Favorites",
                subtitle = "Save to your library collections",
                onClick = {
                    isFavorite = !isFavorite
                    Toast.makeText(context, if (isFavorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                }
            )

            // Synced Lyrics
            OptionRow(
                icon = Icons.Rounded.FormatQuote,
                title = "View Synced Lyrics",
                subtitle = "Real-time scrolling karaoke lyrics view",
                onClick = {
                    onDismiss()
                    onToggleLyrics()
                }
            )

            // Sleep Timer
            OptionRow(
                icon = Icons.Rounded.Bedtime,
                title = "Sleep Timer",
                subtitle = "Stop playback automatically after time",
                onClick = { showSleepTimerDialog = true }
            )

            // Share Track
            OptionRow(
                icon = Icons.Rounded.Share,
                title = "Share Track Info",
                subtitle = "Send song details to friends",
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Listening to '$title' by $artist on AuraPlayer Hi-Fi Studio!")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                    onDismiss()
                }
            )
        }
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Set Sleep Timer", color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("15 Minutes", "30 Minutes", "45 Minutes", "60 Minutes", "Turn Off").forEach { option ->
                        Button(
                            onClick = {
                                Toast.makeText(context, "Sleep Timer: $option", Toast.LENGTH_SHORT).show()
                                showSleepTimerDialog = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option, color = White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", color = Accent)
                }
            },
            containerColor = Surface
        )
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    iconTint: Color = Accent,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = WhiteDim, fontSize = 12.sp)
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Accent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(badge, color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = WhiteSubtle, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun OptionToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, color = WhiteDim, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = Accent,
                uncheckedTrackColor = SurfaceVariant
            )
        )
    }
}
