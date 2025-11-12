package se.isakalmgren.leaveprepared

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface SmhiApiService {
    @GET("category/snow1g/version/1/geotype/point/lon/{longitude}/lat/{latitude}/data.json")
    suspend fun getWeatherForecast(
        @Path("longitude") longitude: String,
        @Path("latitude") latitude: String
    ): WeatherResponse
}

