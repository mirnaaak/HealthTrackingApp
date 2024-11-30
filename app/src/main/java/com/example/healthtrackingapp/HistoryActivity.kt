package com.example.healthtrackingapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthtrackingapp.ui.theme.HealthTrackingAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryActivity : ComponentActivity() {
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_layout)

        val backBtn: ImageView = findViewById(R.id.backArrowImageView)


        backBtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val fromDate: EditText =findViewById(R.id.fromDate)
        fromDate.setOnClickListener{
            showDatePicker(fromDate)
        }

        val toDate: EditText =findViewById(R.id.toDate)
        toDate.setOnClickListener{
            showDatePicker(toDate)
        }
    }

    private fun showDatePicker(item:EditText) {
        val datePicker= DatePickerDialog(this,{DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
            val selectedDate:Calendar=Calendar.getInstance()
            selectedDate.set(year,monthOfYear,dayOfMonth)
            val dateFormat=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate:String=dateFormat.format(selectedDate.time)
            item.setText(formattedDate)
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}