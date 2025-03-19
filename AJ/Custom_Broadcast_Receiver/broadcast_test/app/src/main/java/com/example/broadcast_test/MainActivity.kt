// MainActivity.kt
package com.example.broadcast_test

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var broadcastReceiver: MyBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        statusTextView = findViewById(R.id.statusTextView)

        // Initialize broadcast receiver
        broadcastReceiver = MyBroadcastReceiver()

        // Set click listener for the send button
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                sendCustomBroadcast(message)
                messageEditText.text.clear()
                statusTextView.text = "Broadcast sent: $message"
            }
        }
    }

    private fun sendCustomBroadcast(message: String) {
        // Create intent with the custom action
        val intent = Intent(MyBroadcastReceiver.ACTION_CUSTOM_BROADCAST).apply {
            putExtra("message", message)
        }

        // Send local broadcast (within the app)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        // Also send a global broadcast for ADB to be able to receive it
        sendBroadcast(intent)
    }

    override fun onResume() {
        super.onResume()

        // Register receiver for custom broadcasts - local broadcasts
        val intentFilter = IntentFilter(MyBroadcastReceiver.ACTION_CUSTOM_BROADCAST)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)

        // Also register for global broadcasts (so it works with ADB)
        registerReceiver(broadcastReceiver, intentFilter, RECEIVER_EXPORTED)

        // Register for system broadcasts
        val systemIntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BOOT_COMPLETED)
        }
        registerReceiver(broadcastReceiver, systemIntentFilter, RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()

        try {
            // Unregister the receivers to prevent memory leaks
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
            unregisterReceiver(broadcastReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver might not be registered
            Log.e("MainActivity", "Receiver not registered: ${e.message}")
        }
    }
}