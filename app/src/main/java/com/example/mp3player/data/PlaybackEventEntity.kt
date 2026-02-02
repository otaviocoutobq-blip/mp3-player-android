package com.example.mp3player.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_events")
data class PlaybackEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val type: PlaybackEventType,
    val positionMs: Long,
    val createdAt: Long
)

enum class PlaybackEventType {
    PLAY,
    SKIP,
    LIKE,
    COMPLETE
}
