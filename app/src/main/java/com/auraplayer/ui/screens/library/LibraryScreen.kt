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
import com.auraplayer.domain.repository.SongRepository

@Composable
fun LibraryScreen(
    controllerManager: AuraMediaControllerManager,
    songRepository: SongRepository,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.provideFactory(songRepository)
    )
) {
    val songs   by viewModel.songs.collectAsStateWithLifecycle()
    val query   by viewModel.query.collectAsStateWithLifecycle()
    val filter  by viewModel.filter.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
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
                contentPadding = PaddingValues(bottom = 160.dp),
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

        // ── AdMob Banner (above mini-player) ─────────────────────────
        BannerAdView()
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
    com.auraplayer.ui.components.BounceButton(onClick = onClick) {
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
