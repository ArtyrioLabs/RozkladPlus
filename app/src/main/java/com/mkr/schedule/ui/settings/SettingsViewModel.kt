package com.mkr.schedule.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.util.LogCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled
    
    private val _currentLanguage = MutableStateFlow("uk")
    val currentLanguage: StateFlow<String> = _currentLanguage
    
    private val _isChillModeEnabled = MutableStateFlow(false)
    val isChillModeEnabled: StateFlow<Boolean> = _isChillModeEnabled

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _notificationsEnabled.value = preferencesManager.getNotificationsEnabled()
            LogCollector.log("SettingsViewModel", "Notifications enabled: ${_notificationsEnabled.value}")
            
            preferencesManager.language.collect { language ->
                _currentLanguage.value = language
                applyAppLanguage(language)
                LogCollector.log("SettingsViewModel", "Language: $language")
            }
        }
        
        viewModelScope.launch {
            preferencesManager.isChillModeEnabled.collect { enabled ->
                _isChillModeEnabled.value = enabled
            }
        }
        
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)
            _notificationsEnabled.value = enabled
            LogCollector.log("SettingsViewModel", "Notifications toggled: $enabled")
        }
    }
    
    fun toggleChillMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setChillModeEnabled(enabled)
            _isChillModeEnabled.value = enabled
            LogCollector.log("SettingsViewModel", "Chill mode toggled: $enabled")
        }
    }
    
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            preferencesManager.saveLanguage(languageCode)
            _currentLanguage.value = languageCode
            applyAppLanguage(languageCode)
            LogCollector.log("SettingsViewModel", "Language changed to: $languageCode")
        }
    }

    private fun applyAppLanguage(languageCode: String) {
        val localeTag = when (languageCode.lowercase()) {
            "en" -> "en"
            else -> "uk"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
    }

    fun clearScheduleCache() {
        viewModelScope.launch {
            preferencesManager.getScheduleCachePrefs().edit().clear().apply()
            preferencesManager.getApiLinksPrefs().edit().clear().apply()
            LogCollector.log("SettingsViewModel", "Schedule and API links cache cleared")
        }
    }

    fun resetAppSettings() {
        viewModelScope.launch {
            val notificationsState = _notificationsEnabled.value
            preferencesManager.clear()
            _notificationsEnabled.value = false
            _currentLanguage.value = "uk"
            LogCollector.log("SettingsViewModel", "App settings reset, previous notifications=$notificationsState")
        }
    }
}
