package se.mildtanke.alltidredo

import android.app.NotificationManager
import android.content.Context
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
        notificationManager.notify(1, buildWeatherNotification(applicationContext, recommendations))
    }
}

