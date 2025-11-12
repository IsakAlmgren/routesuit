package se.isakalmgren.leaveprepared

import java.time.ZoneId

/**
 * Application configuration containing all configurable values
 * for weather analysis and recommendations.
 */
data class AppConfig(
    // Location coordinates
    val longitude: Double = 14.2048,
    val latitude: Double = 57.781,
    
    // Commute timespans (24-hour format)
    val morningCommuteStartHour: Int = 7,
    val morningCommuteEndHour: Int = 9,
    val eveningCommuteStartHour: Int = 16,
    val eveningCommuteEndHour: Int = 19,
    
    // Temperature thresholds for clothing levels (in Celsius)
    val temperatureVeryLight: Double = 20.0,      // > 20°C
    val temperatureLight: Double = 15.0,          // > 15°C
    val temperatureModerate: Double = 10.0,        // > 10°C
    val temperatureWarm: Double = 5.0,            // > 5°C
    val temperatureVeryWarm: Double = 0.0,        // > 0°C
    val temperatureCold: Double = -5.0,           // > -5°C
    // Below -5°C is VERY_COLD
    
    // Precipitation thresholds
    val precipitationProbabilityThreshold: Double = 20.0,  // Percentage
    val precipitationAmountThreshold: Double = 0.5,        // Millimeters
    
    // Clothing level messages (configurable by user)
    val clothingMessageLevel1: String = "Shorts and t-shirt",
    val clothingMessageLevel2: String = "T-shirt with a light jacket",
    val clothingMessageLevel3: String = "Long sleeves and a light jacket",
    val clothingMessageLevel4: String = "Sweater and jacket",
    val clothingMessageLevel5: String = "Heavy jacket and layers",
    val clothingMessageLevel6: String = "Winter coat and warm layers essential",
    val clothingMessageLevel7: String = "Heavy winter gear required"
) {
    // Always use the system default timezone
    val timezone: ZoneId
        get() = ZoneId.systemDefault()
}

