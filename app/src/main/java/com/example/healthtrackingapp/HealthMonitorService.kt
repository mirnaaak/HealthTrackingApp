package com.example.healthtrackingapp.com.example.healthtrackingapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import DatabaseHelper
import com.example.healthtrackingapp.R

class HealthMonitorService : Service() {

    private val db = FirebaseFirestore.getInstance()
    private val handler = Handler()
    private lateinit var sharedPreferences: SharedPreferences

    private var lastCallTimestamp: Long = 0

    private val fetchDataRunnable = object : Runnable {
        override fun run() {
            fetchRandomReading() // Fetch data
            handler.postDelayed(this, 5000) // Re-run every 5 seconds
        }
    }

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = getSharedPreferences("HealthTrackingApp", MODE_PRIVATE)
        lastCallTimestamp = sharedPreferences.getLong("lastCallTimestamp", 0)

        // Set up the foreground service notification
        startForegroundServiceWithNotification()

        // Start the periodic data fetching
        handler.post(fetchDataRunnable)
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "HealthMonitorServiceChannel"
        val channelName = "Health Monitor Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Health Monitoring Service")
            .setContentText("Monitoring health data in the background.")
            .setSmallIcon(R.drawable.rounded_save_button) // Replace with your app's icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification) // This ensures your service runs in the foreground
    }

    private fun fetchRandomReading() {
        val documentIds = listOf("reading1", "reading2", "reading3", "reading4", "reading5", "reading6")
        val randomId = documentIds[Random.nextInt(documentIds.size)]

        db.collection("liveData").document(randomId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val hr = document.getString("HR")?.toIntOrNull() ?: 0
                    val spo2 = document.getString("SPO2")?.toIntOrNull() ?: 0

                    val status = if (hr < 60 || hr > 100 || spo2 < 94) {
                        initiateEmergencyCall()
                        "Abnormal"
                    } else {
                        "Normal"
                    }

                    insertReadingIntoHistory(hr, spo2, status)
                } else {
                    Log.e("Firebase", "Document does not exist")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error fetching document", exception)
            }
    }

    private fun initiateEmergencyCall() {
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT PhoneNumber FROM EContact LIMIT 1", null)
        var phoneNumber = ""

        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndex("PhoneNumber"))
        }
        cursor.close()

        if (phoneNumber.isNotEmpty()) {
            val currentTime = System.currentTimeMillis()
            val timeElapsed = currentTime - lastCallTimestamp

            if (timeElapsed >= 5 * 60 * 1000) { // Check 5-minute interval
                // Add emergency call logic here
                // ...

                // Save the timestamp of this call
                lastCallTimestamp = currentTime
                with(sharedPreferences.edit()) {
                    putLong("lastCallTimestamp", lastCallTimestamp)
                    apply()
                }
                Log.d("Service", "Emergency call initiated.")
            } else {
                val timeRemaining = 5 * 60 * 1000 - timeElapsed
                Log.d(
                    "Service",
                    "Emergency call skipped. Wait ${timeRemaining / 1000} seconds more."
                )
            }
        } else {
            Toast.makeText(this, "No emergency contact number available", Toast.LENGTH_LONG).show()
        }
    }

    private fun insertReadingIntoHistory(hr: Int, spo2: Int, status: String) {
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        val insertQuery = """
            INSERT INTO History (Date, HR, SPO2, Status) 
            VALUES (?, ?, ?, ?)
        """
        val statement = db.compileStatement(insertQuery)
        statement.bindString(1, currentTime)
        statement.bindLong(2, hr.toLong())
        statement.bindLong(3, spo2.toLong())
        statement.bindString(4, status)

        statement.executeInsert()
        Log.d("Database", "Inserted data: Time = $currentTime, HR = $hr, SPO2 = $spo2, Status = $status")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(fetchDataRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
