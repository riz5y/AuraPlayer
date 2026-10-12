package com.auraplayer.ui.screens.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.lyrics.LyricLine
import com.auraplayer.ui.theme.White
import com.auraplayer.ui.theme.WhiteDim

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val activeIndex = remember(lyrics, currentPositionMs) {
        val idx = lyrics.indexOfLast { it.timeMs <= currentPositionMs }
        if (idx >= 0) idx else 0
    }

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = (activeIndex - 1).coerceAtLeast(0)
            )
        }
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "No lyrics available for this track",
                style = MaterialTheme.typography.bodyLarge,
                color = WhiteDim
            )
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp, horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isActive = index == activeIndex
                val alpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.35f,
                    animationSpec = spring(),
                    label = "lyric_alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 1f,
                    animationSpec = spring(),
                    label = "lyric_scale"
                )

                Text(
                    text = line.text,
                    fontSize = if (isActive) 28.sp else 24.sp,
                    lineHeight = 36.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .alpha(alpha)
                        .clickable { onLineClick(line.timeMs) }
                )
            }
        }
    }
}
