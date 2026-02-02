package com.example.mp3player.recommendations

import com.example.mp3player.data.MusicRepository
import com.example.mp3player.data.TrackEntity
import kotlin.math.max
import kotlin.random.Random

class RecommendationEngine(
    private val repository: MusicRepository
) {
    suspend fun getRecommendations(nowPlayingId: Long?, limit: Int): List<TrackEntity> {
        val tracks = repository.getTracks()
        if (tracks.isEmpty()) return emptyList()

        val current = nowPlayingId?.let { id -> tracks.firstOrNull { it.id == id } }
        val discoveryCount = max(1, (limit * 0.3).toInt())
        val similarityCount = limit - discoveryCount

        val similar = tracks
            .filter { it.id != nowPlayingId }
            .sortedByDescending { scoreSimilarity(it, current) }
            .take(similarityCount)

        val discovery = tracks
            .filter { it.id != nowPlayingId }
            .sortedWith(
                compareBy<TrackEntity> { it.playCount }
                    .thenBy { it.skipCount }
                    .thenByDescending { it.addedAt }
            )
            .shuffled(Random(System.currentTimeMillis()))
            .take(discoveryCount)

        return (similar + discovery).distinctBy { it.id }.take(limit)
    }

    private fun scoreSimilarity(track: TrackEntity, current: TrackEntity?): Double {
        if (current == null) return 0.0
        val artistScore = if (track.artist == current.artist) 2.5 else 0.0
        val albumScore = if (track.album == current.album) 2.0 else 0.0
        val genreScore = if (track.genre == current.genre) 1.5 else 0.0
        val recencyScore = (track.addedAt / 1_000_000_000.0)
        val skipPenalty = track.skipCount * 0.4
        return artistScore + albumScore + genreScore + recencyScore - skipPenalty
    }
}
