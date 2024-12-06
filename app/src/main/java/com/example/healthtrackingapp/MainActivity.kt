package com.example.healthtrackingapp

import DatabaseHelper
import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random
import android.util.Log
import android.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Handler

class MainActivity : ComponentActivity() {

    // Firebase instance
    private val db = FirebaseFirestore.getInstance()

    // Define variables for the UI elements
    private lateinit var heartRateText: TextView
    private lateinit var spo2Text: TextView
    private lateinit var statusText: TextView
    private lateinit var emergencyCallBtn: Button

    private val REQUEST_CALL_PHONE = 1  // Request code for CALL_PHONE permission

    private var isCallInProgress = false  // Flag to track ongoing emergency calls

    // Handler to run the fetch every 5 seconds
    private val handler = Handler()
    private val fetchDataRunnable = object : Runnable {
        override fun run() {
            fetchRandomReading() // Fetch data from Firebase
            handler.postDelayed(this, 5000) // Re-run this task every 5 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        // Initialize the UI elements for health data
        heartRateText = findViewById(R.id.heartRateText)
        spo2Text = findViewById(R.id.spo2Text)
        statusText = findViewById(R.id.statusText)
        emergencyCallBtn = findViewById(R.id.EmergencyCallBtn)

        // Start the data fetching process
        handler.post(fetchDataRunnable)

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

        // Emergency Call button logic
        emergencyCallBtn.setOnClickListener {
            if (!isCallInProgress) {
                initiateEmergencyCall()
            }
        }
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
                        if (!isCallInProgress) {
                            initiateEmergencyCall()
                        }
                    } else {
                        statusText.text = "Normal Reading"
                        statusText.setTextColor(ContextCompat.getColor(this, R.color.dark_green))
                        isCallInProgress = false
                        emergencyCallBtn.isEnabled = true // Re-enable the button when readings are normal
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = android.net.Uri.parse("tel:$phoneNumber")
                startActivity(callIntent)
                isCallInProgress = true
                emergencyCallBtn.isEnabled = false // Disable the button during the call
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
            }
        } else {
            Toast.makeText(this, "No emergency contact number available", Toast.LENGTH_LONG).show()
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
}

