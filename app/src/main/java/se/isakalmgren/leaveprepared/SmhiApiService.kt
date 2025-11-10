package se.isakalmgren.leaveprepared

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface SmhiApiService {
    @GET("category/snow1g/version/1/geotype/point/lon/14.2048/lat/57.781/data.json")
    suspend fun getWeatherForecast(): WeatherResponse
    
    companion object {
        private const val BASE_URL = "https://opendata-download-metfcst.smhi.se/api/"
        
        fun create(): SmhiApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(SmhiApiService::class.java)
        }
    }
}

