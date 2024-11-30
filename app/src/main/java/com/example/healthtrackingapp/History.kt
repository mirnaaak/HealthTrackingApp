package com.example.healthtrackingapp

data class History(
    var date: String = "",
    var hr: Int = 0,
    var spo2: Int = 0,
    var status: String = ""
)