package com.auraplayer.ui.screens.equalizer

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.ads.AdManager
import com.auraplayer.core.audio.EqualizerManager
import com.auraplayer.ui.components.BounceButton
import com.auraplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    equalizerManager: EqualizerManager,
    adManager: AdManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val bands by equalizerManager.bandLevels.collectAsState()
    val bassBoost by equalizerManager.bassBoostStrength.collectAsState()
    val virtualizer by equalizerManager.virtualizerStrength.collectAsState()
    val currentPreset by equalizerManager.currentPreset.collectAsState()
    val isEnabled by equalizerManager.isEnabled.collectAsState()

    val presets = listOf(
        "Custom", "Club & Bass", "Vocal Clarity", "Rock",
        "Electronic", "Studio Flat", "Bass Booster", "Acoustic Warmth"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WhiteSubtle) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF6366F1), Accent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = White, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            "Studio 5-Band Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isEnabled) "Active · $currentPreset" else "Bypassed",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) Accent else WhiteDim
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        equalizerManager.setEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = Accent,
                        uncheckedTrackColor = SurfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            // Frequency Response Curve Canvas
            FrequencyCurveCanvas(
                bands = bands,
                isEnabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceVariant)
            )

            Spacer(Modifier.height(14.dp))

            // Presets Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = currentPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            equalizerManager.applyPreset(preset)
                        },
                        label = { Text(preset, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent,
                            selectedLabelColor = White,
                            containerColor = SurfaceElevated,
                            labelColor = WhiteMuted
                        ),
                        shape = CircleShape,
                        border = null
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 5 Band Sliders Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                equalizerManager.bandLabels.forEachIndexed { index, label ->
                    val level = bands.getOrElse(index) { 0 }
                    BandSliderColumn(
                        label = label,
                        levelDb = level,
                        enabled = isEnabled,
                        onLevelChange = { newDb ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            equalizerManager.setBandLevel(index, newDb)
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Bass Boost & Virtualizer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sub Bass", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("$bassBoost%", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = {
                                equalizerManager.setBassBoost(it.toInt())
                            },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = White,
                                activeTrackColor = Accent,
                                inactiveTrackColor = SurfaceVariant
                            )
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("3D Spatial", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("$virtualizer%", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = virtualizer.toFloat(),
                            onValueChange = {
                                equalizerManager.setVirtualizer(it.toInt())
                            },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = White,
                                activeTrackColor = Accent,
                                inactiveTrackColor = SurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Rewarded Video Ad CTA button: "Watch video to boost to 32-bit Studio Master EQ"
            Button(
                onClick = {
                    (context as? Activity)?.let { activity ->
                        adManager.showRewardedVideo(
                            activity = activity,
                            onRewarded = {
                                Toast.makeText(context, "🎉 Rewarded! Studio 32-bit Boost Activated!", Toast.LENGTH_LONG).show()
                                equalizerManager.applyPreset("Club & Bass")
                            },
                            onClosed = {}
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF312E81)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PlayCircle, null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Watch Short Video for Studio HD Boost",
                    color = Color(0xFFA5B4FC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BandSliderColumn(
    label: String,
    levelDb: Int,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${if (levelDb > 0) "+" else ""}$levelDb dB",
            fontSize = 11.sp,
            color = if (levelDb != 0) Accent else WhiteDim,
            fontWeight = FontWeight.Bold
        )

        Slider(
            value = levelDb.toFloat(),
            onValueChange = { onLevelChange(it.toInt()) },
            valueRange = -10f..10f,
            steps = 19,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = White,
                activeTrackColor = Accent,
                inactiveTrackColor = SurfaceVariant
            ),
            modifier = Modifier.width(62.dp)
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color = WhiteMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FrequencyCurveCanvas(
    bands: List<Int>,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // Draw flat center line
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx()
        )

        if (bands.isEmpty() || !isEnabled) return@Canvas

        val path = Path()
        val numPoints = bands.size
        val stepX = w / (numPoints + 1)

        val points = mutableListOf<Offset>()
        points.add(Offset(0f, midY))
        for (i in bands.indices) {
            val x = stepX * (i + 1)
            // levelDb: -10..+10 -> y offset: midY - (levelDb / 10f) * (h * 0.4f)
            val y = midY - (bands[i] / 10f) * (h * 0.38f)
            points.add(Offset(x, y))
        }
        points.add(Offset(w, midY))

        path.moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2f
            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }

        // Draw frequency curve stroke
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF6366F1), Color(0xFFA855F7), Accent)
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
