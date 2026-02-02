package com.example.mp3player.data

import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(
    private val database: AppDatabase
) {
    private val trackDao = database.trackDao()
    private val eventDao = database.playbackEventDao()

    fun observeTracks() = trackDao.observeTracks()

    suspend fun getTracks(): List<TrackEntity> = trackDao.getTracks()

    suspend fun getTrack(id: Long): TrackEntity? = trackDao.getTrack(id)

    suspend fun addTrackFromUri(contentResolver: ContentResolver, uri: Uri): Long {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: "Unknown"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "Unknown"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()

            trackDao.insert(
                TrackEntity(
                    uri = uri.toString(),
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    durationMs = duration,
                    addedAt = System.currentTimeMillis(),
                    playCount = 0,
                    skipCount = 0,
                    likeCount = 0
                )
            )
        }
    }

    suspend fun logEvent(trackId: Long, type: PlaybackEventType, positionMs: Long) {
        eventDao.insert(
            PlaybackEventEntity(
                trackId = trackId,
                type = type,
                positionMs = positionMs,
                createdAt = System.currentTimeMillis()
            )
        )
        val track = trackDao.getTrack(trackId) ?: return
        val updated = when (type) {
            PlaybackEventType.PLAY -> track.copy(playCount = track.playCount + 1)
            PlaybackEventType.SKIP -> track.copy(skipCount = track.skipCount + 1)
            PlaybackEventType.LIKE -> track.copy(likeCount = track.likeCount + 1)
            PlaybackEventType.COMPLETE -> track
        }
        trackDao.update(updated)
    }
}
