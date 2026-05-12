package se.isakalmgren.routesuit

import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

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
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        retrofit.create(SmhiApiService::class.java)
    }
    
    single<LocationHelper> {
        LocationHelper(androidContext())
    }
}

