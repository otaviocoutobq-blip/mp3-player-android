package com.example.mp3player

import android.app.Application
import com.example.mp3player.data.AppDatabase
import com.example.mp3player.data.MusicRepository
import com.example.mp3player.recommendations.RecommendationEngine

class Mp3PlayerApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: MusicRepository
        private set
    lateinit var recommendationEngine: RecommendationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        repository = MusicRepository(database)
        recommendationEngine = RecommendationEngine(repository)
    }
}
