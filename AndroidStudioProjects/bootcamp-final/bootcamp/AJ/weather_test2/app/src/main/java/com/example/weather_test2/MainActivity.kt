package com.example.weather_test2

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weather_test.viewmodel.WeatherViewModel
import com.example.weather_test2.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * MainActivity is the entry point of the Weather Application.
 * Uses FusedLocationClient with IP-based geolocation fallback.
 */
class MainActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val viewModel: WeatherViewModel by viewModels()
    private val apiKey = "87d8b541e3df6b7231276a7a93b4e6e1"
    private lateinit var binding: ActivityMainBinding
    private val searchedCities = mutableSetOf<String>()
    private lateinit var citiesAdapter: ArrayAdapter<String>
    private lateinit var locationManager: LocationManager
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private var useGooglePlayServices = false

    companion object {
        private const val TAG = "MainActivity"
        private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val IP_GEOLOCATION_URL = "https://ipapi.co/json/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tell system you want to handle insets yourself
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Then apply padding based on system bars (navigation/status bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
            )

            // Return the insets as consumed so they're not applied again
            WindowInsetsCompat.CONSUMED
        }
        try {
            Log.d(TAG, "onCreate started")
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Check Google Play Services availability
            checkGooglePlayServices()
            loadCachedCities()
            setupCitySearch()
            setupLocationButton()
            observeWeatherUpdates()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                checkLocationPermissionAndGetWeather()
            } else {
                Log.d(TAG, "Couldn't fetch current location")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode == ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Services is available")
            useGooglePlayServices = true
        } else {
            Log.w(
                TAG,
                "Google Play Services unavailable (code: $resultCode), will use IP-based geolocation fallback"
            )
            useGooglePlayServices = false

            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                Log.d(TAG, "Google Play Services error is resolvable")
                // We could show dialog to fix: googleApiAvailability.getErrorDialog(this, resultCode, 1000)
            }
        }
    }

    private fun loadCachedCities() {
        Log.d(TAG, "Loading cached cities")
        val cachedCities = getSharedPreferences("weather_prefs", MODE_PRIVATE)
            .getStringSet("searched_cities", emptySet()) ?: emptySet()
        searchedCities.addAll(cachedCities)
        Log.d(TAG, "Loaded ${searchedCities.size} cached cities")

        citiesAdapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line,
            searchedCities.toList()
        )
        binding.citySearch.setAdapter(citiesAdapter)
    }

    private fun setupCitySearch() {
        // Handle dropdown selection
        binding.citySearch.setOnItemClickListener { _, _, position, _ ->
            citiesAdapter.getItem(position)?.let { city ->
                Log.d(TAG, "Selected city from dropdown: $city")
                viewModel.fetchWeather(city, apiKey)
            }
        }
        // Handle search action
        binding.citySearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val city = binding.citySearch.text.toString().trim()
                if (city.isNotEmpty()) {
                    Log.d(TAG, "Searching for city: $city")
                    addCityToSearchHistory(city)
                    viewModel.fetchWeather(city, apiKey)
                } else {
                    Log.d(TAG, "Empty city search ignored")
                }
                true
            } else false
        }
    }

    private fun setupLocationButton() {
        binding.btngetcurrentlocation.setOnClickListener {
            Log.d(TAG, "Get Current Location button clicked")
            getCurrentLocationAndFetchWeather()
        }
        binding.btnSearchLocation.setOnClickListener {
            Log.d(TAG, "Search Location button clicked")
            val city = binding.citySearch.text.toString().trim()
            if (city.isNotEmpty()) {
                Log.d(TAG, "Searching for city via button: $city")
                addCityToSearchHistory(city)
                viewModel.fetchWeather(city, apiKey)
            } else {
                Log.d(TAG, "Empty city search from button ignored")
            }
        }
    }

    private fun addCityToSearchHistory(city: String) {
        if (searchedCities.add(city)) {
            Log.d(TAG, "Added '$city' to search history")
            citiesAdapter.clear()
            citiesAdapter.addAll(searchedCities.toList())
            citiesAdapter.notifyDataSetChanged()

            // Save to preferences
            getSharedPreferences("weather_prefs", MODE_PRIVATE).edit()
                .putStringSet("searched_cities", searchedCities)
                .apply()
        }
    }

    private fun observeWeatherUpdates() {
        viewModel.weatherData.observe(this) { weather ->
            try {
                Log.d(TAG, "Weather data received: ${weather.name}, ${weather.main.temp}°C")
                binding.txtCity.text = weather.name
                binding.txtTemperature.text = "${weather.main.temp}°C"
                binding.txtCondition.text = weather.weather[0].description.capitalize()
                binding.txtHumidity.text = "Humidity: ${weather.main.humidity}%"
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UI with weather data: ${e.message}", e)
            }
        }
    }

    private fun checkLocationPermissionAndGetWeather() {
        Log.d(TAG, "Checking location permission")
        if (useGooglePlayServices) {
            // Need permission for FusedLocationClient
            when {
                hasLocationPermission() -> {
                    Log.d(TAG, "Location permission already granted")
                    getCurrentLocationAndFetchWeather()
                }

                shouldShowRequestPermissionRationale(LOCATION_PERMISSION) -> {
                    Log.d(TAG, "Should show permission rationale")
                    Toast.makeText(
                        this, "Location permission needed for local weather",
                        Toast.LENGTH_SHORT
                    ).show()
                    locationPermissionRequest.launch(LOCATION_PERMISSION)
                }

                else -> {
                    Log.d(TAG, "Requesting location permission")
                    locationPermissionRequest.launch(LOCATION_PERMISSION)
                }
            }
        } else {
            getCurrentLocationAndFetchWeather()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun getCurrentLocationAndFetchWeather() {
        activityScope.launch {
            Log.d(TAG, "Starting location retrieval process")
            try {
                showLoadingState(true)

                val location = if (useGooglePlayServices && hasLocationPermission()) {
                    Log.d(TAG, "Attempting to get location via Google Play Services")
                    try {
                        getLocationViaFusedClient()
                    } catch (e: Exception) {
                        Log.w(
                            TAG,
                            "FusedLocationProvider failed: ${e.message}, trying LocationManager"
                        )
                        getLocationViaLocationManager() ?: run {
                            Log.w(TAG, "LocationManager also failed, falling back to IP")
                            getLocationViaIp()
                        }
                    }
                } else {
                    Log.d(
                        TAG,
                        "Google Play Services not available or permission denied, trying LocationManager"
                    )
                    getLocationViaLocationManager() ?: run {
                        Log.w(TAG, "LocationManager failed, falling back to IP")
                        getLocationViaIp()
                    }
                }

                location?.let {
                    Log.d(TAG, "Location obtained: ${it.latitude}, ${it.longitude}")
                    viewModel.fetchWeatherByCoordinates(it.latitude, it.longitude, apiKey)
                } ?: run {
                    Log.e(TAG, "Failed to get location from all methods")
                    Toast.makeText(
                        this@MainActivity,
                        "Unable to determine location. Please check settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during location retrieval: ${e.message}", e)
                Toast.makeText(
                    this@MainActivity,
                    "Location error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoadingState(false)
            }
        }
    }

    private fun showLoadingState(isLoading: Boolean) {
        // TODO: Add loading indicator if needed
        // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private suspend fun getLocationViaFusedClient(): Location? = withContext(Dispatchers.IO) {
        try {
            suspendCancellableCoroutine { cont ->
                val cancellationToken = CancellationTokenSource()
                Log.d(TAG, "Requesting location from FusedLocationClient")

                if (ActivityCompat.checkSelfPermission(this@MainActivity, LOCATION_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e(TAG, "Permission missing for FusedLocationClient")
                    cont.resume(null) { }
                    return@suspendCancellableCoroutine
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(
                            TAG,
                            "FusedLocationClient success: ${location.latitude}, ${location.longitude}"
                        )
                        cont.resume(location) { }
                    } else {
                        Log.w(TAG, "FusedLocationClient returned null location")
                        cont.resume(null) { }
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "FusedLocationClient error: ${e.message}")
                    cont.resume(null) { }
                }

                cont.invokeOnCancellation {
                    Log.d(TAG, "FusedLocationClient request cancelled")
                    cancellationToken.cancel()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in FusedLocationClient: ${e.message}", e)
            null
        }
    }

    private suspend fun getLocationViaLocationManager(): Location? = withContext(Dispatchers.IO) {
        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            Log.d(TAG, "LocationManager providers: GPS=$isGpsEnabled, Network=$isNetworkEnabled")

            if (!isGpsEnabled && !isNetworkEnabled) {
                Log.w(TAG, "No location provider available")
                return@withContext null
            }

            val provider = when {
                isGpsEnabled -> {
                    Log.d(TAG, "Using GPS provider")
                    LocationManager.GPS_PROVIDER
                }

                else -> {
                    Log.d(TAG, "Using Network provider")
                    LocationManager.NETWORK_PROVIDER
                }
            }

            return@withContext suspendCancellableCoroutine { cont ->
                if (ActivityCompat.checkSelfPermission(this@MainActivity, LOCATION_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e(TAG, "Permission missing for LocationManager")
                    cont.resume(null) { }
                    return@suspendCancellableCoroutine
                }

                try {
                    // Try to get last known location first for immediate response
                    locationManager.getLastKnownLocation(provider)?.let { lastLocation ->
                        Log.d(
                            TAG,
                            "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}"
                        )
                        // Only use if recent (within last 10 minutes)
                        if (System.currentTimeMillis() - lastLocation.time < 10 * 60 * 1000) {
                            cont.resume(lastLocation) { }
                            return@suspendCancellableCoroutine
                        } else {
                            Log.d(TAG, "Last known location too old, requesting fresh location")
                        }
                    }

                    // Request fresh location update
                    Log.d(TAG, "Requesting location update from $provider")
                    locationManager.requestSingleUpdate(provider, object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            Log.d(
                                TAG,
                                "LocationManager onLocationChanged: ${location.latitude}, ${location.longitude}"
                            )
                            cont.resume(location) { }
                        }

                        override fun onProviderDisabled(provider: String) {
                            Log.w(TAG, "Provider disabled: $provider")
                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {
                            Log.d(TAG, "Provider status changed: $provider, status=$status")
                        }
                    }, Looper.getMainLooper())

                    // Set timeout for location request (10 seconds)
                    activityScope.launch {
                        delay(10000)
                        if (cont.isActive) {
                            Log.w(TAG, "Location request timed out")
                            cont.resume(null) { }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting location updates: ${e.message}", e)
                    cont.resume(null) { }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationManager: ${e.message}", e)
            null
        }
    }

    private suspend fun getLocationViaIp(): Location? = withContext(Dispatchers.IO) {
        val apiKey = "f9301a79ab3a71"
        val urlString = "https://ipinfo.io/json?token=$apiKey"

        try {
            Log.d(TAG, "Fetching location from ipinfo.io service")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code from ipinfo.io: $responseCode")

            if (responseCode !in 200..299) {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Error response from ipinfo.io: HTTP $responseCode - $errorMsg")
                connection.disconnect()
                return@withContext null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val jsonObject = JSONObject(response)
            val city = jsonObject.optString("city", "Unknown")

            val locString = jsonObject.optString("loc")
            if (locString.isNullOrEmpty() || !locString.contains(",")) {
                Log.e(TAG, "Invalid location format in ipinfo.io response: $locString")
                return@withContext null
            }

            val (latitudeStr, longitudeStr) = locString.split(",", limit = 2)
            val latitude = latitudeStr.toDoubleOrNull()
            val longitude = longitudeStr.toDoubleOrNull()

            if (latitude == null || longitude == null) {
                Log.e(TAG, "Failed to parse latitude or longitude from loc string: $locString")
                return@withContext null
            }

            Log.d(TAG, "ipinfo.io geolocation success: $city ($latitude, $longitude)")

            Location("IpInfo").apply {
                this.latitude = latitude
                this.longitude = longitude
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception while getting location from ipinfo.io: ${e.message}", e)
            null
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Location permission granted")
            getCurrentLocationAndFetchWeather()
        } else {
            Log.w(TAG, "Location permission denied")
            if (!shouldShowRequestPermissionRationale(LOCATION_PERMISSION)) {
                Log.d(TAG, "Permission denied permanently, showing settings dialog")
                showSettingsDialog()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()

                // Fall back to IP-based location
                Log.d(TAG, "Falling back to IP-based location after permission denial")
                getCurrentLocationAndFetchWeather()
            }
        }
    }

    private fun showSettingsDialog() {
        Log.d(TAG, "Showing settings dialog")
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Enable location in app settings for accurate local weather data")
            .setPositiveButton("Settings") { _, _ ->
                Log.d(TAG, "Opening app settings")
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .setNegativeButton("Use IP Location") { _, _ ->
                Log.d(TAG, "User selected to use IP location")
                getCurrentLocationAndFetchWeather()
            }
            .show()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        activityScope.cancel()
        super.onDestroy()

    }
}