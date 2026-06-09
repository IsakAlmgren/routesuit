package se.mildtanke.routesuit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

fun ensureNotificationChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        Constants.NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
    )
    manager.createNotificationChannel(channel)
}

fun buildWeatherNotification(context: Context, recommendations: CommuteRecommendations): android.app.Notification {
    ensureNotificationChannel(context)

    val message = buildString {
        if (recommendations.morningCommute != null) {
            append(context.getString(R.string.notification_to_work))
            append(context.getString(R.string.temperature_format, recommendations.morningCommute.temperature))
            if (recommendations.morningCommute.needsRainClothes) {
                if (recommendations.morningCommute.rainForLater) {
                    append(context.getString(R.string.notification_bring_rain_gear_later))
                } else {
                    append(context.getString(R.string.notification_rain_clothes_needed))
                }
            }
            if (recommendations.eveningCommute != null) append("\n")
        }
        if (recommendations.eveningCommute != null) {
            append(context.getString(R.string.notification_from_work))
            append(context.getString(R.string.temperature_format, recommendations.eveningCommute.temperature))
            if (recommendations.eveningCommute.needsRainClothes) {
                append(context.getString(R.string.notification_rain_clothes_needed))
            }
        }
    }

    val needsRainClothes = recommendations.morningCommute?.needsRainClothes == true ||
            recommendations.eveningCommute?.needsRainClothes == true

    val title = if (needsRainClothes) {
        context.getString(R.string.bring_rain_clothes_today)
    } else {
        context.getString(R.string.weather_update)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
}
