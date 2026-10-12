package com.auraplayer.ui.screens.nowplaying

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auraplayer.ui.theme.DeepBlack

/**
 * Full-screen Now Playing modal sheet.
 * Smoothly slides up from the mini-player and supports drag-down to dismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingModal(
    viewModel: NowPlayingViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = DeepBlack,
        modifier = Modifier.fillMaxSize()
    ) {
        NowPlayingScreen(
            viewModel = viewModel,
            onCollapse = onDismiss,
            modifier = Modifier.fillMaxSize()
        )
    }
}
