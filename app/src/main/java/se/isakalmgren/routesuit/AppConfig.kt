package se.isakalmgren.routesuit

import java.time.ZoneId
import java.util.Calendar

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
    
    // Precipitation thresholds
    val precipitationProbabilityThreshold: Double = 20.0,  // Percentage
    val precipitationAmountThreshold: Double = 0.5,        // Millimeters
    
    // Notification settings
    // Set of days of week when notifications should be sent (Calendar.DAY_OF_WEEK values)
    // Default: Monday through Friday (weekdays)
    val notificationDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    )
) {
    // Always use the system default timezone
    val timezone: ZoneId
        get() = ZoneId.systemDefault()
}

