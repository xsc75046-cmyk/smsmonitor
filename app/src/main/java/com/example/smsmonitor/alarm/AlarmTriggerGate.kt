package com.example.smsmonitor.alarm

import android.content.Context

/** Prevents the SMS broadcast and notification listener from scheduling the
 * same alarm twice when both channels report one incoming message. */
object AlarmTriggerGate {

    private const val PREFS = "sms_monitor_alarm_gate"
    private const val KEY_SENDER = "last_sender"
    private const val KEY_BODY = "last_body"
    private const val KEY_TIME = "last_time"
    private const val DEDUPE_WINDOW_MS = 30_000L

    @Synchronized
    fun shouldSchedule(context: Context, source: String, sender: String, body: String): Boolean {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val previousBody = prefs.getString(KEY_BODY, null).orEmpty()
        val sameMessage = equivalentBody(previousBody, body) &&
            now - prefs.getLong(KEY_TIME, 0L) in 0..DEDUPE_WINDOW_MS

        if (sameMessage) {
            EventLog.append(context, "Alarm gate: duplicate from $source, alarm skipped")
            return false
        }

        prefs.edit()
            .putString(KEY_SENDER, sender)
            .putString(KEY_BODY, body)
            .putLong(KEY_TIME, now)
            .apply()
        return true
    }

    private fun equivalentBody(previous: String, current: String): Boolean {
        if (previous.isBlank() || current.isBlank()) return false
        val a = previous.filterNot { it.isWhitespace() }
        val b = current.filterNot { it.isWhitespace() }
        return a == b || (minOf(a.length, b.length) >= 8 && (a.contains(b) || b.contains(a)))
    }
}
