package com.example.weather_test2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.example.weather_test2.databinding.ActivitySplashScreenBinding;

/**
 * Splash screen activity that displays a full-screen welcome screen before launching the main activity.
 * - This screen is displayed for a few seconds before automatically navigating to `MainActivity`.
 * - Removes the status bar for a full-screen experience.
 */

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    // Splash screen display time in milliseconds
    private static final int SPLASH_TIME = 4000;

    /**
     * Called when the activity is first created.
     * - Sets up the splash screen layout using view binding.
     * - Enables full-screen mode by hiding the status bar.
     * - Initiates the splash screen timer.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Initialize view binding for splash screen layout
            ActivitySplashScreenBinding binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Removing the status bar for a full-screen splash screen experience
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // Start the splash screen timer
            splashScreen();
        } catch (Exception e) {
            Log.e("SplashScreen", "Error in onCreate: " + e.getMessage(), e);
        }
    }

    /**
     * Initiates a delayed transition from the splash screen to the main activity.
     * - Waits for `SPLASH_TIME` milliseconds before starting `MainActivity`.
     * - Ensures a smooth transition using a post-delayed handler.
     */
    private void splashScreen() {
        Log.d("SplashScreen", "Splash screen started, will transition in " + SPLASH_TIME + " ms");

        new Handler().postDelayed(() -> {
            try {
                // Navigating to MainActivity after the splash screen duration
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish(); // Finish this activity so it's not in the back stack
                Log.d("SplashScreen", "Navigating to MainActivity");
            } catch (Exception e) {
                Log.e("SplashScreen", "Error transitioning to MainActivity: " + e.getMessage(), e);
            }
        }, SPLASH_TIME);
    }
}