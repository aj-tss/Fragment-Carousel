package com.example.weather_test.repository

import retrofit2.Call // ✅ CORRECT
import android.util.Log
import com.example.weather_test.model.WeatherResponse
import com.example.weather_test.network.WeatherApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
/**
 * Repository class responsible for handling API interactions for weather data.
 *
 * This class:
 * - Initializes the Retrofit instance.
 * - Provides a method to fetch weather data from the API.
 */
class WeatherRepository {

    // Instance of the Weather API service
    private val api: WeatherApiService

    init {
        try {
            // Retrofit setup for network requests
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/") // OpenWeather API base URL
                .addConverterFactory(GsonConverterFactory.create()) // Convert JSON response to Kotlin objects
                .build()

            // Create API service instance
            api = retrofit.create(WeatherApiService::class.java)
            Log.d("WeatherRepository", "Retrofit instance created successfully.")

        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error initializing Retrofit: ${e.message}", e)
            throw RuntimeException("Failed to initialize Retrofit", e) // Crash the app if Retrofit setup fails
        }
    }

    /**
     * Fetches weather data for the given [city] using the provided [apiKey].
     * @param city The name of the city for which weather data is required.
     * @param apiKey The API key required for authentication.
     * @return A [Call] object for the weather data request.
     */
    fun getWeather(city: String, apiKey: String) = try {
        Log.d("WeatherRepository", "Fetching weather data for city: $city")
        api.getWeather(city, apiKey)
    } catch (e: Exception) {
        Log.e("WeatherRepository", "Error fetching weather data: ${e.message}", e)
        throw RuntimeException("Failed to fetch weather data", e) // Rethrow exception to handle it at a higher level
    }

    fun getWeatherByCoordinates(lat: Double, lon: Double, apiKey: String): Call<WeatherResponse> {
        return api.getWeatherByCoordinates(lat, lon, apiKey)
    }

}
