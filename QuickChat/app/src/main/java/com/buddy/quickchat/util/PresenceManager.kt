package com.buddy.quickchat.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.buddy.quickchat.data.repository.AuthRepository
import com.buddy.quickchat.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : DefaultLifecycleObserver {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        updatePresence(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        updatePresence(false)
        super.onStop(owner)
    }

    private fun updatePresence(isOnline: Boolean) {
        val uid = authRepository.currentUser?.uid
        if (uid != null) {
            coroutineScope.launch {
                userRepository.updatePresence(uid, isOnline)
            }
        }
    }
}
