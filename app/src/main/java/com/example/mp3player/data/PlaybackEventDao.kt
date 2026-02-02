package com.example.mp3player.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaybackEventDao {
    @Insert
    suspend fun insert(event: PlaybackEventEntity)

    @Query("SELECT * FROM playback_events WHERE trackId = :trackId")
    suspend fun eventsForTrack(trackId: Long): List<PlaybackEventEntity>
}
