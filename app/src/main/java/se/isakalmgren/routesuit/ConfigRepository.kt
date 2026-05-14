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
            longitude = loadDouble("longitude", defaultConfig.longitude),
            latitude = loadDouble("latitude", defaultConfig.latitude),
            morningCommuteStartHour = prefs.getInt("morning_commute_start", 7),
            morningCommuteEndHour = prefs.getInt("morning_commute_end", 9),
            eveningCommuteStartHour = prefs.getInt("evening_commute_start", 16),
            eveningCommuteEndHour = prefs.getInt("evening_commute_end", 19),
            precipitationProbabilityThreshold = loadDouble("precip_prob_threshold", defaultConfig.precipitationProbabilityThreshold),
            precipitationAmountThreshold = loadDouble("precip_amount_threshold", defaultConfig.precipitationAmountThreshold),
            notificationDays = loadNotificationDays(),
            notificationHour = prefs.getInt("notification_hour", Constants.NOTIFICATION_DEFAULT_HOUR),
            notificationMinute = prefs.getInt("notification_minute", Constants.NOTIFICATION_DEFAULT_MINUTE)
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
    
    // Reads a Double stored as Long bits (new format), with Float fallback for migration.
    // On the first save after migration the old Float key is removed.
    private fun loadDouble(key: String, default: Double): Double {
        val bitsKey = "${key}_bits"
        return when {
            prefs.contains(bitsKey) -> Double.fromBits(prefs.getLong(bitsKey, default.toBits()))
            prefs.contains(key) -> prefs.getFloat(key, default.toFloat()).toDouble()
            else -> default
        }
    }

    fun saveConfig(config: AppConfig) {
        prefs.edit().apply {
            putLong("longitude_bits", config.longitude.toBits())
            putLong("latitude_bits", config.latitude.toBits())
            remove("longitude")
            remove("latitude")
            putInt("morning_commute_start", config.morningCommuteStartHour)
            putInt("morning_commute_end", config.morningCommuteEndHour)
            putInt("evening_commute_start", config.eveningCommuteStartHour)
            putInt("evening_commute_end", config.eveningCommuteEndHour)
            putLong("precip_prob_threshold_bits", config.precipitationProbabilityThreshold.toBits())
            putLong("precip_amount_threshold_bits", config.precipitationAmountThreshold.toBits())
            remove("precip_prob_threshold")
            remove("precip_amount_threshold")
            putString("notification_days", config.notificationDays.joinToString(","))
            putInt("notification_hour", config.notificationHour)
            putInt("notification_minute", config.notificationMinute)
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

