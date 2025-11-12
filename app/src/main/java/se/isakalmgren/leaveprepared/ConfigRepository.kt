package se.isakalmgren.leaveprepared

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId

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
    
    private fun getClothingMessageKey(level: Int, languageCode: String): String {
        return "clothing_msg_${level}_$languageCode"
    }
    
    fun loadConfig(): AppConfig {
        val defaultConfig = AppConfig()
        val currentLanguageCode = languageRepository.getCurrentLanguageCode()
        val defaultMessages = AppConfig.getDefaultClothingMessages(currentLanguageCode)
        
        return AppConfig(
            longitude = prefs.getFloat("longitude", defaultConfig.longitude.toFloat()).toDouble(),
            latitude = prefs.getFloat("latitude", defaultConfig.latitude.toFloat()).toDouble(),
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
            clothingMessageLevel1 = prefs.getString(
                getClothingMessageKey(1, currentLanguageCode),
                defaultMessages[1] ?: defaultConfig.clothingMessageLevel1
            ) ?: defaultMessages[1] ?: defaultConfig.clothingMessageLevel1,
            clothingMessageLevel2 = prefs.getString(
                getClothingMessageKey(2, currentLanguageCode),
                defaultMessages[2] ?: defaultConfig.clothingMessageLevel2
            ) ?: defaultMessages[2] ?: defaultConfig.clothingMessageLevel2,
            clothingMessageLevel3 = prefs.getString(
                getClothingMessageKey(3, currentLanguageCode),
                defaultMessages[3] ?: defaultConfig.clothingMessageLevel3
            ) ?: defaultMessages[3] ?: defaultConfig.clothingMessageLevel3,
            clothingMessageLevel4 = prefs.getString(
                getClothingMessageKey(4, currentLanguageCode),
                defaultMessages[4] ?: defaultConfig.clothingMessageLevel4
            ) ?: defaultMessages[4] ?: defaultConfig.clothingMessageLevel4,
            clothingMessageLevel5 = prefs.getString(
                getClothingMessageKey(5, currentLanguageCode),
                defaultMessages[5] ?: defaultConfig.clothingMessageLevel5
            ) ?: defaultMessages[5] ?: defaultConfig.clothingMessageLevel5,
            clothingMessageLevel6 = prefs.getString(
                getClothingMessageKey(6, currentLanguageCode),
                defaultMessages[6] ?: defaultConfig.clothingMessageLevel6
            ) ?: defaultMessages[6] ?: defaultConfig.clothingMessageLevel6,
            clothingMessageLevel7 = prefs.getString(
                getClothingMessageKey(7, currentLanguageCode),
                defaultMessages[7] ?: defaultConfig.clothingMessageLevel7
            ) ?: defaultMessages[7] ?: defaultConfig.clothingMessageLevel7
        )
    }
    
    fun saveConfig(config: AppConfig) {
        val currentLanguageCode = languageRepository.getCurrentLanguageCode()
        prefs.edit().apply {
            putFloat("longitude", config.longitude.toFloat())
            putFloat("latitude", config.latitude.toFloat())
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
            // Save clothing messages with language code
            putString(getClothingMessageKey(1, currentLanguageCode), config.clothingMessageLevel1)
            putString(getClothingMessageKey(2, currentLanguageCode), config.clothingMessageLevel2)
            putString(getClothingMessageKey(3, currentLanguageCode), config.clothingMessageLevel3)
            putString(getClothingMessageKey(4, currentLanguageCode), config.clothingMessageLevel4)
            putString(getClothingMessageKey(5, currentLanguageCode), config.clothingMessageLevel5)
            putString(getClothingMessageKey(6, currentLanguageCode), config.clothingMessageLevel6)
            putString(getClothingMessageKey(7, currentLanguageCode), config.clothingMessageLevel7)
            apply()
        }
        _config.value = config
    }
    
    fun resetToDefaults() {
        val currentLanguageCode = languageRepository.getCurrentLanguageCode()
        val defaultMessages = AppConfig.getDefaultClothingMessages(currentLanguageCode)
        val defaultConfig = AppConfig(
            clothingMessageLevel1 = defaultMessages[1] ?: "Shorts and t-shirt",
            clothingMessageLevel2 = defaultMessages[2] ?: "T-shirt with a light jacket",
            clothingMessageLevel3 = defaultMessages[3] ?: "Long sleeves and a light jacket",
            clothingMessageLevel4 = defaultMessages[4] ?: "Sweater and jacket",
            clothingMessageLevel5 = defaultMessages[5] ?: "Heavy jacket and layers",
            clothingMessageLevel6 = defaultMessages[6] ?: "Winter coat and warm layers essential",
            clothingMessageLevel7 = defaultMessages[7] ?: "Heavy winter gear required"
        )
        saveConfig(defaultConfig)
    }
    
    fun reloadConfig() {
        _config.value = loadConfig()
    }
}

