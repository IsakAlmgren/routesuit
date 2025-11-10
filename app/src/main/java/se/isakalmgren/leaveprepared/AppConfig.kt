package se.isakalmgren.leaveprepared

import java.time.ZoneId

/**
 * Application configuration containing all configurable values
 * for weather analysis and recommendations.
 */
data class AppConfig(
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
    val precipitationProbabilityThreshold: Double = 50.0,  // Percentage
    val precipitationAmountThreshold: Double = 0.5,        // Millimeters
    
    // Clothing level messages (configurable by user)
    val clothingMessageLevel1: String = "Light clothing - shorts and t-shirt weather",
    val clothingMessageLevel2: String = "Light clothing - t-shirt with a light jacket",
    val clothingMessageLevel3: String = "Moderate clothing - long sleeves and a light jacket",
    val clothingMessageLevel4: String = "Cool weather - sweater and jacket recommended",
    val clothingMessageLevel5: String = "Cold weather - heavy jacket and layers",
    val clothingMessageLevel6: String = "Very cold - winter coat and warm layers essential",
    val clothingMessageLevel7: String = "Extremely cold - heavy winter gear required",
    
    // Timezone
    val timezone: ZoneId = ZoneId.of("Europe/Stockholm")
)

