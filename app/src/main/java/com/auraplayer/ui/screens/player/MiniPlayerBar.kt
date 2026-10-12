package com.auraplayer.ui.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import coil3.request.crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.auraplayer.ui.components.BounceButton
import com.auraplayer.ui.screens.nowplaying.PlayerState
import com.auraplayer.ui.screens.nowplaying.DynamicColors
import com.auraplayer.ui.theme.*

/**
 * Floating mini-player bar.
 *
 * Design decisions (user confirmed):
 *  - Fully opaque with album-art dynamic gradient
 *  - 28dp rounded corners
 *  - Thin glowing progress bar at bottom edge
 *  - Corner radius lerps from 28dp → 20dp as sheet expands
 */
@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    dynamicColors: DynamicColors,
    /** 0f = fully collapsed (mini), 1f = fully expanded (full screen) */
    sheetProgress: Float,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
) {
    // Corner radius: 28dp collapsed → 20dp expanded
    val cornerRadius by animateDpAsState(
        targetValue = androidx.compose.ui.unit.lerp(28.dp, 20.dp, sheetProgress),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner_radius"
    )

    // Fade out mini-player content as sheet expands
    val contentAlpha = (1f - sheetProgress * 2.5f).coerceIn(0f, 1f)

    // Progress fraction
    val progress = if (playerState.duration > 0L)
        (playerState.currentPosition / playerState.duration.toFloat()).coerceIn(0f, 1f)
    else 0f

    // Animate gradient colors from album art
    val bgStart by animateColorAsState(
        targetValue = dynamicColors.dominant,
        animationSpec = tween(800),
        label = "bg_start"
    )
    val bgEnd by animateColorAsState(
        targetValue = dynamicColors.vibrant,
        animationSpec = tween(1000),
        label = "bg_end"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
            .background(
                Brush.horizontalGradient(listOf(bgStart, bgEnd))
            )
    ) {
        // ── Main content row ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(playerState.currentMediaItem?.mediaMetadata?.artworkUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerState.currentMediaItem?.mediaMetadata
                        ?.title?.toString() ?: "Not Playing",
                    color = White.copy(alpha = contentAlpha),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playerState.currentMediaItem?.mediaMetadata
                        ?.artist?.toString() ?: "—",
                    color = WhiteMuted.copy(alpha = contentAlpha),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Skip next
            BounceButton(onClick = onSkipNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = White.copy(alpha = contentAlpha),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Play / Pause
            BounceButton(onClick = onPlayPause) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = contentAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying)
                            Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = dynamicColors.dominant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── Glowing progress bar at bottom edge (User choice: B) ─────
        GlowingProgressBar(
            progress = progress,
            color = dynamicColors.onContent,
            alpha = contentAlpha,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GlowingProgressBar(
    progress: Float,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val trackWidth = size.width
        val cy = size.height / 2f

        // Track background
        drawLine(
            color = color.copy(alpha = 0.2f * alpha),
            start = Offset(0f, cy),
            end = Offset(trackWidth, cy),
            strokeWidth = size.height,
            cap = StrokeCap.Round
        )

        // Filled progress — glowing
        if (progress > 0f) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f * alpha),
                        color.copy(alpha = 0.6f * alpha)
                    ),
                    endX = trackWidth * progress
                ),
                start = Offset(0f, cy),
                end = Offset(trackWidth * progress, cy),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
        }
    }
}
