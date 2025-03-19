// CustomBroadcastReceiver.kt
package com.example.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.TextView
import android.widget.Toast

class CustomBroadcastReceiver(private val statusTextView: TextView) : BroadcastReceiver() {
    companion object {
        // The key for the extra data in the Intent
        const val EXTRA_MESSAGE = "com.example.EXTRA_MESSAGE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "com.example.CUSTOM_BROADCAST") {
            // Extract the message from the Intent
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Broadcast received (no message)"
            Log.d("AJ", "onReceive: ");
            // Display a toast message
            Toast.makeText(context, "Received: $message", Toast.LENGTH_LONG).show()

            // Update the status text view
            statusTextView.text = "Last received message: $message"
        }
    }
}
