package com.buddy.quickchat.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.quickchat.data.repository.AuthRepository
import com.buddy.quickchat.data.repository.StorageRepository
import com.buddy.quickchat.data.repository.UserRepository
import com.buddy.quickchat.domain.model.User
import com.buddy.quickchat.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userState: StateFlow<UiState<User>> = _userState.asStateFlow()

    private val _uploadState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uploadState: StateFlow<UiState<String>> = _uploadState.asStateFlow()

    val currentUid = authRepository.currentUser?.uid ?: ""

    init {
        loadProfile()
    }

    private fun loadProfile() {
        if (currentUid.isEmpty()) return
        viewModelScope.launch {
            _userState.value = UiState.Loading
            _userState.value = userRepository.getUser(currentUid)
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        if (currentUid.isEmpty()) return
        viewModelScope.launch {
            _uploadState.value = UiState.Loading
            val uploadResult = storageRepository.uploadProfileImage(currentUid, uri)
            _uploadState.value = uploadResult
            if (uploadResult is UiState.Success) {
                // Update Firestore user document with new photoUrl
                val userRes = userRepository.getUser(currentUid)
                if (userRes is UiState.Success) {
                    val updatedUser = userRes.data.copy(photoUrl = uploadResult.data)
                    userRepository.createUser(currentUid, updatedUser.name, updatedUser.email) // Simplified update
                    loadProfile()
                }
            }
        }
    }
}
