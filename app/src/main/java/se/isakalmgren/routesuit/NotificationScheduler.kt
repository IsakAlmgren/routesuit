package se.isakalmgren.routesuit

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val WORK_NAME = "weather_notification_work"
    private const val TAG = "NotificationScheduler"
    
    fun scheduleDailyNotification(context: Context, configRepository: ConfigRepository? = null) {
        val workManager = WorkManager.getInstance(context)
        
        // Get config to check allowed notification days
        val allowedDays = configRepository?.getConfig()?.notificationDays 
            ?: AppConfig().notificationDays
        
        // Find next allowed day
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 7)
        calendar.set(Calendar.MINUTE, 30)
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
                Log.w(TAG, "No allowed notification days configured, scheduling for tomorrow anyway")
            }
        }
        
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val initialDelayMinutes = TimeUnit.MILLISECONDS.toMinutes(delay)
        
        // Ensure minimum delay of 15 minutes (WorkManager requirement for reliability)
        val finalDelayMinutes = maxOf(initialDelayMinutes, 15)
        
        Log.d(TAG, "Scheduling notification in $finalDelayMinutes minutes for day ${calendar.get(Calendar.DAY_OF_WEEK)}")
        
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
        Log.d(TAG, "Notification work enqueued with ID: ${workRequest.id}")
    }
    
    fun scheduleNextDay(context: Context, configRepository: ConfigRepository? = null) {
        val workManager = WorkManager.getInstance(context)
        
        // Get config to check allowed notification days
        val allowedDays = configRepository?.getConfig()?.notificationDays 
            ?: AppConfig().notificationDays
        
        // Calculate next 7:30 AM and find next allowed day
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 7)
        calendar.set(Calendar.MINUTE, 30)
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
            Log.w(TAG, "No allowed notification days configured, scheduling for tomorrow anyway")
        }
        
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delay)
        
        // Ensure minimum delay of 15 minutes
        val finalDelayMinutes = maxOf(delayMinutes, 15)
        
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
        Log.d(TAG, "Rescheduled notification for day ${calendar.get(Calendar.DAY_OF_WEEK)} at 7:30 AM (in $finalDelayMinutes minutes) with ID: ${workRequest.id}")
    }
    
    fun cancelNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "Cancelled all notification work")
    }
}

