package se.isakalmgren.leaveprepared

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class WeatherRecommendation(
    val needsRainClothes: Boolean,
    val clothingLevel: ClothingLevel,
    val temperature: Double,
    val precipitationProbability: Double,
    val precipitationAmount: Double,
    val message: String,
    val timeWindow: String = "",
    val rainForLater: Boolean = false, // True if rain is needed for later in the day, not during this commute
    val date: LocalDate? = null, // The date this recommendation is for
    val dayLabel: String = "" // "Today" or "Tomorrow" or formatted date
)

data class CommuteRecommendations(
    val morningCommute: WeatherRecommendation?,
    val eveningCommute: WeatherRecommendation?
)

enum class ClothingLevel {
    LEVEL_1,  // > 20°C
    LEVEL_2,  // 15-20°C
    LEVEL_3,  // 10-15°C
    LEVEL_4,  // 5-10°C
    LEVEL_5,  // 0-5°C
    LEVEL_6,  // -5 to 0°C
    LEVEL_7   // < -5°C
}

fun getClothingLevel(temperature: Double, config: AppConfig): ClothingLevel {
    return when {
        temperature > config.temperatureVeryLight -> ClothingLevel.LEVEL_1
        temperature > config.temperatureLight -> ClothingLevel.LEVEL_2
        temperature > config.temperatureModerate -> ClothingLevel.LEVEL_3
        temperature > config.temperatureWarm -> ClothingLevel.LEVEL_4
        temperature > config.temperatureVeryWarm -> ClothingLevel.LEVEL_5
        temperature > config.temperatureCold -> ClothingLevel.LEVEL_6
        else -> ClothingLevel.LEVEL_7
    }
}

fun getClothingMessage(level: ClothingLevel, config: AppConfig): String {
    return when (level) {
        ClothingLevel.LEVEL_1 -> config.clothingMessageLevel1
        ClothingLevel.LEVEL_2 -> config.clothingMessageLevel2
        ClothingLevel.LEVEL_3 -> config.clothingMessageLevel3
        ClothingLevel.LEVEL_4 -> config.clothingMessageLevel4
        ClothingLevel.LEVEL_5 -> config.clothingMessageLevel5
        ClothingLevel.LEVEL_6 -> config.clothingMessageLevel6
        ClothingLevel.LEVEL_7 -> config.clothingMessageLevel7
    }
}

/**
 * Generates the recommendation message for a WeatherRecommendation.
 * 
 * @param recommendation The weather recommendation to generate a message for
 * @param config The app configuration containing thresholds and clothing messages
 * @return The formatted recommendation message
 */
fun generateRecommendationMessage(recommendation: WeatherRecommendation, config: AppConfig): String {
    val clothingMessage = getClothingMessage(recommendation.clothingLevel, config)
    
    return buildString {
        append(clothingMessage)
        if (recommendation.needsRainClothes) {
            if (recommendation.rainForLater) {
                // Rain for later case - use the recommendation's precipitation values
                // (which should be set to the evening commute's values)
                append("\n🌧️ Bring rain clothes for later! ")
                append("Rain expected on your way home (${recommendation.precipitationProbability.toInt()}% chance, ")
                append("${String.format("%.1f", recommendation.precipitationAmount)} mm)")
            } else {
                // Normal rain case
                append("\n🌧️ Bring rain clothes! ")
                if (recommendation.precipitationProbability > config.precipitationProbabilityThreshold) {
                    append("Precipitation probability: ${recommendation.precipitationProbability.toInt()}%")
                }
                if (recommendation.precipitationAmount > config.precipitationAmountThreshold) {
                    append(" Expected precipitation: ${String.format("%.1f", recommendation.precipitationAmount)} mm")
                }
            }
        } else {
            append("\n☀️ No rain expected")
        }
    }
}

fun parseTime(timeString: String, config: AppConfig): ZonedDateTime? {
    return try {
        val instant = Instant.parse(timeString)
        // Convert to system default timezone
        instant.atZone(config.timezone)
    } catch (e: Exception) {
        null
    }
}

fun getDayLabel(date: LocalDate, currentDate: LocalDate): String {
    return when {
        date == currentDate -> "Today"
        date == currentDate.plusDays(1) -> "Tomorrow"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
            date.format(formatter)
        }
    }
}

fun formatHourForDisplay(hour24: Int): String {
    return when {
        hour24 == 0 -> "12 AM"
        hour24 < 12 -> "$hour24 AM"
        hour24 == 12 -> "12 PM"
        else -> "${hour24 - 12} PM"
    }
}

fun analyzeWeatherForCommute(
    timeSeries: List<TimeSeries>,
    startHour: Int,
    endHour: Int,
    commuteName: String,
    config: AppConfig
): WeatherRecommendation? {
    val now = ZonedDateTime.now(config.timezone)
    val currentDate = now.toLocalDate()
    
    // Filter time series for the specified hour range (local time) and only future times
    val allCommuteHours = timeSeries.filter { timeEntry ->
        val localTime = parseTime(timeEntry.time, config)
        localTime?.let {
            val hour = it.hour
            // Only include times in the future and within the hour range
            hour in startHour until endHour && it.isAfter(now)
        } ?: false
    }
    
    if (allCommuteHours.isEmpty()) return null
    
    // Group by date and get the first (earliest) date's commute hours
    val commuteHoursByDate = allCommuteHours.groupBy { entry ->
        parseTime(entry.time, config)?.toLocalDate()
    }
    
    // Get the earliest date (next occurrence)
    val commuteDate = commuteHoursByDate.keys.filterNotNull().minOrNull()
    if (commuteDate == null) return null
    
    // Get all commute hours for that specific date
    val commuteHours = commuteHoursByDate[commuteDate] ?: return null
    
    // Calculate average temperature and max precipitation probability
    val temperatures = commuteHours.mapNotNull { it.data.airTemperature }
    if (temperatures.isEmpty()) return null
    
    val avgTemperature = temperatures.average()
    val maxPrecipitationProb = commuteHours.maxOfOrNull { it.data.probabilityOfPrecipitation ?: 0.0 } ?: 0.0
    val maxPrecipitationAmount = commuteHours.maxOfOrNull { it.data.precipitationAmountMean ?: 0.0 } ?: 0.0
    
    // Determine if rain clothes are needed using config thresholds
    val needsRainClothes = maxPrecipitationProb > config.precipitationProbabilityThreshold || 
                          maxPrecipitationAmount > config.precipitationAmountThreshold
    
    val clothingLevel = getClothingLevel(avgTemperature, config)
    
    val dayLabel = getDayLabel(commuteDate, currentDate)
    
    val recommendation = WeatherRecommendation(
        needsRainClothes = needsRainClothes,
        clothingLevel = clothingLevel,
        temperature = avgTemperature,
        precipitationProbability = maxPrecipitationProb,
        precipitationAmount = maxPrecipitationAmount,
        message = "", // Will be generated below
        timeWindow = commuteName,
        date = commuteDate,
        dayLabel = dayLabel,
        rainForLater = false
    )
    
    return recommendation.copy(message = generateRecommendationMessage(recommendation, config))
}

fun analyzeWeatherForCommutes(timeSeries: List<TimeSeries>, config: AppConfig): CommuteRecommendations {
    val now = ZonedDateTime.now(config.timezone)
    val currentHour = now.hour
    
    // Morning commute: using config timespan
    // Only show if the morning window hasn't passed today
    val morningCommuteRaw = if (currentHour < config.morningCommuteEndHour) {
        val startDisplay = formatHourForDisplay(config.morningCommuteStartHour)
        val endDisplay = formatHourForDisplay(config.morningCommuteEndHour)
        val commuteName = "Morning Commute ($startDisplay-$endDisplay)"
        analyzeWeatherForCommute(
            timeSeries, 
            config.morningCommuteStartHour, 
            config.morningCommuteEndHour, 
            commuteName,
            config
        )
    } else {
        null
    }
    
    // Evening commute: using config timespan
    val eveningStartDisplay = formatHourForDisplay(config.eveningCommuteStartHour)
    val eveningEndDisplay = formatHourForDisplay(config.eveningCommuteEndHour)
    val eveningCommuteName = "Evening Commute ($eveningStartDisplay-$eveningEndDisplay)"
    val eveningCommute = analyzeWeatherForCommute(
        timeSeries, 
        config.eveningCommuteStartHour, 
        config.eveningCommuteEndHour, 
        eveningCommuteName,
        config
    )
    
    // If evening commute needs rain clothes, morning commute should also recommend bringing them
    val morningCommute = if (morningCommuteRaw != null && eveningCommute?.needsRainClothes == true && !morningCommuteRaw.needsRainClothes) {
        // Update morning recommendation to include rain gear for later
        // Use evening commute's precipitation values for the message
        val updatedRecommendation = morningCommuteRaw.copy(
            needsRainClothes = true,
            rainForLater = true,
            precipitationProbability = eveningCommute.precipitationProbability,
            precipitationAmount = eveningCommute.precipitationAmount,
            message = "" // Will be generated below
        )
        updatedRecommendation.copy(message = generateRecommendationMessage(updatedRecommendation, config))
    } else {
        morningCommuteRaw
    }
    
    return CommuteRecommendations(morningCommute, eveningCommute)
}