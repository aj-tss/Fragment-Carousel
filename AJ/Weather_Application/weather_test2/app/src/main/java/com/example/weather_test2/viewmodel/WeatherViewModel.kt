package com.example.weather_test.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.weather_test.model.WeatherResponse
import com.example.weather_test.repository.WeatherRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()
    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse> get() = _weatherData

    fun fetchWeather(city: String, apiKey: String) {
        repository.getWeather(city, apiKey).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    _weatherData.value = response.body()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                // Handle API failure
            }
        })
    }
}
