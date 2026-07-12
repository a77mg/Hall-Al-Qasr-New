package com.example

import android.app.Application
import com.example.repository.PresenceRepository
import com.example.repository.Repositories
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    // Injection via Hilt (if Hilt plugin is active)
    @Inject
    lateinit var presenceRepository: PresenceRepository

    override fun onCreate() {
        super.onCreate()
        
        // Ensure immediate initialization of Presence system so it runs in background
        // Note: Because Hilt initialization can sometimes be lazy depending on usage,
        // calling initialize() here explicitly ensures it starts listening instantly.
        
        try {
            if (::presenceRepository.isInitialized) {
                presenceRepository.initialize()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Keep the manual DI working for the preview environment fallback
        Repositories.presence.initialize()
    }
}
