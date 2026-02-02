package com.example.mp3player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mp3player.Mp3PlayerApp
import com.example.mp3player.data.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecommendationsViewModel(application: Application) : AndroidViewModel(application) {
    private val recommendationEngine = (application as Mp3PlayerApp).recommendationEngine
    private val _recommendations = MutableStateFlow<List<TrackEntity>>(emptyList())
    val recommendations: StateFlow<List<TrackEntity>> = _recommendations.asStateFlow()

    fun loadRecommendations(nowPlayingId: Long?) {
        viewModelScope.launch {
            _recommendations.value = recommendationEngine.getRecommendations(nowPlayingId, limit = 30)
        }
    }
}
