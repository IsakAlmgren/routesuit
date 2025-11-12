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
            val response = apiService.getWeatherForecast()
            val appConfig = configRepository.getConfig()
            val recommendations = analyzeWeatherForCommutes(response.timeSeries, appConfig)
            
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
        val channelName = "Weather Forecast"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        notificationManager.createNotificationChannel(channel)
        
        // Build notification message with both commutes
        val message = buildString {
            if (recommendations.morningCommute != null) {
                append("🌅 To Work: ")
                append("${String.format("%.1f", recommendations.morningCommute.temperature)}°C")
                if (recommendations.morningCommute.needsRainClothes) {
                    if (recommendations.morningCommute.rainForLater) {
                        append(" 🌧️ Bring rain gear for later!")
                    } else {
                        append(" 🌧️ Rain clothes needed")
                    }
                }
                if (recommendations.eveningCommute != null) {
                    append("\n")
                }
            }
            if (recommendations.eveningCommute != null) {
                append("🌆 From Work: ")
                append("${String.format("%.1f", recommendations.eveningCommute.temperature)}°C")
                if (recommendations.eveningCommute.needsRainClothes) {
                    append(" 🌧️ Rain clothes needed")
                }
            }
        }
        
        // Determine if rain clothes are needed for either commute
        val needsRainClothes = recommendations.morningCommute?.needsRainClothes == true || 
                               recommendations.eveningCommute?.needsRainClothes == true
        
        val title = if (needsRainClothes) {
            "🌧️ Bring Rain Clothes Today!"
        } else {
            "☀️ Weather Update"
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

