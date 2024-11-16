package com.example.healthtrackingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import android.widget.TextView


class settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settingslayout)

        // Access the EditText and Button
        val myEditText: EditText = findViewById(R.id.myEditText)
        val enableButton: ImageView = findViewById(R.id.enableButton)
        val saveButton: Button = findViewById(R.id.saveButton)
        val backBtn: ImageView = findViewById(R.id.backArrowImageView)

        // Set OnClickListener for the button
        enableButton.setOnClickListener {
            myEditText.isEnabled = true // Enable the EditText
            myEditText.requestFocus()  // Optional: Focus the EditText

            // Show the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(myEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        saveButton.setOnClickListener{
            myEditText.isEnabled = false
        }

        backBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}

