package com.buddy.quickchat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.quickchat.data.repository.AuthRepository
import com.buddy.quickchat.data.repository.UserRepository
import com.buddy.quickchat.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val authState: StateFlow<UiState<Unit>> = _authState.asStateFlow()

    fun isUserLoggedIn() = authRepository.currentUser != null
    fun getCurrentUid() = authRepository.currentUser?.uid

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = authRepository.login(email, pass)
            _authState.value = if (result is UiState.Success) UiState.Success(Unit)
            else UiState.Error((result as UiState.Error).message)
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = authRepository.register(email, pass)
            if (result is UiState.Success) {
                userRepository.createUser(result.data, name, email)
                _authState.value = UiState.Success(Unit)
            } else {
                _authState.value = UiState.Error((result as UiState.Error).message)
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = UiState.Idle
    }
}
