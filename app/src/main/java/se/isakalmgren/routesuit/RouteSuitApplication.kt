package se.isakalmgren.routesuit

import android.app.Application
import android.content.pm.ApplicationInfo
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class RouteSuitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging
        // Check if app is debuggable (works even if BuildConfig isn't generated yet)
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }
        // In release builds, Timber won't log anything (no-op)
        
        startKoin {
            androidContext(this@RouteSuitApplication)
            modules(appModule)
        }
    }
}

