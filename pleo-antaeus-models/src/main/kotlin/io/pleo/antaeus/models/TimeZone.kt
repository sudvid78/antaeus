package io.pleo.antaeus.models

import java.util.TimeZone

data class TimeZone(
        val id: Int,
        val timeZone: TimeZone,
        val processday: Int,
        val reminderday: Int
)
