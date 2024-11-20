package com.example.healthtrackingapp

import DatabaseHelper
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthtrackingapp.ui.theme.HealthTrackingAppTheme
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast


class settings : ComponentActivity() {
    private var initialPhoneNumber: String = ""  // To store the initial phone number
    private var initialPhoneNumber2: String = "" // to store the initial phone number in a variable that will now change throughout the code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settingslayout)

        // Access the EditText and Button
        val myEditText: EditText = findViewById(R.id.myEditText)
        val enableButton: ImageView = findViewById(R.id.enableButton)
        val saveButton: Button = findViewById(R.id.saveButton)
        val backBtn: ImageView = findViewById(R.id.backArrowImageView)
        var EditFlag = 0
        // Get the phone number from the database and set it in the EditText
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT PhoneNumber FROM EContact LIMIT 1", null)

        if (cursor.moveToFirst()) {
            initialPhoneNumber = cursor.getString(cursor.getColumnIndex("PhoneNumber"))
            initialPhoneNumber2 = cursor.getString(cursor.getColumnIndex("PhoneNumber"))

            myEditText.setText(initialPhoneNumber)  // Set the phone number in the EditText

            // Move the cursor to the end of the EditText text
            myEditText.setSelection(initialPhoneNumber.length)
        }

        cursor.close()  // Always close the cursor when done

        // If the EditText is empty, show the keyboard and allow editing
        if (myEditText.text.isEmpty()) {
            myEditText.isEnabled = true  // Enable the EditText
            myEditText.requestFocus()  // Optional: Focus the EditText
            saveButton.visibility = View.VISIBLE
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(myEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        // Set OnClickListener for the button to enable editing
        enableButton.setOnClickListener {
            EditFlag =1
            myEditText.isEnabled = true  // Enable the EditText
            myEditText.requestFocus()  // Focus the EditText
            saveButton.visibility = View.VISIBLE
            // Show the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(myEditText, InputMethodManager.SHOW_IMPLICIT)

        }

        // Handle save button click
        saveButton.setOnClickListener {
            if (myEditText.text.isNotEmpty() && myEditText.text.toString() != initialPhoneNumber) {
                // Show confirmation dialog before saving the new number
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirm Update")
                    .setMessage("Are you sure you want to update the emergency number?")
                    .setPositiveButton("Yes") { dialog, id ->
                        // Proceed with saving the new phone number
                        val phoneNumber = myEditText.text.toString()

                        // Get a writable database
                        val dbWritable = dbHelper.writableDatabase

                        // Delete existing entry and insert new phone number
                        dbWritable.execSQL("DELETE FROM EContact")
                        val insertQuery = "INSERT INTO EContact (PhoneNumber) VALUES ('$phoneNumber')"
                        dbWritable.execSQL(insertQuery)

                        // Close the database
                        dbWritable.close()

                        // Disable the EditText and hide the save button
                        myEditText.isEnabled = false
                        saveButton.visibility = View.INVISIBLE
                        initialPhoneNumber = myEditText.text.toString()

                        // Show a success Toast
                        Toast.makeText(this, "Emergency Number Updated", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No") { dialog, id ->
                        // Do nothing if "No" is pressed
                        dialog.dismiss()
                    }

                // Show the dialog
                builder.create().show()

            } else {
                // Show a Toast message if the EditText is empty or no change is made
                Toast.makeText(this, "Please enter a new number to save", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle back button click
        backBtn.setOnClickListener {
            // Get the current phone number from the database
            val dbHelper = DatabaseHelper(this)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT PhoneNumber FROM EContact LIMIT 1", null)

            var currentPhoneNumber = ""
            if (cursor.moveToFirst()) {
                currentPhoneNumber = cursor.getString(cursor.getColumnIndex("PhoneNumber"))
            }
            cursor.close()  // Always close the cursor

            // Check if the phone number was changed
            if (EditFlag == 1 && currentPhoneNumber == initialPhoneNumber2) {
                // Show a Toast message if no change was made
                Toast.makeText(this, "Emergency Number has not been changed", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                EditFlag =0

            } else {
                // Proceed with back navigation
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }
}