package se.isakalmgren.leaveprepared

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId

class ConfigRepository(private val context: Context) {
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
            morningCommuteStartHour = prefs.getInt("morning_commute_start", 7),
            morningCommuteEndHour = prefs.getInt("morning_commute_end", 9),
            eveningCommuteStartHour = prefs.getInt("evening_commute_start", 16),
            eveningCommuteEndHour = prefs.getInt("evening_commute_end", 19),
            temperatureVeryLight = prefs.getFloat("temp_very_light", 20.0f).toDouble(),
            temperatureLight = prefs.getFloat("temp_light", 15.0f).toDouble(),
            temperatureModerate = prefs.getFloat("temp_moderate", 10.0f).toDouble(),
            temperatureWarm = prefs.getFloat("temp_warm", 5.0f).toDouble(),
            temperatureVeryWarm = prefs.getFloat("temp_very_warm", 0.0f).toDouble(),
            temperatureCold = prefs.getFloat("temp_cold", -5.0f).toDouble(),
            precipitationProbabilityThreshold = prefs.getFloat("precip_prob_threshold", 50.0f).toDouble(),
            precipitationAmountThreshold = prefs.getFloat("precip_amount_threshold", 0.5f).toDouble(),
            clothingMessageLevel1 = prefs.getString("clothing_msg_1", defaultConfig.clothingMessageLevel1) ?: defaultConfig.clothingMessageLevel1,
            clothingMessageLevel2 = prefs.getString("clothing_msg_2", defaultConfig.clothingMessageLevel2) ?: defaultConfig.clothingMessageLevel2,
            clothingMessageLevel3 = prefs.getString("clothing_msg_3", defaultConfig.clothingMessageLevel3) ?: defaultConfig.clothingMessageLevel3,
            clothingMessageLevel4 = prefs.getString("clothing_msg_4", defaultConfig.clothingMessageLevel4) ?: defaultConfig.clothingMessageLevel4,
            clothingMessageLevel5 = prefs.getString("clothing_msg_5", defaultConfig.clothingMessageLevel5) ?: defaultConfig.clothingMessageLevel5,
            clothingMessageLevel6 = prefs.getString("clothing_msg_6", defaultConfig.clothingMessageLevel6) ?: defaultConfig.clothingMessageLevel6,
            clothingMessageLevel7 = prefs.getString("clothing_msg_7", defaultConfig.clothingMessageLevel7) ?: defaultConfig.clothingMessageLevel7
        )
    }
    
    fun saveConfig(config: AppConfig) {
        prefs.edit().apply {
            putInt("morning_commute_start", config.morningCommuteStartHour)
            putInt("morning_commute_end", config.morningCommuteEndHour)
            putInt("evening_commute_start", config.eveningCommuteStartHour)
            putInt("evening_commute_end", config.eveningCommuteEndHour)
            putFloat("temp_very_light", config.temperatureVeryLight.toFloat())
            putFloat("temp_light", config.temperatureLight.toFloat())
            putFloat("temp_moderate", config.temperatureModerate.toFloat())
            putFloat("temp_warm", config.temperatureWarm.toFloat())
            putFloat("temp_very_warm", config.temperatureVeryWarm.toFloat())
            putFloat("temp_cold", config.temperatureCold.toFloat())
            putFloat("precip_prob_threshold", config.precipitationProbabilityThreshold.toFloat())
            putFloat("precip_amount_threshold", config.precipitationAmountThreshold.toFloat())
            putString("clothing_msg_1", config.clothingMessageLevel1)
            putString("clothing_msg_2", config.clothingMessageLevel2)
            putString("clothing_msg_3", config.clothingMessageLevel3)
            putString("clothing_msg_4", config.clothingMessageLevel4)
            putString("clothing_msg_5", config.clothingMessageLevel5)
            putString("clothing_msg_6", config.clothingMessageLevel6)
            putString("clothing_msg_7", config.clothingMessageLevel7)
            apply()
        }
        _config.value = config
    }
    
    fun resetToDefaults() {
        val defaultConfig = AppConfig()
        saveConfig(defaultConfig)
    }
}

