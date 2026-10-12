package com.auraplayer.ui.screens.nowplaying

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.auraplayer.ui.components.AuraSeekBar
import com.auraplayer.ui.components.BounceButton
import com.auraplayer.ui.components.SpringAlbumArt
import com.auraplayer.ui.theme.*

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val dynamicColors by viewModel.dynamicColors.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-extract colors when track changes
    LaunchedEffect(playerState.currentMediaItem?.mediaId) {
        val uri = playerState.currentMediaItem?.mediaMetadata?.artworkUri
        viewModel.extractColors(context, uri)
    }

    // Animate full background gradient smoothly between songs
    val bgTop by animateColorAsState(
        targetValue = dynamicColors.dominant,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bg_top"
    )
    val bgBottom by animateColorAsState(
        targetValue = dynamicColors.vibrant.copy(alpha = 0.7f),
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "bg_bottom"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(bgTop, bgBottom, DeepBlack))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // Drag handle pill
            Box(
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(WhiteSubtle)
            )

            Spacer(Modifier.height(32.dp))

            // ── Album Art ────────────────────────────────────────────
            SpringAlbumArt(
                artworkUri = playerState.currentMediaItem?.mediaMetadata?.artworkUri,
                isPlaying = playerState.isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(Modifier.height(36.dp))

            // ── Song info + love button ───────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.currentMediaItem?.mediaMetadata?.title?.toString()
                            ?: "Not Playing",
                        style = MaterialTheme.typography.displayMedium,
                        color = White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = playerState.currentMediaItem?.mediaMetadata?.artist?.toString()
                            ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = WhiteMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BounceButton(onClick = { /* TODO: toggle favourite */ }) {
                    Icon(
                        imageVector = Icons.Rounded.FavoriteBorder,
                        contentDescription = "Love",
                        tint = WhiteMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Seek bar ─────────────────────────────────────────────
            AuraSeekBar(
                position = playerState.currentPosition,
                duration = playerState.duration,
                trackColor = dynamicColors.onContent,
                modifier = Modifier.fillMaxWidth(),
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(Modifier.height(32.dp))

            // ── Transport controls ────────────────────────────────────
            TransportControls(
                isPlaying = playerState.isPlaying,
                repeatMode = playerState.repeatMode,
                shuffleEnabled = playerState.shuffleModeEnabled,
                onContent = dynamicColors.onContent,
                onPrevious = { viewModel.skipPrevious() },
                onPlayPause = { viewModel.playPause() },
                onNext = { viewModel.skipNext() },
                onRepeat = { viewModel.cycleRepeat() },
                onShuffle = { viewModel.toggleShuffle() }
            )

            Spacer(Modifier.height(28.dp))

            // ── Volume row (subtle) ───────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.VolumeDown, null, tint = WhiteDim, modifier = Modifier.size(18.dp))
                Slider(
                    value = 0.7f,
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = White,
                        activeTrackColor = White,
                        inactiveTrackColor = WhiteSubtle
                    )
                )
                Icon(Icons.Rounded.VolumeUp, null, tint = WhiteDim, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onContent: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit
) {
    // Secondary controls
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BounceButton(onClick = onShuffle) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) onContent else onContent.copy(0.4f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Primary controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            BounceButton(onClick = onPrevious) {
                Icon(Icons.Rounded.SkipPrevious, "Previous", tint = onContent,
                    modifier = Modifier.size(46.dp))
            }

            // Play/Pause circle button
            BounceButton(onClick = onPlayPause) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(onContent),
                    contentAlignment = Alignment.Center
                ) {
                    val transition = updateTransition(isPlaying, label = "play_icon")
                    val iconSize by transition.animateDp(
                        transitionSpec = { spring(Spring.DampingRatioMediumBouncy) },
                        label = "icon_size"
                    ) { if (it) 30.dp else 34.dp }

                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = DeepBlack,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            BounceButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, "Next", tint = onContent,
                    modifier = Modifier.size(46.dp))
            }
        }

        BounceButton(onClick = onRepeat) {
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                    Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) onContent
                       else onContent.copy(0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
