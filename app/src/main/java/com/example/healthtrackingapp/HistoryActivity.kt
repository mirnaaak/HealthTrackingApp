package com.example.healthtrackingapp

import DatabaseHelper
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

class HistoryActivity : ComponentActivity() {
    private val calendar = Calendar.getInstance()
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var historyTable: TableLayout
    private lateinit var filterBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var fromDate: EditText
    private lateinit var toDate: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_layout)

        val backBtn: ImageView = findViewById(R.id.backArrowImageView)


        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        fromDate = findViewById(R.id.fromDate)
        fromDate.setOnClickListener {
            showDatePicker(fromDate)
        }

        toDate = findViewById(R.id.toDate)
        toDate.setOnClickListener {
            showDatePicker(toDate)
        }

        dbHelper = DatabaseHelper(this)
        historyTable = findViewById(R.id.historyTable)
        populateTable(dbHelper.getAllHistories())

        filterBtn = findViewById(R.id.filterBtn)
        filterBtn.setOnClickListener {
            if (fromDate.text.isEmpty() && toDate.text.isEmpty()) {
                Toast.makeText(this, "Please enter at least one of the dates", Toast.LENGTH_SHORT).show()
            } else if (fromDate.text.isNotEmpty() && toDate.text.isEmpty()) {
                populateTable(
                    dbHelper.filterFromHistories(
                        databaseFormatFromDateString(fromDate.text.toString())
                    )
                )
            } else if (fromDate.text.isEmpty() && toDate.text.isNotEmpty()) {
                populateTable(
                    dbHelper.filterToHistories(
                        databaseFormatToDateString(toDate.text.toString())
                    )
                )
            } else {
                if (fromDate.text.toString() == toDate.text.toString() || isFromBeforeTo(
                        fromDate.text.toString(),
                        toDate.text.toString()
                    )
                ) {
                    populateTable(
                        dbHelper.filterHistories(
                            databaseFormatFromDateString(fromDate.text.toString()),
                            databaseFormatToDateString(toDate.text.toString())
                        )
                    )
                } else {
                    Toast.makeText(
                        this,
                        "Please enter dates in the correct order",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }

        resetBtn = findViewById(R.id.resetBtn)
        resetBtn.setOnClickListener {
            fromDate.setText("")
            toDate.setText("")
            populateTable(dbHelper.getAllHistories())
        }

    }

    private fun showDatePicker(item: EditText) {
        DatePickerDialog(
            this, { DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                val selectedDate: Calendar = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate: String = dateFormat.format(selectedDate.time)
                item.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun removeRowsExceptHeader() {
        // Check if the TableLayout has more than one child (header + rows)
        if (historyTable.childCount > 1) {
            // Loop through the TableLayout, starting from the second child (index 1)
            for (i in 1 until historyTable.childCount) {
                historyTable.removeViewAt(1)  // Always remove the second child (which is the first row after the header)
            }
        }
    }

    private fun populateTable(historyList: List<History>) {
        removeRowsExceptHeader()

        // Loop through the history list and add rows
        for (history in historyList) {
            // Create a new row
            val tableRow = TableRow(this)
            tableRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            tableRow.setBackgroundColor(resources.getColor(R.color.white))
            tableRow.gravity = Gravity.CENTER
            tableRow.setPadding(5, 5, 5, 5)

            // Create TextViews for each column
            val tvDate = createTextView(displayFormatDateString(history.date))
            val tvHR = createTextView(history.hr.toString())
            val tvSPO2 = createTextView(history.spo2.toString())
            val tvStatus = createTextView(history.status)

            tvStatus.setTextColor(resources.getColor(R.color.white))
            tvStatus.setPadding(10, 5, 10, 5)
            tvStatus.textSize = 15f
            if (history.status == "Normal") {
                tvStatus.setBackgroundColor(resources.getColor(R.color.dark_green))
            } else {
                tvStatus.setBackgroundColor(resources.getColor(R.color.red))
            }

            // Add TextViews to the row
            tableRow.addView(tvDate)
            tableRow.addView(tvHR)
            tableRow.addView(tvSPO2)
            tableRow.addView(tvStatus)

            // Add the row to the TableLayout
            historyTable.addView(tableRow)
        }
    }

    // Helper function to create TextView for a table cell
    private fun createTextView(text: String): TextView {
        val textView = TextView(this)
        textView.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )
        textView.text = text
        textView.textSize = 16f
        textView.gravity = Gravity.CENTER
        textView.setTextColor(resources.getColor(R.color.black))
        return textView
    }

    private fun databaseFormatToDateString(dateString: String): String {
        // Define the input format (dd/MM/yyyy)
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        // Parse the input date string
        val date = inputFormat.parse(dateString)

        // Use Calendar to set the time to 23:59:59
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        // Define the output format (yyyy-MM-dd HH:mm:ss)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        // Format the calendar's time into the new format
        return outputFormat.format(calendar.time)
    }

    private fun databaseFormatFromDateString(dateString: String): String {
        // Define the input format (dd/MM/yyyy)
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        // Parse the input date string
        val date = inputFormat.parse(dateString)

        // Define the output format (yyyy-MM-dd HH:mm:ss)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        // Format the date into the new format
        return outputFormat.format(date)
    }

    private fun displayFormatDateString(dateString: String): String {
        // Define the input and output date formats
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)

        // Parse the input date string
        val date = inputFormat.parse(dateString)

        // Format the date into the new format
        return outputFormat.format(date)
    }

    private fun isFromBeforeTo(fromDate: String, toDate: String): Boolean {
        // Define the date format
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        // Parse the strings into Date objects
        val date1: Date = format.parse(fromDate)!!
        val date2: Date = format.parse(toDate)!!

        // Compare the dates
        return date1.before(date2)
    }
}