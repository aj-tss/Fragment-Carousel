package com.example.calc_app9// Replace with your actual package name

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION = 2000L // 2 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide the action bar if it exists
        supportActionBar?.hide()

        // Handler to delay the start of the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            // Start the main activity after the delay
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Close the splash activity
        }, SPLASH_DURATION)
    }
}