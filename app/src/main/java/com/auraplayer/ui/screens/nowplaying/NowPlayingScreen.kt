package com.auraplayer.ui.screens.nowplaying

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.auraplayer.data.lyrics.LyricLine
import com.auraplayer.data.lyrics.LrcParser
import com.auraplayer.ui.components.AuraSeekBar
import com.auraplayer.ui.components.BounceButton
import com.auraplayer.ui.components.FluidMeshBackground
import com.auraplayer.ui.components.SpringAlbumArt
import com.auraplayer.ui.theme.*

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val dynamicColors by viewModel.dynamicColors.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLyrics by remember { mutableStateOf(false) }

    // Dynamic lyrics holder
    val sampleLyrics = remember(playerState.currentMediaItem?.mediaId) {
        val title = playerState.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Now Playing"
        val artist = playerState.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Aura"
        listOf(
            LyricLine(0L, "♪ $title"),
            LyricLine(12000L, "Performed by $artist"),
            LyricLine(24000L, "Feel the sound in high fidelity"),
            LyricLine(38000L, "Pure offline music experience"),
            LyricLine(52000L, "Echoes moving through the ambient waves"),
            LyricLine(70000L, "Smooth bassline flowing seamlessly"),
            LyricLine(95000L, "Aura Player · Designed for Pure Sound"),
            LyricLine(120000L, "♪ (Instrumental)")
        )
    }

    LaunchedEffect(playerState.currentMediaItem?.mediaId) {
        val uri = playerState.currentMediaItem?.mediaMetadata?.artworkUri
        viewModel.extractColors(context, uri)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // ── Abstract Vector Waveform Illustration Background ─────────
        com.auraplayer.ui.components.IllustrationVisualizer(
            colors = dynamicColors,
            isPlaying = playerState.isPlaying
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Drag handle pill
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(WhiteSubtle)
            )

            Spacer(Modifier.height(12.dp))

            // ── Center Stage: Album Art OR Synced Lyrics ─────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showLyrics,
                    transitionSpec = {
                        fadeIn(spring()) togetherWith fadeOut(spring())
                    },
                    label = "art_or_lyrics"
                ) { lyricsActive ->
                    if (lyricsActive) {
                        LyricsView(
                            lyrics = sampleLyrics,
                            currentPositionMs = playerState.currentPosition,
                            onLineClick = { viewModel.seekTo(it) }
                        )
                    } else {
                        SpringAlbumArt(
                            artworkUri = playerState.currentMediaItem?.mediaMetadata?.artworkUri,
                            isPlaying = playerState.isPlaying,
                            modifier = Modifier
                                .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Song Info + Action Buttons ───────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.currentMediaItem?.mediaMetadata?.title?.toString()
                            ?: "Not Playing",
                        style = MaterialTheme.typography.headlineMedium,
                        color = White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = playerState.currentMediaItem?.mediaMetadata?.artist?.toString()
                            ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = WhiteMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Lyrics Toggle Button (Apple Music style)
                    BounceButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) dynamicColors.onContent else WhiteDim,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Favorite Button
                    BounceButton(onClick = { /* Favorite */ }) {
                        Icon(
                            imageVector = Icons.Rounded.FavoriteBorder,
                            contentDescription = "Love",
                            tint = WhiteMuted,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Seek bar ─────────────────────────────────────────────
            AuraSeekBar(
                position = playerState.currentPosition,
                duration = playerState.duration,
                trackColor = dynamicColors.onContent,
                modifier = Modifier.fillMaxWidth(),
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(10.dp))

            // ── Volume row (subtle) ───────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeDown,
                    null,
                    tint = WhiteDim,
                    modifier = Modifier.size(16.dp)
                )
                Slider(
                    value = 0.7f,
                    onValueChange = {},
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = White,
                        activeTrackColor = White,
                        inactiveTrackColor = WhiteSubtle
                    )
                )
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeUp,
                    null,
                    tint = WhiteDim,
                    modifier = Modifier.size(16.dp)
                )
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
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BounceButton(onClick = onPrevious) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    "Previous",
                    tint = onContent,
                    modifier = Modifier.size(38.dp)
                )
            }

            BounceButton(onClick = onPlayPause) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(onContent),
                    contentAlignment = Alignment.Center
                ) {
                    val transition = updateTransition(isPlaying, label = "play_icon")
                    val iconSize by transition.animateDp(
                        transitionSpec = { spring(Spring.DampingRatioMediumBouncy) },
                        label = "icon_size"
                    ) { if (it) 26.dp else 30.dp }

                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = DeepBlack,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            BounceButton(onClick = onNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    "Next",
                    tint = onContent,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        BounceButton(onClick = onRepeat) {
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                    Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != Player.REPEAT_MODE_OFF) onContent
                       else onContent.copy(0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
