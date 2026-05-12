package se.isakalmgren.routesuit

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId
import java.util.Calendar

class ConfigRepository(
    private val context: Context,
    private val languageRepository: LanguageRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_config",
        Context.MODE_PRIVATE
    )
    
    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()
    
    fun getConfig(): AppConfig = _config.value
    
    fun loadConfig(): AppConfig {
        val defaultConfig = AppConfig()
        
        return AppConfig(
            longitude = prefs.getFloat("longitude", defaultConfig.longitude.toFloat()).toDouble(),
            latitude = prefs.getFloat("latitude", defaultConfig.latitude.toFloat()).toDouble(),
            morningCommuteStartHour = prefs.getInt("morning_commute_start", 7),
            morningCommuteEndHour = prefs.getInt("morning_commute_end", 9),
            eveningCommuteStartHour = prefs.getInt("evening_commute_start", 16),
            eveningCommuteEndHour = prefs.getInt("evening_commute_end", 19),
            precipitationProbabilityThreshold = prefs.getFloat("precip_prob_threshold", 50.0f).toDouble(),
            precipitationAmountThreshold = prefs.getFloat("precip_amount_threshold", 0.5f).toDouble(),
            notificationDays = loadNotificationDays()
        )
    }
    
    private fun loadNotificationDays(): Set<Int> {
        val defaultDays = AppConfig().notificationDays
        val daysString = prefs.getString("notification_days", null)
        return if (daysString != null && daysString.isNotEmpty()) {
            daysString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        } else {
            defaultDays
        }
    }
    
    fun saveConfig(config: AppConfig) {
        prefs.edit().apply {
            putFloat("longitude", config.longitude.toFloat())
            putFloat("latitude", config.latitude.toFloat())
            putInt("morning_commute_start", config.morningCommuteStartHour)
            putInt("morning_commute_end", config.morningCommuteEndHour)
            putInt("evening_commute_start", config.eveningCommuteStartHour)
            putInt("evening_commute_end", config.eveningCommuteEndHour)
            putFloat("precip_prob_threshold", config.precipitationProbabilityThreshold.toFloat())
            putFloat("precip_amount_threshold", config.precipitationAmountThreshold.toFloat())
            // Save notification days as comma-separated string
            putString("notification_days", config.notificationDays.joinToString(","))
            apply()
        }
        _config.value = config
    }
    
    fun resetToDefaults() {
        val defaultConfig = AppConfig()
        saveConfig(defaultConfig)
    }
    
    fun reloadConfig() {
        _config.value = loadConfig()
    }
}

