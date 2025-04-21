package com.example.soni_innogeek

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class StormAlertService : Service() {
    private lateinit var database: FirebaseDatabase
    private lateinit var stormRef: DatabaseReference
    private var stormListener: ValueEventListener? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        setupStormListener()
    }

    private fun startForegroundService() {
        val channelId = createNotificationChannel("service_channel", "Storm Monitoring", NotificationManager.IMPORTANCE_LOW)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Storm Monitoring Active")
            .setContentText("Watching for weather changes")
            .setSmallIcon(R.drawable.baseline_add_alert_24)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(SERVICE_ID, notification)
        }
    }

    private fun setupStormListener() {
        database = FirebaseDatabase.getInstance()
        stormRef = database.getReference("storm_alert")

        stormListener = object : ValueEventListener {
            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onDataChange(snapshot: DataSnapshot) {
                val isStormActive = snapshot.getValue(Boolean::class.java) ?: false
                if (isStormActive) {
                    showStormAlertNotification()
                    // Reset after notification
                    stormRef.setValue(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StormAlert", "Firebase error: ${error.message}")
            }
        }

        stormRef.addValueEventListener(stormListener!!)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showStormAlertNotification() {
        val channelId = createNotificationChannel("alert_channel", "Storm Alerts", NotificationManager.IMPORTANCE_HIGH)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⛈️ Storm Detected!")
            .setContentText("Dangerous weather conditions imminent!")
            .setSmallIcon(R.drawable.baseline_add_alert_24)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(ALERT_ID, notification)
    }

    private fun createNotificationChannel(channelId: String, name: String, importance: Int): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(channelId, name, importance).apply {
                description = "Storm monitoring service"
                enableVibration(true)
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(this)
            }
        }
        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        stormListener?.let {
            stormRef.removeEventListener(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val SERVICE_ID = 1753
        const val ALERT_ID = 1754
    }
}