package com.example.weather_test.model

/**
 * Represents the weather response from the API.
 * This data class contains:
 * - [main]: Main weather details such as temperature and humidity.
 * - [weather]: List of weather conditions (e.g., cloudy, rainy).
 * - [name]: The city name for which the weather data is retrieved.
 */
data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String
)

/**
 * Contains the main weather details.
 * @param temp The temperature in degrees Celsius.
 * @param humidity The humidity percentage.
 */
data class Main(
    val temp: Double,
    val humidity: Int
)

/**
 * Represents a specific weather condition.
 * @param description A textual description of the weather condition (e.g., "clear sky").
 * //@param icon The icon ID used to display corresponding weather images.
 */
data class Weather(
    val description: String,
    //val icon: String
)
