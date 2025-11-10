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

fun getClothingLevel(temperature: Double): ClothingLevel {
    return when {
        temperature > 20 -> ClothingLevel.VERY_LIGHT
        temperature > 15 -> ClothingLevel.LIGHT
        temperature > 10 -> ClothingLevel.MODERATE
        temperature > 5 -> ClothingLevel.WARM
        temperature > 0 -> ClothingLevel.VERY_WARM
        temperature > -5 -> ClothingLevel.COLD
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

fun parseTime(timeString: String): ZonedDateTime? {
    return try {
        val instant = Instant.parse(timeString)
        // Convert to Sweden timezone (Europe/Stockholm handles DST automatically)
        instant.atZone(ZoneId.of("Europe/Stockholm"))
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

fun analyzeWeatherForCommute(
    timeSeries: List<TimeSeries>,
    startHour: Int,
    endHour: Int,
    commuteName: String
): WeatherRecommendation? {
    val now = ZonedDateTime.now(ZoneId.of("Europe/Stockholm"))
    val currentDate = now.toLocalDate()
    
    // Filter time series for the specified hour range (local time) and only future times
    val allCommuteHours = timeSeries.filter { timeEntry ->
        val localTime = parseTime(timeEntry.time)
        localTime?.let {
            val hour = it.hour
            // Only include times in the future and within the hour range
            hour in startHour until endHour && it.isAfter(now)
        } ?: false
    }
    
    if (allCommuteHours.isEmpty()) return null
    
    // Group by date and get the first (earliest) date's commute hours
    val commuteHoursByDate = allCommuteHours.groupBy { entry ->
        parseTime(entry.time)?.toLocalDate()
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
    
    // Determine if rain clothes are needed
    // Rain clothes needed if: precipitation probability > 50% OR precipitation amount > 0.5mm
    val needsRainClothes = maxPrecipitationProb > 50.0 || maxPrecipitationAmount > 0.5
    
    val clothingLevel = getClothingLevel(avgTemperature)
    val clothingMessage = getClothingMessage(clothingLevel)
    
    val message = buildString {
        append(clothingMessage)
        if (needsRainClothes) {
            append("\n🌧️ Bring rain clothes! ")
            if (maxPrecipitationProb > 50) {
                append("Precipitation probability: ${maxPrecipitationProb.toInt()}%")
            }
            if (maxPrecipitationAmount > 0.5) {
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

fun analyzeWeatherForCommutes(timeSeries: List<TimeSeries>): CommuteRecommendations {
    val now = ZonedDateTime.now(ZoneId.of("Europe/Stockholm"))
    val currentHour = now.hour
    
    // Morning commute: 7-9 AM local time
    // Only show if the morning window hasn't passed today (before 9 AM)
    val morningCommuteRaw = if (currentHour < 9) {
        analyzeWeatherForCommute(timeSeries, 7, 9, "Morning Commute (7-9 AM)")
    } else {
        null
    }
    
    // Evening commute: 4-7 PM local time (16-19 in 24h format)
    val eveningCommute = analyzeWeatherForCommute(timeSeries, 16, 19, "Evening Commute (4-7 PM)")
    
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