package com.example.healthtrackingapp

import DatabaseHelper
import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.provider.CallLog
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
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Handler

class MainActivity : ComponentActivity() {

    // Firebase instance
    private val db = FirebaseFirestore.getInstance()

    // Define variables for the UI elements
    private lateinit var heartRateText: TextView
    private lateinit var spo2Text: TextView

    private val REQUEST_CALL_PHONE = 1  // Request code for CALL_PHONE permission

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
        setContentView(R.layout.layout1)

        // Initialize the UI elements for health data
        heartRateText = findViewById(R.id.heartRateText)
        spo2Text = findViewById(R.id.spo2Text)

        // Start the data fetching process
        handler.post(fetchDataRunnable)

        // History button logic
        val historyBtn: TextView = findViewById(R.id.historyButton)
        historyBtn.setOnClickListener {
            val intent = Intent(this, history::class.java)
            startActivity(intent)
        }

        // Settings button logic
        val settingBtn: ImageButton = findViewById(R.id.settingsButton)
        settingBtn.setOnClickListener {
            val intent = Intent(this, settings::class.java)
            startActivity(intent)
        }

        // Emergency Call button logic
        val emergencyCallBtn: Button = findViewById(R.id.EmergencyCallBtn)
        emergencyCallBtn.setOnClickListener {
            // Check if the CALL_PHONE permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                // Retrieve the phone number from the EContact table
                val dbHelper = DatabaseHelper(this)
                val db = dbHelper.readableDatabase

                val cursor = db.rawQuery("SELECT PhoneNumber FROM EContact LIMIT 1", null)
                var phoneNumber = ""

                if (cursor.moveToFirst()) {
                    phoneNumber = cursor.getString(cursor.getColumnIndex("PhoneNumber"))
                }
                cursor.close()

                // Check if a phone number is available
                if (phoneNumber.isNotEmpty()) {
                    // Create an intent to call the phone number
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = android.net.Uri.parse("tel:$phoneNumber")
                    startActivity(callIntent)
                } else {
                    // Show a toast if the phone number is not found
                    Toast.makeText(this, "No emergency contact number available", Toast.LENGTH_LONG).show()
                }
            } else {
                // Request the CALL_PHONE permission
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
            }
        }
    }

    private fun fetchRandomReading() {
        // List of document IDs
        val documentIds = listOf("reading1", "reading2", "reading3", "reading4", "reading5", "reading6")
        val randomId = documentIds[Random.nextInt(documentIds.size)] // Pick a random ID

        // Fetch the document from Firestore
        db.collection("liveData").document(randomId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Retrieve fields from the document
                    val hr = document.getString("HR") ?: "N/A"
                    val spo2 = document.getString("SPO2") ?: "N/A"

                    // Update the UI with the fetched values
                    heartRateText.text = hr
                    spo2Text.text = spo2

                    // Notify user which reading was fetched
                    Log.d("Firebase", "Data fetched from $randomId: HR = $hr, SPO2 = $spo2")

                    // Insert the fetched data into the local database (History table)
                    insertReadingIntoHistory(hr.toIntOrNull() ?: 0, spo2.toIntOrNull() ?: 0)
                } else {
                    Log.e("Firebase", "Document does not exist")
                    Toast.makeText(this, "No data found for $randomId", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                // Log and notify on error
                Log.e("Firebase", "Error fetching document", exception)
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun insertReadingIntoHistory(hr: Int, spo2: Int) {
        // Get the current time in the format 'yyyy-MM-dd HH:mm:ss'
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        // Get the writable database
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.writableDatabase

        // Insert the data into the History table
        val insertQuery = """
            INSERT INTO History (Date, HR, SPO2) 
            VALUES (?, ?, ?)
        """
        val statement = db.compileStatement(insertQuery)
        statement.bindString(1, currentTime)  // Bind the current time
        statement.bindLong(2, hr.toLong())    // Bind the heart rate
        statement.bindLong(3, spo2.toLong())  // Bind the SPO2 level

        // Execute the insert
        statement.executeInsert()
        Log.d("Database", "Inserted data into History: Time = $currentTime, HR = $hr, SPO2 = $spo2")
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry calling the emergency number
                val dbHelper = DatabaseHelper(this)
                val db = dbHelper.readableDatabase

                val cursor = db.rawQuery("SELECT PhoneNumber FROM EContact LIMIT 1", null)
                var phoneNumber = ""

                if (cursor.moveToFirst()) {
                    phoneNumber = cursor.getString(cursor.getColumnIndex("PhoneNumber"))
                }
                cursor.close()

                if (phoneNumber.isNotEmpty()) {
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = android.net.Uri.parse("tel:$phoneNumber")
                    startActivity(callIntent)
                } else {
                    Toast.makeText(this, "No emergency contact number available", Toast.LENGTH_LONG).show()
                }
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Permission Denied. Can't make the emergency call.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the handler callback to avoid memory leaks
        handler.removeCallbacks(fetchDataRunnable)
    }
}
