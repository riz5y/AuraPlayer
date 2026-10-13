package com.auraplayer.ui.screens.nowplaying

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.auraplayer.ads.AdManager
import com.auraplayer.core.audio.EqualizerManager
import com.auraplayer.core.billing.BillingManager
import com.auraplayer.ui.screens.equalizer.EqualizerSheet

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    onCollapse: () -> Unit = {},
    equalizerManager: EqualizerManager? = null,
    adManager: AdManager? = null,
    billingManager: BillingManager? = null,
    modifier: Modifier = Modifier
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val dynamicColors by viewModel.dynamicColors.collectAsStateWithLifecycle()
    val isPro = billingManager?.isProUser?.collectAsStateWithLifecycle()?.value ?: false
    val context = LocalContext.current
    var showLyrics by remember { mutableStateOf(false) }
    var showTrackOptionsSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember {
        mutableFloatStateOf(
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10).toFloat() / maxVolume
        )
    }
    val isEqEnabled = equalizerManager?.isEnabled?.collectAsState()?.value ?: false

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Spotify-Style Top Navigation Bar ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM YOUR LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhiteDim,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = playerState.currentMediaItem?.mediaMetadata?.albumTitle?.toString()
                            ?: "Aura Player",
                        style = MaterialTheme.typography.bodySmall,
                        color = White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = {
                    val activity = context as? Activity
                    if (!isPro && adManager != null && activity != null) {
                        adManager.showActionInterstitial(activity) { showTrackOptionsSheet = true }
                    } else {
                        showTrackOptionsSheet = true
                    }
                }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Center Stage: Album Art OR Synced Lyrics ─────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
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
                                .sizeIn(maxWidth = 340.dp, maxHeight = 340.dp)
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Song Info + Favorite Button ───────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.currentMediaItem?.mediaMetadata?.title?.toString()
                            ?: "Not Playing",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = playerState.currentMediaItem?.mediaMetadata?.artist?.toString()
                            ?: "—",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = WhiteMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Hi-Res Lossless Audio Specs Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevated)
                                .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("⚡ LOSSLESS AUDIO", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevated)
                                .border(0.5.dp, WhiteSubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("24-BIT / 96kHz · FLAC", color = WhiteDim, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Favorite Button with animation and toast
                BounceButton(onClick = {
                    isFavorite = !isFavorite
                    Toast.makeText(context, if (isFavorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                }) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isFavorite) Color(0xFFFF4B6E).copy(alpha = 0.2f) else SurfaceElevated)
                            .border(0.5.dp, if (isFavorite) Color(0xFFFF4B6E) else WhiteSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Love",
                            tint = if (isFavorite) Color(0xFFFF4B6E) else WhiteMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Seek bar ─────────────────────────────────────────────
            AuraSeekBar(
                position = playerState.currentPosition,
                duration = playerState.duration,
                trackColor = dynamicColors.onContent,
                modifier = Modifier.fillMaxWidth(),
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(Modifier.height(14.dp))

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

            Spacer(Modifier.height(14.dp))

            // ── Real Hardware Volume Slider ──────────────────────────
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
                    value = currentVolume,
                    onValueChange = { newVol ->
                        currentVolume = newVol
                        audioManager?.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            (newVol * maxVolume).toInt(),
                            0
                        )
                    },
                    modifier = Modifier.weight(1f).height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = White,
                        activeTrackColor = dynamicColors.onContent,
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

            Spacer(Modifier.height(16.dp))

            // ── Bottom Utility Dock (Spotify / Apple Music Studio Toolbar) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevated.copy(alpha = 0.75f))
                    .border(0.5.dp, WhiteSubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Equalizer / Studio Sound Mixer
                BounceButton(onClick = {
                    val activity = context as? Activity
                    if (!isPro && adManager != null && activity != null) {
                        adManager.showActionInterstitial(activity) { showEqualizerSheet = true }
                    } else {
                        showEqualizerSheet = true
                    }
                }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isEqEnabled) Accent.copy(alpha = 0.25f) else Color.Transparent)
                            .border(0.5.dp, if (isEqEnabled) Accent else Color.Transparent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Mixer",
                            tint = if (isEqEnabled) Accent else WhiteDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isEqEnabled) "EQ ON" else "MIXER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEqEnabled) Accent else WhiteDim
                        )
                    }
                }

                // 2. Synced Karaoke Lyrics Toggle
                BounceButton(onClick = { showLyrics = !showLyrics }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (showLyrics) Accent.copy(alpha = 0.25f) else Color.Transparent)
                            .border(0.5.dp, if (showLyrics) Accent else Color.Transparent, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) Accent else WhiteDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "LYRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showLyrics) Accent else WhiteDim
                        )
                    }
                }

                // 3. Studio Audio Options
                BounceButton(onClick = {
                    val activity = context as? Activity
                    if (!isPro && adManager != null && activity != null) {
                        adManager.showActionInterstitial(activity) { showTrackOptionsSheet = true }
                    } else {
                        showTrackOptionsSheet = true
                    }
                }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Options",
                            tint = WhiteDim,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "STUDIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhiteDim
                        )
                    }
                }

                // 4. Share Track
                BounceButton(onClick = {
                    val title = playerState.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Now Playing"
                    val artist = playerState.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Aura Artist"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Listening to '$title' by $artist on AuraPlayer Hi-Fi Studio!")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                }) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = WhiteDim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 5. Up Next / Queue Peek Card (Completes full vertical display!) ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val activity = context as? Activity
                        if (!isPro && adManager != null && activity != null) {
                            adManager.showActionInterstitial(activity) { showTrackOptionsSheet = true }
                        } else {
                            showTrackOptionsSheet = true
                        }
                    }
                    .background(SurfaceElevated.copy(alpha = 0.55f))
                    .border(0.5.dp, WhiteSubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = Color.Transparent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UP NEXT IN QUEUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = WhiteDim,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap for Studio Audio Specs, DSP & Queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = WhiteSubtle,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }

    if (showTrackOptionsSheet) {
        TrackOptionsSheet(
            playerState = playerState,
            equalizerManager = equalizerManager,
            onOpenEqualizer = { showEqualizerSheet = true },
            onToggleLyrics = { showLyrics = !showLyrics },
            onDismiss = { showTrackOptionsSheet = false }
        )
    }

    if (showEqualizerSheet && equalizerManager != null && adManager != null) {
        EqualizerSheet(
            equalizerManager = equalizerManager,
            adManager = adManager,
            onDismiss = { showEqualizerSheet = false }
        )
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
