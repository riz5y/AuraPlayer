package com.auraplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.ui.theme.WhiteDim
import com.auraplayer.ui.theme.WhiteMuted

/**
 * Custom seek bar — no Material Slider, fully custom drawn.
 * Features:
 * - Thumb grows on drag (spring)
 * - Track has rounded caps
 * - Smooth scrub feel
 */
@Composable
fun AuraSeekBar(
    position: Long,
    duration: Long,
    trackColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackWidth by remember { mutableFloatStateOf(0f) }

    val fraction = if (isDragging) dragFraction
    else if (duration > 0L) (position / duration.toFloat()).coerceIn(0f, 1f)
    else 0f

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.4f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "thumb_scale"
    )

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / trackWidth).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / trackWidth).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek((dragFraction * duration).toLong())
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false }
                    )
                }
        ) {
            trackWidth = size.width
            val trackHeight = 4.dp.toPx()
            val cy = size.height / 2f
            val thumbRadius = 6.dp.toPx() * thumbScale

            // Background track
            drawRoundRect(
                color = trackColor.copy(alpha = 0.25f),
                topLeft = Offset(0f, cy - trackHeight / 2),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )

            // Progress track
            val progressWidth = (size.width * fraction).coerceAtLeast(thumbRadius)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, cy - trackHeight / 2),
                size = Size(progressWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2)
            )

            // Thumb
            drawCircle(
                color = trackColor,
                radius = thumbRadius,
                center = Offset(size.width * fraction, cy)
            )
        }

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(if (isDragging) (dragFraction * duration).toLong() else position),
                color = WhiteMuted,
                fontSize = 12.sp
            )
            Text(
                text = formatMs(duration),
                color = WhiteDim,
                fontSize = 12.sp
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
