package com.example.healthtrackingapp

import DatabaseHelper
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.healthtrackingapp.com.example.healthtrackingapp.HealthMonitorService
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var heartRateText: TextView
    private lateinit var spo2Text: TextView
    private lateinit var statusText: TextView
    private lateinit var emergencyCallBtn: Button
    private val REQUEST_CALL_PHONE = 1
    private var lastCallTimestamp: Long = 0
    private var Ebutton = 0

    private val handler = Handler()
    private val fetchDataRunnable = object : Runnable {
        override fun run() {
            fetchRandomReading()
            handler.postDelayed(this, 5000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        // Initialize the UI elements
        heartRateText = findViewById(R.id.heartRateText)
        spo2Text = findViewById(R.id.spo2Text)
        statusText = findViewById(R.id.statusText)
        emergencyCallBtn = findViewById(R.id.EmergencyCallBtn)

        // Retrieve the last call timestamp from SharedPreferences
        val sharedPreferences = getSharedPreferences("HealthTrackingApp", MODE_PRIVATE)
        lastCallTimestamp = sharedPreferences.getLong("lastCallTimestamp", 0)

        // Start the data fetching process
        handler.post(fetchDataRunnable)

        emergencyCallBtn.setOnClickListener {
            Ebutton = 1
            initiateEmergencyCall()
        }

        // History button logic
        val historyBtn: TextView = findViewById(R.id.historyButton)
        historyBtn.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // Settings button logic
        val settingBtn: ImageButton = findViewById(R.id.settingsButton)
        settingBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startHealthMonitorService() {
        val intent = Intent(this, HealthMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopHealthMonitorService() {
        val intent = Intent(this, HealthMonitorService::class.java)
        stopService(intent)
    }

    override fun onStart() {
        super.onStart()
        // Stop the background service when the app is in the foreground
        stopHealthMonitorService()
    }

    override fun onStop() {
        super.onStop()
        // Start the background service when the app goes into the background
        startHealthMonitorService()
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

                    heartRateText.text = hr.toString()
                    spo2Text.text = spo2.toString()

                    if (hr < 60 || hr > 100 || spo2 < 94) {
                        statusText.text = "Abnormal Reading"
                        statusText.setTextColor(ContextCompat.getColor(this, R.color.red))
                        initiateEmergencyCall()
                    } else {
                        statusText.text = "Normal Reading"
                        statusText.setTextColor(ContextCompat.getColor(this, R.color.dark_green))
                    }

                    insertReadingIntoHistory(hr, spo2)
                } else {
                    Log.e("Firebase", "Document does not exist")
                    Toast.makeText(this, "No data found for $randomId", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error fetching document", exception)
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
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
            var callOkay = 0
            if (timeElapsed < 5 * 60 * 1000) {
                val timeRemaining = 5 * 60 * 1000 - timeElapsed
                val minutesLeft = timeRemaining / 1000 / 60
                val secondsLeft = (timeRemaining / 1000) % 60
                Toast.makeText(
                    this,
                    "Next call allowed in $minutesLeft minutes and $secondsLeft seconds.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                callOkay = 1
            }
            if (callOkay == 1 || Ebutton == 1) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = android.net.Uri.parse("tel:$phoneNumber")
                    startActivity(callIntent)

                    // Set last call timestamp on call completion
                    lastCallTimestamp = currentTime
                    val sharedPreferences = getSharedPreferences("HealthTrackingApp", MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putLong("lastCallTimestamp", lastCallTimestamp)
                        apply()
                    }
                    callOkay = 0
                    Ebutton = 0
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CALL_PHONE),
                        REQUEST_CALL_PHONE
                    )
                }
            }
        } else {
            Toast.makeText(this, "No emergency contact number available", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun insertReadingIntoHistory(hr: Int, spo2: Int) {
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val currentStatus = statusText.text.toString()

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
        statement.bindString(4, currentStatus.replace(" Reading", ""))

        statement.executeInsert()
        Log.d("Database", "Inserted data into History: Time = $currentTime, HR = $hr, SPO2 = $spo2, Status = $currentStatus")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(fetchDataRunnable)
    }

    override fun onResume() {
        super.onResume()
        // Start the background service again if needed
        val serviceIntent = Intent(this, HealthMonitorService::class.java)
        stopService(serviceIntent)
    }


}
