package com.example.mp3player.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mp3player.Mp3PlayerApp
import com.example.mp3player.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as Mp3PlayerApp).repository

    val tracks: StateFlow<List<TrackEntity>> = repository.observeTracks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTrack(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTrackFromUri(getApplication<Application>().contentResolver, uri)
        }
    }
}
