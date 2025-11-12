package se.isakalmgren.leaveprepared

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single<LanguageRepository> {
        LanguageRepository(androidContext())
    }
    
    single<ConfigRepository> {
        ConfigRepository(androidContext(), get())
    }
    
    single<AppConfig> {
        get<ConfigRepository>().getConfig()
    }
    
    single<SmhiApiService> {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://opendata-download-metfcst.smhi.se/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        retrofit.create(SmhiApiService::class.java)
    }
    
    single<LocationHelper> {
        LocationHelper(androidContext())
    }
}

