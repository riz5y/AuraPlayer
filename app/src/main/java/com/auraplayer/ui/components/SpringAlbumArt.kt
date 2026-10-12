package com.auraplayer.ui.components

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
