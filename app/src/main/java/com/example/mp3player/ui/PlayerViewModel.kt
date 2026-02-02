package com.example.mp3player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.mp3player.Mp3PlayerApp
import com.example.mp3player.data.PlaybackEventType
import com.example.mp3player.data.TrackEntity
import com.example.mp3player.playback.PlayerConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as Mp3PlayerApp).repository
    private val connection = PlayerConnection(application)
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private var positionJob: Job? = null

    init {
        viewModelScope.launch {
            connection.controller.collectLatest { controller ->
                if (controller != null) {
                    controller.addListener(playerListener)
                    updateState(controller)
                    startPositionUpdates(controller)
                }
            }
        }
    }

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int) {
        val controller = connection.controller.value ?: return
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setGenre(track.genre)
                        .build()
                )
                .build()
        }
        controller.setMediaItems(items, startIndex, 0L)
        controller.prepare()
        controller.play()
        logEvent(PlaybackEventType.PLAY)
    }

    fun togglePlayPause() {
        val controller = connection.controller.value ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        connection.controller.value?.seekTo(positionMs)
    }

    fun skipNext() {
        val controller = connection.controller.value ?: return
        if (controller.currentPosition < 10_000L) {
            logEvent(PlaybackEventType.SKIP)
        }
        controller.seekToNext()
    }

    fun skipPrevious() {
        connection.controller.value?.seekToPrevious()
    }

    fun likeCurrent() {
        logEvent(PlaybackEventType.LIKE)
    }

    private fun logEvent(type: PlaybackEventType) {
        val controller = connection.controller.value ?: return
        val currentId = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        viewModelScope.launch {
            repository.logEvent(currentId, type, controller.currentPosition)
        }
    }

    private fun updateState(controller: Player) {
        val metadata = controller.mediaMetadata
        _uiState.value = _uiState.value.copy(
            isReady = controller.playbackState != Player.STATE_IDLE,
            isPlaying = controller.isPlaying,
            title = metadata.title?.toString() ?: "",
            artist = metadata.artist?.toString() ?: "",
            duration = controller.duration.coerceAtLeast(0L),
            position = controller.currentPosition,
            currentMediaId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        )
    }

    private fun startPositionUpdates(controller: Player) {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                updateState(controller)
                delay(500L)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            connection.controller.value?.let { updateState(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val controller = connection.controller.value ?: return
            updateState(controller)
            if (playbackState == Player.STATE_ENDED) {
                logEvent(PlaybackEventType.COMPLETE)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            connection.controller.value?.let { updateState(it) }
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                logEvent(PlaybackEventType.PLAY)
            }
        }
    }

    override fun onCleared() {
        connection.release()
        super.onCleared()
    }
}


data class PlayerUiState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val position: Long = 0L,
    val duration: Long = 0L,
    val currentMediaId: Long? = null
)
