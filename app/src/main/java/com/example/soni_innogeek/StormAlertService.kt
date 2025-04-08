package com.example.soni_innogeek

import android.app.*
import android.content.Intent
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

class StormAlertService : Service() {

    private lateinit var databaseReference: DatabaseReference
    private var valueEventListener: ValueEventListener? = null

    companion object {
        private const val TAG = "StormAlertService"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "storm_alert_channel"
        private const val ALERT_PATH = "storm_alert" // Path in Firebase database

        var isRunning = false // To track if the service is running
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        listenForStormAlerts()
        // If the service gets killed, restart it
        return START_STICKY
    }

    private fun listenForStormAlerts() {
        try {
            databaseReference = FirebaseDatabase.getInstance().reference.child(ALERT_PATH)

            valueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stormAlert = snapshot.getValue(Boolean::class.java) ?: false
                    Log.d(TAG, "Storm alert value changed: $stormAlert")

                    if (stormAlert) {
                        sendStormAlertNotification()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                }
            }

            databaseReference.addValueEventListener(valueEventListener!!)
            Log.d(TAG, "Started listening for storm alerts")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up database listener", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Storm Alert Channel"
            val description = "Channel for storm alert notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Storm Alert Service")
            .setContentText("Monitoring for severe weather alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun sendStormAlertNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Storm Alert")
            .setContentText("Severe weather conditions detected in your area")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Severe weather conditions detected in your area. Take necessary precautions and stay safe."))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setLights(Color.RED, 3000, 3000)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use a different ID than the foreground notification
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the database listener to prevent memory leaks
        valueEventListener?.let {
            databaseReference.removeEventListener(it)
        }
        isRunning = false
    }
}