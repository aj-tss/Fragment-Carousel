package com.example.weather_test.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weather_test.model.WeatherResponse
import com.example.weather_test.repository.WeatherRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * ViewModel for handling weather data.
 *
 * This class is responsible for:
 * - Fetching weather data from the repository.
 * - Exposing the weather data as LiveData for observation by the UI.
 * - Handling API responses and failures.
 */
class WeatherViewModel : ViewModel() {

    // Repository instance for making API calls
    private val repository = WeatherRepository()

    // LiveData to store weather response data
    private val _weatherData = MutableLiveData<WeatherResponse>()

    // Exposes weather data as immutable LiveData to be observed by UI components.
    val weatherData: LiveData<WeatherResponse> get() = _weatherData

    /**
     * Fetches weather data for the given [city] using the provided [apiKey].
     * This function makes an asynchronous API call and updates [_weatherData]
     * when a successful response is received.
     *
     * @param city The name of the city for which to fetch weather data.
     * @param apiKey The API key required for authentication.
     */
    fun fetchWeather(city: String, apiKey: String) {
        Log.d("WeatherViewModel", "Fetching weather for city: $city")

        try {
            repository.getWeather(city, apiKey).enqueue(object : Callback<WeatherResponse> {

                /**
                 * Called when the API request receives a response.
                 * Updates [_weatherData] with the response body if successful.
                 */
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        _weatherData.value = response.body()
                        Log.d("WeatherViewModel", "Weather data received: ${response.body()}")
                    } else {
                        Log.w("WeatherViewModel", "API response unsuccessful: ${response.code()}")
                    }
                }

                /**
                 * Called when the API request fails.
                 * Logs the error message for debugging.
                 */
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("WeatherViewModel", "Failed to fetch weather data: ${t.message}", t)
                }
            })
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error fetching weather data: ${e.message}", e)
        }
    }

    // Fetch Weather by coordinates
    fun fetchWeatherByCoordinates(lat: Double, lon: Double, apiKey: String) {
        repository.getWeatherByCoordinates(lat, lon, apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    _weatherData.value = response.body()
                } else {
                    Log.w("WeatherViewModel", "API response unsuccessful: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherViewModel", "Failed to fetch weather data: ${t.message}", t)
            }
        })
    }

}
