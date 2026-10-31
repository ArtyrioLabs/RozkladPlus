package com.mkr.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkr.schedule.data.model.User
import com.mkr.schedule.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val accessToken: String? = null,
    val user: User? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    private fun checkAuthStatus() {
        viewModelScope.launch {
            val token = authRepository.getAccessToken().first()
            if (!token.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    accessToken = token
                )
                // Try to get user info
                authRepository.getUserInfo(token).onSuccess { user ->
                    _uiState.value = _uiState.value.copy(user = user)
                }
            }
        }
    }
    
    fun login(username: String, password: String, appKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.login(username, password, appKey)
                .onSuccess {
                    val token = "Bearer ${it.accessToken}"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        accessToken = token
                    )
                    // Get user info after login
                    authRepository.getUserInfo(token).onSuccess { user ->
                        _uiState.value = _uiState.value.copy(user = user)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        error = exception.message ?: "Помилка входу"
                    )
                }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

