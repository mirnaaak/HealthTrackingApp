package com.example.healthtrackingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout1)

        val historyBtn: TextView = findViewById(R.id.historyButton)
        historyBtn.setOnClickListener{
            val intent = Intent(this, history::class.java)
            startActivity(intent)
        }

        val settingBtn: ImageButton = findViewById(R.id.settingsButton)
        settingBtn.setOnClickListener{
            val intent = Intent(this, settings::class.java)
            startActivity(intent)
        }
    }
}

