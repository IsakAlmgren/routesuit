package se.isakalmgren.leaveprepared

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
    
    fun scheduleDailyNotification(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        // Schedule notification for 7:30 AM every day
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 7)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If it's already past 7:30 AM today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val initialDelayMinutes = TimeUnit.MILLISECONDS.toMinutes(delay)
        
        // Ensure minimum delay of 15 minutes (WorkManager requirement for reliability)
        val finalDelayMinutes = maxOf(initialDelayMinutes, 15)
        
        Log.d(TAG, "Scheduling notification in $finalDelayMinutes minutes (${initialDelayMinutes} minutes until 7:30 AM)")
        
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
    
    fun scheduleNextDay(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        // Calculate next 7:30 AM (always tomorrow since this is called after notification)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 7)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Always schedule for tomorrow (since we just sent today's notification)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        
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
        Log.d(TAG, "Rescheduled notification for next day at 7:30 AM (in $finalDelayMinutes minutes) with ID: ${workRequest.id}")
    }
    
    fun cancelNotification(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "Cancelled all notification work")
    }
}

