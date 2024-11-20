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

class MainActivity : ComponentActivity() {

    private val REQUEST_CALL_PHONE = 1  // Request code for CALL_PHONE permission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout1)

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

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry calling the emergency number
                // You can call the emergency number here directly after granting permission
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
}
