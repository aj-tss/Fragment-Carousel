package com.example.broadcast_test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MyBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "MyBroadcastReceiver"
        const val ACTION_CUSTOM_BROADCAST = "com.example.broadcast_test.ACTION_CUSTOM_BROADCAST"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CUSTOM_BROADCAST -> {
                val message = intent.getStringExtra("message") ?: "No message provided"
                Log.d(TAG, "Received broadcast: $message")

                // Show a toast message for broadcasts received via ADB
                Toast.makeText(context, "ADB Broadcast received: $message", Toast.LENGTH_LONG).show()
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Boot completed")
                Toast.makeText(context, "Device booted", Toast.LENGTH_SHORT).show()
            }
            Intent.ACTION_BATTERY_LOW -> {
                Log.d(TAG, "Battery is low")
                Toast.makeText(context, "Battery is low!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}