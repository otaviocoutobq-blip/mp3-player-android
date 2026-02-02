package com.example.mp3player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlayerConnection(context: Context) {
    private val appContext = context.applicationContext
    private val sessionToken = SessionToken(appContext, ComponentName(appContext, PlayerService::class.java))
    private val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller

    init {
        controllerFuture.addListener(
            {
                _controller.value = controllerFuture.get()
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}
