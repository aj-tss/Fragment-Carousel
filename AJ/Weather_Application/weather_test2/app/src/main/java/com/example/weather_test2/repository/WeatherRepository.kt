package com.example.weather_test.repository

import com.example.weather_test.network.WeatherApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {
    private val api: WeatherApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(WeatherApiService::class.java)
    }

    fun getWeather(city: String, apiKey: String) = api.getWeather(city, apiKey)
}
