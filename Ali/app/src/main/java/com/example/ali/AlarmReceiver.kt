package com.example.ali

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val serviceIntent = Intent(it, SyncService::class.java)
            it.startService(serviceIntent)
        }
    }
}
