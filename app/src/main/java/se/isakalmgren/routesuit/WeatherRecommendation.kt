package se.isakalmgren.routesuit

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WeatherRecommendation(
    val needsRainClothes: Boolean,
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

/**
 * Generates the recommendation message for a WeatherRecommendation.
 * 
 * @param recommendation The weather recommendation to generate a message for
 * @param config The app configuration containing thresholds
 * @param context The context to access string resources
 * @return The formatted recommendation message
 */
fun generateRecommendationMessage(recommendation: WeatherRecommendation, config: AppConfig, context: Context): String {
    return buildString {
        if (recommendation.needsRainClothes) {
            if (recommendation.rainForLater) {
                // Rain for later case - use the recommendation's precipitation values
                // (which should be set to the evening commute's values)
                append(context.getString(R.string.bring_rain_clothes_for_later_message))
                append(context.getString(R.string.rain_expected_home, recommendation.precipitationProbability.toInt(), recommendation.precipitationAmount))
            } else {
                // Normal rain case
                append(context.getString(R.string.bring_rain_clothes_message))
                if (recommendation.precipitationProbability > config.precipitationProbabilityThreshold) {
                    append(context.getString(R.string.precipitation_probability, recommendation.precipitationProbability.toInt()))
                }
                if (recommendation.precipitationAmount > config.precipitationAmountThreshold) {
                    append(" " + context.getString(R.string.expected_precipitation, recommendation.precipitationAmount))
                }
            }
        } else {
            append(context.getString(R.string.no_rain_expected_message))
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

fun getDayLabel(date: LocalDate, currentDate: LocalDate, context: Context): String {
    return when {
        date == currentDate -> context.getString(R.string.today)
        date == currentDate.plusDays(1) -> context.getString(R.string.tomorrow)
        else -> {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
            date.format(formatter)
        }
    }
}

fun formatHourForDisplay(hour24: Int, context: Context): String {
    // For now, keep the AM/PM format as is since it's standard
    // If needed, we can add 24-hour format support later
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
    config: AppConfig,
    context: Context
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
    
    val dayLabel = getDayLabel(commuteDate, currentDate, context)
    
    val recommendation = WeatherRecommendation(
        needsRainClothes = needsRainClothes,
        temperature = avgTemperature,
        precipitationProbability = maxPrecipitationProb,
        precipitationAmount = maxPrecipitationAmount,
        message = "", // Will be generated below
        timeWindow = commuteName,
        date = commuteDate,
        dayLabel = dayLabel,
        rainForLater = false
    )
    
    return recommendation.copy(message = generateRecommendationMessage(recommendation, config, context))
}

fun analyzeWeatherForCommutes(timeSeries: List<TimeSeries>, config: AppConfig, context: Context): CommuteRecommendations {
    val now = ZonedDateTime.now(config.timezone)
    val currentHour = now.hour
    
    // Morning commute: using config timespan
    // Only show if the morning window hasn't passed today
    val morningCommuteRaw = if (currentHour < config.morningCommuteEndHour) {
        val startDisplay = formatHourForDisplay(config.morningCommuteStartHour, context)
        val endDisplay = formatHourForDisplay(config.morningCommuteEndHour, context)
        val commuteName = context.getString(R.string.morning_commute_time, startDisplay, endDisplay)
        analyzeWeatherForCommute(
            timeSeries, 
            config.morningCommuteStartHour, 
            config.morningCommuteEndHour, 
            commuteName,
            config,
            context
        )
    } else {
        null
    }
    
    // Evening commute: using config timespan
    val eveningStartDisplay = formatHourForDisplay(config.eveningCommuteStartHour, context)
    val eveningEndDisplay = formatHourForDisplay(config.eveningCommuteEndHour, context)
    val eveningCommuteName = context.getString(R.string.evening_commute_time, eveningStartDisplay, eveningEndDisplay)
    val eveningCommute = analyzeWeatherForCommute(
        timeSeries, 
        config.eveningCommuteStartHour, 
        config.eveningCommuteEndHour, 
        eveningCommuteName,
        config,
        context
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
        updatedRecommendation.copy(message = generateRecommendationMessage(updatedRecommendation, config, context))
    } else {
        morningCommuteRaw
    }
    
    return CommuteRecommendations(morningCommute, eveningCommute)
}