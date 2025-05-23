package com.example.weather_test.network

import com.example.weather_test.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for fetching weather data.
 * This interface defines the API endpoints used to retrieve weather information
 * from a remote server.
 */
interface WeatherApiService {
    /**
     * Fetches weather data for the specified [city] using the provided [apiKey].
     * This function makes a GET request to the `/weather` endpoint.
     * The API returns a [WeatherResponse] object containing weather details.
     */
    @GET("weather")
    fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric", // Default to metric units (Celsius)
    ): Call<WeatherResponse>

    @GET("weather")
    fun getWeatherByCoordinates(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"  // default to Celsius
    ): Call<WeatherResponse>

}