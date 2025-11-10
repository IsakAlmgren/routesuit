package se.isakalmgren.leaveprepared

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LeavePreparedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@LeavePreparedApplication)
            modules(appModule)
        }
    }
}

