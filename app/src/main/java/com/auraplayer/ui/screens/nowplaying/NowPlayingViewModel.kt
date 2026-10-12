package com.auraplayer.ui.screens.nowplaying

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.auraplayer.service.AuraMediaControllerManager
import com.auraplayer.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.ui.graphics.Color

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val controllerManager: AuraMediaControllerManager
) : ViewModel() {

    val controller = controllerManager.controller

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _dynamicColors = MutableStateFlow(DynamicColors())
    val dynamicColors: StateFlow<DynamicColors> = _dynamicColors.asStateFlow()

    init {
        viewModelScope.launch {
            controller.collectLatest { ctrl ->
                ctrl ?: return@collectLatest
                while (true) {
                    _playerState.value = PlayerState(
                        isPlaying = ctrl.isPlaying,
                        currentPosition = ctrl.currentPosition,
                        duration = ctrl.duration.coerceAtLeast(0L),
                        currentMediaItem = ctrl.currentMediaItem,
                        repeatMode = ctrl.repeatMode,
                        shuffleModeEnabled = ctrl.shuffleModeEnabled
                    )
                    delay(500)
                }
            }
        }
    }

    fun extractColors(context: Context, artworkUri: Uri?) {
        viewModelScope.launch {
            val colors = runCatching {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(200)
                    .allowHardware(false)
                    .build()
                val bitmap = (loader.execute(req) as? SuccessResult)
                    ?.image?.toBitmap() ?: return@runCatching DynamicColors()

                val palette = Palette.from(bitmap).generate()
                DynamicColors(
                    dominant  = Color(palette.getDominantColor(0xFF1A1A2E.toInt())),
                    vibrant   = Color(palette.getVibrantColor(
                        palette.getDarkVibrantColor(0xFF7B2FBE.toInt()))),
                    onContent = if (palette.getDominantColor(0).luminance() < 0.4f)
                        Color.White else Color.Black
                )
            }.getOrDefault(DynamicColors())
            _dynamicColors.value = colors
        }
    }

    fun playPause() = controller.value?.let {
        if (it.isPlaying) it.pause() else it.play()
    }
    fun seekTo(pos: Long)  = controller.value?.seekTo(pos)
    fun skipNext()         = controller.value?.seekToNextMediaItem()
    fun skipPrevious()     = controller.value?.seekToPreviousMediaItem()
    fun toggleShuffle()    = controller.value?.let {
        it.shuffleModeEnabled = !it.shuffleModeEnabled
    }
    fun cycleRepeat() = controller.value?.let {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else                   -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerManager.disconnect()
    }
}

private fun Int.luminance(): Float {
    val r = (this shr 16 and 0xFF) / 255f
    val g = (this shr 8  and 0xFF) / 255f
    val b = (this        and 0xFF) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentMediaItem: MediaItem? = null,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false
)

data class DynamicColors(
    val dominant:  Color = Color(0xFF1A1A2E),
    val vibrant:   Color = Color(0xFF7B2FBE),
    val onContent: Color = Color.White
)
