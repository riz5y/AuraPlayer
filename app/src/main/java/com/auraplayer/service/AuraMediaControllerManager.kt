package com.auraplayer.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class AuraMediaControllerManager(
    private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    fun connect() {
        val token = SessionToken(
            context,
            ComponentName(context, AuraMediaSessionService::class.java)
        )
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener({
            _controller.value = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        MediaController.releaseFuture(controllerFuture ?: return)
        _controller.value = null
    }
}
