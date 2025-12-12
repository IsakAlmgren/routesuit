package se.isakalmgren.routesuit

import android.content.Context
import timber.log.Timber
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val WORK_NAME = "weather_notification_work"
    
    fun scheduleDailyNotification(context: Context, configRepository: ConfigRepository? = null) {
        val workManager = WorkManager.getInstance(context)
        
        // Get config to check allowed notification days
        val allowedDays = configRepository?.getConfig()?.notificationDays 
            ?: AppConfig().notificationDays
        
        // Find next allowed day
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, Constants.NOTIFICATION_DEFAULT_HOUR)
        calendar.set(Calendar.MINUTE, Constants.NOTIFICATION_DEFAULT_MINUTE)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If it's already past 7:30 AM today, check if today is allowed
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        if (calendar.timeInMillis <= System.currentTimeMillis() || !allowedDays.contains(today)) {
            // Move to tomorrow and find next allowed day
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            var daysToAdd = 0
            while (daysToAdd < 7 && !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                daysToAdd++
            }
            if (daysToAdd >= 7) {
                Timber.w("No allowed notification days configured, scheduling for tomorrow anyway")
            }
        }
        
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val initialDelayMinutes = TimeUnit.MILLISECONDS.toMinutes(delay)
        
        // Ensure minimum delay (WorkManager requirement for reliability)
        val finalDelayMinutes = maxOf(initialDelayMinutes, Constants.NOTIFICATION_MIN_DELAY_MINUTES)
        
        Timber.d("Scheduling notification in $finalDelayMinutes minutes for day ${calendar.get(Calendar.DAY_OF_WEEK)}")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Use OneTimeWorkRequest - the worker will reschedule itself after completion
        val workRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
            .setInitialDelay(finalDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        
        // Use enqueueUniqueWork to replace any existing work
        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Timber.d("Notification work enqueued with ID: ${workRequest.id}")
    }
    
    fun scheduleNextDay(context: Context, configRepository: ConfigRepository? = null) {
        val workManager = WorkManager.getInstance(context)
        
        // Get config to check allowed notification days
        val allowedDays = configRepository?.getConfig()?.notificationDays 
            ?: AppConfig().notificationDays
        
        // Calculate next notification time and find next allowed day
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, Constants.NOTIFICATION_DEFAULT_HOUR)
        calendar.set(Calendar.MINUTE, Constants.NOTIFICATION_DEFAULT_MINUTE)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Always start from tomorrow (since we just sent today's notification)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        
        // Find next allowed day
        var daysToAdd = 0
        while (daysToAdd < 7 && !allowedDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            daysToAdd++
        }
        if (daysToAdd >= 7) {
            Timber.w("No allowed notification days configured, scheduling for tomorrow anyway")
        }
        
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delay)
        
        // Ensure minimum delay
        val finalDelayMinutes = maxOf(delayMinutes, Constants.NOTIFICATION_MIN_DELAY_MINUTES)
        
        val workRequest = OneTimeWorkRequestBuilder<WeatherNotificationWorker>()
            .setInitialDelay(finalDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        // Use enqueueUniqueWork to replace any existing work
        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Timber.d("Rescheduled notification for day ${calendar.get(Calendar.DAY_OF_WEEK)} at 7:30 AM (in $finalDelayMinutes minutes) with ID: ${workRequest.id}")
    }
    
    fun cancelNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Timber.d("Cancelled all notification work")
    }
}

