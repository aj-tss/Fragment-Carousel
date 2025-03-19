package com.example.weather_test.network

import com.example.weather_test.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
// define endpoint using https method
interface WeatherApiService {
    @GET("weather")
    fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse>
}
