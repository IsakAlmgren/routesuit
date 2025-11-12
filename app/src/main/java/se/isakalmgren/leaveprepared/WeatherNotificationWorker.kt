package se.isakalmgren.leaveprepared

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {
    
    private val apiService: SmhiApiService by inject()
    private val configRepository: ConfigRepository by inject()
    
    override suspend fun doWork(): Result {
        return try {
            val appConfig = configRepository.getConfig()
            val lonStr = String.format(java.util.Locale.US, "%.4f", appConfig.longitude)
            val latStr = String.format(java.util.Locale.US, "%.3f", appConfig.latitude)
            val url = "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/geotype/point/lon/$lonStr/lat/$latStr/data.json"
            android.util.Log.d("WeatherNotificationWorker", "Attempting to fetch weather from URL: $url")
            android.util.Log.d("WeatherNotificationWorker", "Longitude: ${appConfig.longitude} -> $lonStr, Latitude: ${appConfig.latitude} -> $latStr")
            
            val response = apiService.getWeatherForecast(
                longitude = lonStr,
                latitude = latStr
            )
            val recommendations = analyzeWeatherForCommutes(response.timeSeries, appConfig, applicationContext)
            
            sendNotification(recommendations)
            
            // Reschedule for next day after successful completion
            NotificationScheduler.scheduleNextDay(applicationContext)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("WeatherNotificationWorker", "Error in doWork", e)
            // On failure, still reschedule but with retry backoff
            NotificationScheduler.scheduleNextDay(applicationContext)
            Result.retry()
        }
    }
    
    private fun sendNotification(recommendations: CommuteRecommendations) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        val channelId = "weather_forecast_channel"
        val channelName = applicationContext.getString(R.string.notification_channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
        
        // Build notification message with both commutes
        val message = buildString {
            if (recommendations.morningCommute != null) {
                append(applicationContext.getString(R.string.notification_to_work))
                append(applicationContext.getString(R.string.temperature_format, recommendations.morningCommute.temperature))
                if (recommendations.morningCommute.needsRainClothes) {
                    if (recommendations.morningCommute.rainForLater) {
                        append(applicationContext.getString(R.string.notification_bring_rain_gear_later))
                    } else {
                        append(applicationContext.getString(R.string.notification_rain_clothes_needed))
                    }
                }
                if (recommendations.eveningCommute != null) {
                    append("\n")
                }
            }
            if (recommendations.eveningCommute != null) {
                append(applicationContext.getString(R.string.notification_from_work))
                append(applicationContext.getString(R.string.temperature_format, recommendations.eveningCommute.temperature))
                if (recommendations.eveningCommute.needsRainClothes) {
                    append(applicationContext.getString(R.string.notification_rain_clothes_needed))
                }
            }
        }
        
        // Determine if rain clothes are needed for either commute
        val needsRainClothes = recommendations.morningCommute?.needsRainClothes == true || 
                               recommendations.eveningCommute?.needsRainClothes == true
        
        val title = if (needsRainClothes) {
            applicationContext.getString(R.string.bring_rain_clothes_today)
        } else {
            applicationContext.getString(R.string.weather_update)
        }
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1, notification)
    }
}

