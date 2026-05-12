package se.isakalmgren.routesuit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.context.GlobalContext
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        Timber.d("Device booted, rescheduling weather notifications")
        val configRepository = try {
            GlobalContext.get().get<ConfigRepository>()
        } catch (e: Exception) {
            Timber.w(e, "Koin not yet available in BootReceiver, using default config")
            null
        }
        NotificationScheduler.scheduleDailyNotification(context, configRepository)
    }
}
