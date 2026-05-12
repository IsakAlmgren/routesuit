package se.isakalmgren.routesuit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

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
            Timber.d("Fetching weather for coordinates: lon=$lonStr, lat=$latStr")
            
            val response = apiService.getWeatherForecast(
                longitude = lonStr,
                latitude = latStr
            )
            val recommendations = analyzeWeatherForCommutes(response.timeSeries, appConfig, applicationContext)
            
            // Check if today is an allowed notification day
            val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            val allowedDays = appConfig.notificationDays
            
            if (allowedDays.contains(today)) {
                sendNotification(recommendations)
            } else {
                Timber.d("Skipping notification - today (day $today) is not in allowed days: $allowedDays")
            }
            
            // Reschedule for next day after successful completion
            NotificationScheduler.scheduleNextDay(applicationContext, configRepository)
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error in doWork")
            // On failure, still reschedule but with retry backoff
            NotificationScheduler.scheduleNextDay(applicationContext, configRepository)
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
        
        // Create intent to open MainActivity when notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(1, notification)
    }
}

