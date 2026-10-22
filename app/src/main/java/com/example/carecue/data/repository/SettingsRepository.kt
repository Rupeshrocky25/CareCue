package com.example.carecue.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone

data class CareCueSettings(
    val userName: String,
    val householdFocus: String,
    val isOnboardingCompleted: Boolean,
    val timeZoneId: String,
    val notificationsEnabled: Boolean,
    val aiProvider: String,
    val apiKey: String,
    val isDemoMode: Boolean
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("carecue_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<CareCueSettings> = _settings.asStateFlow()

    private fun loadSettings(): CareCueSettings {
        return CareCueSettings(
            userName = prefs.getString("user_name", "") ?: "",
            householdFocus = prefs.getString("household_focus", "General Household & Care") ?: "General Household & Care",
            isOnboardingCompleted = prefs.getBoolean("is_onboarding_completed", false),
            timeZoneId = prefs.getString("time_zone_id", TimeZone.getDefault().id) ?: TimeZone.getDefault().id,
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            aiProvider = prefs.getString("ai_provider", "Local Engine (Deterministic/Offline)") ?: "Local Engine (Deterministic/Offline)",
            apiKey = prefs.getString("api_key", "") ?: "",
            isDemoMode = false // Disabled sample data by default!
        )
    }

    fun completeOnboarding(userName: String, focus: String, timeZoneId: String) {
        prefs.edit()
            .putString("user_name", userName.trim())
            .putString("household_focus", focus.trim())
            .putString("time_zone_id", timeZoneId.trim())
            .putBoolean("is_onboarding_completed", true)
            .apply()
        _settings.value = _settings.value.copy(
            userName = userName.trim(),
            householdFocus = focus.trim(),
            timeZoneId = timeZoneId.trim(),
            isOnboardingCompleted = true
        )
    }

    fun updateUserName(name: String) {
        prefs.edit().putString("user_name", name.trim()).apply()
        _settings.value = _settings.value.copy(userName = name.trim())
    }

    fun updateTimeZone(timeZoneId: String) {
        prefs.edit().putString("time_zone_id", timeZoneId).apply()
        _settings.value = _settings.value.copy(timeZoneId = timeZoneId)
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun updateAiProvider(provider: String) {
        prefs.edit().putString("ai_provider", provider).apply()
        _settings.value = _settings.value.copy(aiProvider = provider)
    }

    fun updateApiKey(key: String) {
        prefs.edit().putString("api_key", key.trim()).apply()
        _settings.value = _settings.value.copy(apiKey = key.trim())
    }

    fun resetAllSettings() {
        prefs.edit().clear().apply()
        _settings.value = loadSettings()
    }
}
