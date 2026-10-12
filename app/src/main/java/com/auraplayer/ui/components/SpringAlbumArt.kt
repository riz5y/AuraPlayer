package com.auraplayer.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Album art that:
 * - Scales UP (1.0) + shadow UP when playing   — energetic, alive
 * - Scales DOWN (0.92) + shadow DOWN on pause  — like Apple Music
 * - Spring physics so it bounces naturally
 */
@Composable
fun SpringAlbumArt(
    artworkUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "art_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPlaying) 32.dp else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "art_elev"
    )

    Card(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { AlbumArtPlaceholder() },
            error = { AlbumArtPlaceholder() }
        )
    }
}

@Composable
private fun AlbumArtPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2C2448), Color(0xFF6C5CE7), Color(0xFF1B1B2F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(72.dp)
        )
    }
}
