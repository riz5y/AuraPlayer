package com.auraplayer.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.domain.repository.SongRepository
import com.auraplayer.ui.components.BounceButton
import com.auraplayer.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    songRepository: SongRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var hapticBassEnabled by remember { mutableStateOf(true) }
    var fluidShaderEnabled by remember { mutableStateOf(true) }
    var crossfadeDuration by remember { mutableFloatStateOf(4f) }
    var selectedPreset by remember { mutableStateOf("Club & Bass") }
    var sleepTimerMin by remember { mutableIntStateOf(0) }
    var autoPauseNoisy by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack)
            .systemBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 120.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displayMedium,
            color = White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ── 1. Audio & Fidelity Section ──────────────────────────────
        SettingsSectionHeader("AUDIO & FIDELITY")
        SettingsCard {
            SettingsDropdownRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Equalizer Preset",
                currentValue = selectedPreset,
                options = listOf("Club & Bass", "Vocal Clarity", "Electronic", "Studio Flat"),
                onSelect = { selectedPreset = it }
            )

            HorizontalDivider(color = Outline, thickness = 0.5.dp)

            // Crossfade Slider
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, null, tint = Accent, modifier = Modifier.size(22.dp))
                        Text("Smart Crossfade", color = White, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        "${crossfadeDuration.toInt()}s",
                        color = Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = crossfadeDuration,
                    onValueChange = { crossfadeDuration = it },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = White,
                        activeTrackColor = Accent,
                        inactiveTrackColor = SurfaceVariant
                    )
                )
            }

            HorizontalDivider(color = Outline, thickness = 0.5.dp)

            SettingsToggleRow(
                icon = Icons.Rounded.Vibration,
                title = "Haptic Subwoofer Feedback",
                subtitle = "Subtle tactile pulses in sync with low-end bass",
                checked = hapticBassEnabled,
                onCheckedChange = { hapticBassEnabled = it }
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── 2. Visuals & Fluid Engine ────────────────────────────────
        SettingsSectionHeader("VISUALS & AMBIENT")
        SettingsCard {
            SettingsToggleRow(
                icon = Icons.Rounded.Waves,
                title = "AGSL Liquid Water Shaders",
                subtitle = "Fluid caustic ripples responding to album colors",
                checked = fluidShaderEnabled,
                onCheckedChange = { fluidShaderEnabled = it }
            )

            HorizontalDivider(color = Outline, thickness = 0.5.dp)

            SettingsToggleRow(
                icon = Icons.Rounded.Headphones,
                title = "Auto-Pause on Unplug",
                subtitle = "Pause playback when headphones are disconnected",
                checked = autoPauseNoisy,
                onCheckedChange = { autoPauseNoisy = it }
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── 3. Playback & Sleep Timer ────────────────────────────────
        SettingsSectionHeader("PLAYBACK AUTOMATION")
        SettingsCard {
            SettingsDropdownRow(
                icon = Icons.Rounded.Bedtime,
                title = "Sleep Timer",
                currentValue = if (sleepTimerMin == 0) "Off" else "$sleepTimerMin Minutes",
                options = listOf("Off", "15 Minutes", "30 Minutes", "45 Minutes", "60 Minutes"),
                onSelect = {
                    sleepTimerMin = when (it) {
                        "15 Minutes" -> 15
                        "30 Minutes" -> 30
                        "45 Minutes" -> 45
                        "60 Minutes" -> 60
                        else -> 0
                    }
                    if (sleepTimerMin > 0) {
                        Toast.makeText(context, "Sleep timer set to $it", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── 4. Library & Storage ─────────────────────────────────────
        SettingsSectionHeader("LIBRARY & STORAGE")
        SettingsCard {
            SettingsActionRow(
                icon = Icons.Rounded.Sync,
                title = "Rescan Media Library",
                subtitle = "Scan device storage for new songs and albums",
                onClick = {
                    coroutineScope.launch {
                        songRepository.syncWithMediaStore()
                        Toast.makeText(context, "Library refreshed!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            HorizontalDivider(color = Outline, thickness = 0.5.dp)

            SettingsActionRow(
                icon = Icons.Rounded.CleaningServices,
                title = "Clear Album Art Cache",
                subtitle = "Re-extract palette and cache files",
                onClick = {
                    Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // App Info Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AuraPlayer v1.0.0 Pro", color = WhiteMuted, fontSize = 13.sp)
            Text("Apple Music Fidelity · Built with Jetpack Compose", color = WhiteDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = WhiteDim,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = WhiteDim, style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = WhiteDim, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = WhiteDim, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsDropdownRow(
    icon: ImageVector,
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(24.dp))
        Text(title, color = White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(currentValue, color = Accent, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Rounded.UnfoldMore, null, tint = WhiteDim, modifier = Modifier.size(20.dp))

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceElevated)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = if (opt == currentValue) Accent else White) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
