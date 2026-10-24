package com.buddy.quickchat

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.buddy.quickchat.util.PresenceManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuickChatApp : Application() {

    @Inject
    lateinit var presenceManager: PresenceManager

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(presenceManager)
    }
}
