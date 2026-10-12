package com.auraplayer.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.auraplayer.ads.BannerAdView
import com.auraplayer.domain.model.Song
import com.auraplayer.service.AuraMediaControllerManager
import com.auraplayer.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import com.auraplayer.core.billing.BillingManager
import com.auraplayer.ads.AdManager
import com.auraplayer.core.audio.EqualizerManager
import com.auraplayer.ui.screens.equalizer.EqualizerSheet
import com.auraplayer.ui.screens.pro.ProPaywallSheet
import androidx.compose.foundation.border
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import com.auraplayer.domain.repository.SongRepository

@Composable
fun LibraryScreen(
    controllerManager: AuraMediaControllerManager,
    songRepository: SongRepository,
    billingManager: BillingManager,
    adManager: AdManager? = null,
    equalizerManager: EqualizerManager? = null,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.provideFactory(songRepository)
    )
) {
    val songs   by viewModel.songs.collectAsStateWithLifecycle()
    val query   by viewModel.query.collectAsStateWithLifecycle()
    val filter  by viewModel.filter.collectAsStateWithLifecycle()
    val isPro   by billingManager.isProUser.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var showProPaywall by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack)
            .systemBarsPadding()
    ) {
        // ── Header ───────────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = showSearch,
                transitionSpec = {
                    fadeIn(spring()) togetherWith fadeOut(spring())
                },
                label = "header_anim"
            ) { searching ->
                if (searching) {
                    TextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search songs, artists…", color = WhiteDim) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceVariant,
                            unfocusedContainerColor = SurfaceVariant,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            cursorColor = Accent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f).height(52.dp)
                    )
                } else {
                    Text(
                        "My Library",
                        style = MaterialTheme.typography.displayMedium,
                        color = White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (!showSearch) {
                // Custom Equalizer Button
                BounceIconButton(
                    icon = Icons.Rounded.GraphicEq,
                    onClick = { showEqualizerSheet = true }
                )

                Spacer(Modifier.width(12.dp))

                // Import Music Button
                BounceIconButton(
                    icon = Icons.Rounded.CloudUpload,
                    onClick = { showImportSheet = true }
                )

                Spacer(Modifier.width(12.dp))
            }

            BounceIconButton(
                icon = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                onClick = {
                    showSearch = !showSearch
                    if (!showSearch) viewModel.onQueryChange("")
                }
            )
        }

        // ── Filter tabs ───────────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(LibraryFilter.entries) { tab ->
                FilterChip(
                    selected = filter == tab,
                    onClick = { viewModel.onFilterChange(tab) },
                    label = {
                        Text(
                            tab.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = White,
                        selectedLabelColor = DeepBlack,
                        containerColor = SurfaceVariant,
                        labelColor = WhiteMuted
                    ),
                    border = null,
                    shape = CircleShape
                )
            }
        }

        // ── Spotify-Style Aura Pro Top Promo Banner ───────────────────
        if (!isPro) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showProPaywall = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF2E2405), Color(0xFF1B1B22))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = DeepBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upgrade to Aura Pro",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Zero ads · 5-Band Studio EQ · Lossless sound",
                            color = WhiteMuted,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFD700))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Get Pro",
                            color = DeepBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        var selectedAlbum by remember { mutableStateOf<AlbumItem?>(null) }

        // ── Main Content Switcher ─────────────────────────────────────
        if (filter == LibraryFilter.ALBUMS) {
            AlbumsGrid(
                songs = songs,
                onAlbumClick = { selectedAlbum = it },
                modifier = Modifier.weight(1f)
            )
        } else {
            // Song list
            LazyColumn(
                contentPadding = PaddingValues(bottom = 90.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        onClick = { playSong(controllerManager, songs, song) }
                    )
                }

                if (songs.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), Alignment.Center) {
                            Text("No songs found", color = WhiteDim)
                        }
                    }
                }
            }
        }

        selectedAlbum?.let { album ->
            AlbumDetailSheet(
                album = album,
                controllerManager = controllerManager,
                onDismiss = { selectedAlbum = null }
            )
        }

        if (showProPaywall) {
            ProPaywallSheet(
                billingManager = billingManager,
                onDismiss = { showProPaywall = false }
            )
        }

        if (showImportSheet) {
            MusicImportSheet(
                songRepository = songRepository,
                onDismiss = { showImportSheet = false }
            )
        }

        if (showEqualizerSheet && equalizerManager != null && adManager != null) {
            EqualizerSheet(
                equalizerManager = equalizerManager,
                adManager = adManager,
                onDismiss = { showEqualizerSheet = false }
            )
        }

        // ── AdMob Banner (Framed clean Spotify-style container) ───────
        if (!isPro) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(0.5.dp, Outline, RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BannerAdView()
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Album art
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
        )

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            formatMs(song.duration),
            style = MaterialTheme.typography.labelMedium,
            color = WhiteDim
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 86.dp),
        color = Outline.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

@Composable
private fun BounceIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    com.auraplayer.ui.components.BounceButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    ) {
        Icon(icon, contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
    }
}

private fun playSong(
    manager: AuraMediaControllerManager,
    queue: List<Song>,
    song: Song
) {
    val ctrl = manager.controller.value ?: return
    val items = queue.map { s ->
        MediaItem.Builder()
            .setMediaId(s.id.toString())
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(s.uri)
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(s.title)
                    .setArtist(s.artist)
                    .setAlbumTitle(s.album)
                    .setArtworkUri(s.artworkUri)
                    .build()
            )
            .build()
    }
    val index = queue.indexOfFirst { it.id == song.id }
    ctrl.setMediaItems(items, index, 0L)
    ctrl.prepare()
    ctrl.play()
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
