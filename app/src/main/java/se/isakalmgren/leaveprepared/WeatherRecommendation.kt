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
    VERY_LIGHT,    // > 20°C - Light clothing
    LIGHT,         // 15-20°C - T-shirt, light jacket
    MODERATE,      // 10-15°C - Long sleeves, light jacket
    WARM,          // 5-10°C - Sweater, jacket
    VERY_WARM,     // 0-5°C - Heavy jacket, layers
    COLD,          // -5 to 0°C - Winter coat, warm layers
    VERY_COLD       // < -5°C - Heavy winter gear
}

fun getClothingLevel(temperature: Double, config: AppConfig): ClothingLevel {
    return when {
        temperature > config.temperatureVeryLight -> ClothingLevel.VERY_LIGHT
        temperature > config.temperatureLight -> ClothingLevel.LIGHT
        temperature > config.temperatureModerate -> ClothingLevel.MODERATE
        temperature > config.temperatureWarm -> ClothingLevel.WARM
        temperature > config.temperatureVeryWarm -> ClothingLevel.VERY_WARM
        temperature > config.temperatureCold -> ClothingLevel.COLD
        else -> ClothingLevel.VERY_COLD
    }
}

fun getClothingMessage(level: ClothingLevel): String {
    return when (level) {
        ClothingLevel.VERY_LIGHT -> "Light clothing - shorts and t-shirt weather"
        ClothingLevel.LIGHT -> "Light clothing - t-shirt with a light jacket"
        ClothingLevel.MODERATE -> "Moderate clothing - long sleeves and a light jacket"
        ClothingLevel.WARM -> "Warm clothing - sweater and jacket recommended"
        ClothingLevel.VERY_WARM -> "Very warm clothing - heavy jacket and layers"
        ClothingLevel.COLD -> "Cold weather - winter coat and warm layers essential"
        ClothingLevel.VERY_COLD -> "Very cold - heavy winter gear required"
    }
}

fun parseTime(timeString: String, config: AppConfig): ZonedDateTime? {
    return try {
        val instant = Instant.parse(timeString)
        // Convert to configured timezone
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
    val clothingMessage = getClothingMessage(clothingLevel)
    
    val message = buildString {
        append(clothingMessage)
        if (needsRainClothes) {
            append("\n🌧️ Bring rain clothes! ")
            if (maxPrecipitationProb > config.precipitationProbabilityThreshold) {
                append("Precipitation probability: ${maxPrecipitationProb.toInt()}%")
            }
            if (maxPrecipitationAmount > config.precipitationAmountThreshold) {
                append(" Expected precipitation: ${String.format("%.1f", maxPrecipitationAmount)} mm")
            }
        } else {
            append("\n☀️ No rain expected")
        }
    }
    
    val dayLabel = getDayLabel(commuteDate, currentDate)
    
    return WeatherRecommendation(
        needsRainClothes = needsRainClothes,
        clothingLevel = clothingLevel,
        temperature = avgTemperature,
        precipitationProbability = maxPrecipitationProb,
        precipitationAmount = maxPrecipitationAmount,
        message = message,
        timeWindow = commuteName,
        date = commuteDate,
        dayLabel = dayLabel
    )
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
        val updatedMessage = buildString {
            append(getClothingMessage(morningCommuteRaw.clothingLevel))
            append("\n🌧️ Bring rain clothes for later! ")
            append("Rain expected on your way home (${eveningCommute.precipitationProbability.toInt()}% chance, ")
            append("${String.format("%.1f", eveningCommute.precipitationAmount)} mm)")
        }
        
        morningCommuteRaw.copy(
            needsRainClothes = true,
            message = updatedMessage,
            rainForLater = true
        )
    } else {
        morningCommuteRaw
    }
    
    return CommuteRecommendations(morningCommute, eveningCommute)
}