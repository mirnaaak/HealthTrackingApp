package com.example.healthtrackingapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random
import com.example.healthtrackingapp.R
import DatabaseHelper

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

        // Start periodic data fetching
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

        startForeground(1, notification)
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
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            val callState = telephonyManager.callState

            if (callState == android.telephony.TelephonyManager.CALL_STATE_IDLE) {
                val currentTime = System.currentTimeMillis()
                val timeElapsed = currentTime - lastCallTimestamp

                if (timeElapsed >= 5 * 60 * 1000) { // Minimum 5-minute gap between calls
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        val callIntent = Intent(Intent.ACTION_CALL)
                        callIntent.data = android.net.Uri.parse("tel:$phoneNumber")
                        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for starting activity from service
                        startActivity(callIntent)

                        // Save timestamp
                        lastCallTimestamp = currentTime
                        sharedPreferences.edit().putLong("lastCallTimestamp", lastCallTimestamp).apply()
                        Log.d("Service", "Emergency call initiated.")
                    } else {
                        Log.e("Service", "CALL_PHONE permission not granted.")
                    }
                } else {
                    Log.d(
                        "Service",
                        "Emergency call skipped. Wait ${(5 * 60 * 1000 - timeElapsed) / 1000} seconds more."
                    )
                }
            } else {
                Log.d("Service", "Call not initiated. Phone is already in use.")
            }
        } else {
            Log.e("Service", "No emergency contact number available.")
        }
    }


    private fun insertReadingIntoHistory(hr: Int, spo2: Int, status: String) {
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        // Check if the date already exists
        val checkQuery = "SELECT COUNT(*) FROM History WHERE Date = ?"
        val checkStatement = db.compileStatement(checkQuery)
        checkStatement.bindString(1, currentTime)
        val count = checkStatement.simpleQueryForLong()

        if (count == 0L) { // If no record exists for this date, insert the new reading
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
        } else {
            Log.d("Database", "Reading not inserted. Entry for Date = $currentTime already exists.")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(fetchDataRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
