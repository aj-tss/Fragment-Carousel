package com.example.weather_test2

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.weather_test.viewmodel.WeatherViewModel
import com.example.weather_test2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewModel: WeatherViewModel by viewModels()
    private val apiKey = "87d8b541e3df6b7231276a7a93b4e6e1" //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        // binding
// Binding
        var binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        val cityInput = findViewById<EditText>(R.id.cityInput)
        val getWeatherButton = findViewById<Button>(R.id.getWeatherButton)
        val weatherText = findViewById<TextView>(R.id.weatherText)

        getWeatherButton.setOnClickListener {
            val city = cityInput.text.toString()
            if (city.isNotEmpty()) {
                viewModel.fetchWeather(city, apiKey)
            }
        }

        viewModel.weatherData.observe(this, Observer { weather ->
            val info = "City: ${weather.name}\n" +
                    "Temperature: ${weather.main.temp}°C\n" +
                    "Humidity: ${weather.main.humidity}%\n" +
                    "Condition: ${weather.weather[0].description}"
            weatherText.text = info
        })
    }
}
