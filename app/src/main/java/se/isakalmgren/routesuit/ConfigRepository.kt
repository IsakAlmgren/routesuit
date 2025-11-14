package se.isakalmgren.routesuit

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
            // Migration: check for new key first, then fall back to old key if it exists
            temperatureHot = if (prefs.contains("temp_hot")) {
                prefs.getFloat("temp_hot", 20.0f).toDouble()
            } else {
                prefs.getFloat("temp_very_light", 20.0f).toDouble() // Old key: temp_very_light -> temp_hot
            },
            // Migration: check for new key first, then fall back to old key if it exists
            temperatureWarm = if (prefs.contains("temp_warm") && !prefs.contains("temp_very_light")) {
                // New key exists and old keys don't - this is the new temp_warm (15°C)
                prefs.getFloat("temp_warm", 15.0f).toDouble()
            } else if (prefs.contains("temp_light")) {
                // Old key exists - migrate from temp_light (15°C)
                prefs.getFloat("temp_light", 15.0f).toDouble() // Old key: temp_light -> temp_warm
            } else {
                // Check if old temp_warm exists (was 5°C, now should be temp_cool)
                val oldTempWarm = prefs.getFloat("temp_warm", 15.0f).toDouble()
                if (oldTempWarm < 10.0f && prefs.contains("temp_warm")) {
                    // This is the old temp_warm (5°C), use default for new temp_warm
                    15.0
                } else {
                    oldTempWarm
                }
            },
            // Migration: check for new key first, then fall back to old key if it exists
            temperatureMild = if (prefs.contains("temp_mild")) {
                prefs.getFloat("temp_mild", 10.0f).toDouble()
            } else {
                prefs.getFloat("temp_moderate", 10.0f).toDouble() // Old key: temp_moderate -> temp_mild
            },
            // Migration: check for new key first, then fall back to old key if it exists
            temperatureCool = if (prefs.contains("temp_cool")) {
                prefs.getFloat("temp_cool", 5.0f).toDouble()
            } else {
                prefs.getFloat("temp_warm", 5.0f).toDouble() // Old key: temp_warm -> temp_cool
            },
            temperatureCold = if (prefs.contains("temp_very_cold") || prefs.contains("temp_cool")) {
                // New keys exist - use new temp_cold key
                if (prefs.contains("temp_cold")) {
                    val tempColdValue = prefs.getFloat("temp_cold", 0.0f).toDouble()
                    // If temp_cold is negative, it might be the old temp_cold (-5°C)
                    // Check if temp_very_cold exists to determine which it is
                    if (tempColdValue < -2.0f && !prefs.contains("temp_very_cold")) {
                        // This is old temp_cold (-5°C), use default for new temp_cold
                        0.0
                    } else {
                        tempColdValue
                    }
                } else {
                    // New key doesn't exist, check for old temp_very_warm
                    prefs.getFloat("temp_very_warm", 0.0f).toDouble() // Old key: temp_very_warm -> temp_cold
                }
            } else {
                // Old keys exist - migrate from temp_very_warm (0°C)
                prefs.getFloat("temp_very_warm", 0.0f).toDouble()
            },
            temperatureVeryCold = if (prefs.contains("temp_very_cold")) {
                prefs.getFloat("temp_very_cold", -5.0f).toDouble()
            } else {
                // Migrate from old temp_cold if it was -5°C
                val oldTempCold = prefs.getFloat("temp_cold", -5.0f).toDouble()
                if (oldTempCold < -2.0f) {
                    oldTempCold // This was the old temp_cold (-5°C)
                } else {
                    -5.0 // Default
                }
            },
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
            putFloat("temp_hot", config.temperatureHot.toFloat())
            putFloat("temp_warm", config.temperatureWarm.toFloat())
            putFloat("temp_mild", config.temperatureMild.toFloat())
            putFloat("temp_cool", config.temperatureCool.toFloat())
            putFloat("temp_cold", config.temperatureCold.toFloat())
            putFloat("temp_very_cold", config.temperatureVeryCold.toFloat())
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

