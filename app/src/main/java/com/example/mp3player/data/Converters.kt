package com.example.mp3player.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPlaybackEventType(type: PlaybackEventType): String = type.name

    @TypeConverter
    fun toPlaybackEventType(value: String): PlaybackEventType = PlaybackEventType.valueOf(value)
}
