// MainActivity.kt (Broadcast Receiver)
package com.example.broadcastreceiver

import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    // Define the same action string as in the sender app
    companion object {
        const val CUSTOM_BROADCAST_ACTION = "com.example.CUSTOM_BROADCAST"
    }

    private lateinit var statusTextView: TextView
    private lateinit var receiver: CustomBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        statusTextView = findViewById(R.id.statusTextView)

        // Initialize the receiver
        receiver = CustomBroadcastReceiver(statusTextView)

        // Register the receiver with the specified action
        val filter = IntentFilter(CUSTOM_BROADCAST_ACTION)
        registerReceiver(receiver, filter)

        statusTextView.text = "Broadcast receiver registered. Waiting for broadcasts..."
    }


    override fun onDestroy() {
        super.onDestroy()

        // Unregister the receiver when the activity is no longer visible
        unregisterReceiver(receiver)
    }
}