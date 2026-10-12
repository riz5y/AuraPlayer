package com.auraplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.auraplayer.ui.screens.nowplaying.DynamicColors
import com.auraplayer.ui.theme.DeepBlack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Abstract fluid vector curves & waveforms illustration.
 * ONLY animates when [isPlaying] is true. Freezes in place when paused.
 */
@Composable
fun IllustrationVisualizer(
    colors: DynamicColors?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = colors?.vibrant ?: Color(0xFF6C5CE7)
    val secondaryColor = colors?.dominant ?: Color(0xFF1E1E2E)

    // Infinite transition drives wave phase ONLY when isPlaying == true
    val transition = rememberInfiniteTransition(label = "wave_anim")
    val phase by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        )
    } else {
        remember { mutableFloatStateOf(0.8f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Ambient background smooth dynamic gradient (fills full screen seamlessly)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.40f),
                        secondaryColor.copy(alpha = 0.25f),
                        DeepBlack,
                        DeepBlack
                    )
                )
            )

            // Draw 4 harmonic abstract vector curves
            val waveConfigs = listOf(
                WaveConfig(amplitude = 60f, frequency = 1.2f, speedMultiplier = 1.0f, alpha = 0.75f, strokeWidth = 3.5f),
                WaveConfig(amplitude = 45f, frequency = 1.8f, speedMultiplier = -0.8f, alpha = 0.50f, strokeWidth = 2.5f),
                WaveConfig(amplitude = 75f, frequency = 0.9f, speedMultiplier = 1.3f, alpha = 0.40f, strokeWidth = 2.0f),
                WaveConfig(amplitude = 30f, frequency = 2.2f, speedMultiplier = -1.1f, alpha = 0.30f, strokeWidth = 1.5f)
            )

            val centerY = h * 0.48f

            waveConfigs.forEachIndexed { index, config ->
                val path = Path()
                val wavePhase = phase * config.speedMultiplier + (index * 0.8f)

                path.moveTo(0f, centerY)

                val step = 12f
                var x = 0f
                while (x <= w) {
                    val normalizedX = x / w
                    // Envelope factor: wave is bigger in the middle, pinches at the screen edges
                    val envelope = sin(normalizedX * PI).toFloat()
                    val y = centerY + sin((normalizedX * 2 * PI * config.frequency) + wavePhase).toFloat() * (config.amplitude * envelope)
                    path.lineTo(x, y)
                    x += step
                }

                val curveBrush = Brush.horizontalGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.1f),
                        primaryColor.copy(alpha = config.alpha),
                        Color.White.copy(alpha = config.alpha * 0.9f),
                        primaryColor.copy(alpha = config.alpha),
                        secondaryColor.copy(alpha = 0.1f)
                    )
                )

                drawPath(
                    path = path,
                    brush = curveBrush,
                    style = Stroke(
                        width = config.strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

private data class WaveConfig(
    val amplitude: Float,
    val frequency: Float,
    val speedMultiplier: Float,
    val alpha: Float,
    val strokeWidth: Float
)
