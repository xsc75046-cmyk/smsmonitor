package com.example.smsmonitor.alarm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EventLog {
    private const val TAG = "SmsMonitorLog"
    private const val PREFS = "sms_monitor_event_log"
    private const val KEY_LINES = "lines"
    private const val MAX_LINES = 50
    private const val CACHE_FILE = "event-log.txt"
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun append(context: Context, message: String) {
        try {
            val prefs: SharedPreferences = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lines = prefs.getString(KEY_LINES, "").orEmpty()
                .split('\n')
                .filter { it.isNotEmpty() }
                .toMutableList()
            val stamp = fmt.format(Date())
            val stamped = "[$stamp] $message"
            lines.add(stamped)
            while (lines.size > MAX_LINES) lines.removeAt(0)
            prefs.edit().putString(KEY_LINES, lines.joinToString("\n")).apply()
            Log.i(TAG, stamped)
        } catch (_: Throwable) {
            // never crash the caller
        }
    }

    fun read(context: Context): String {
        val stored = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LINES, "").orEmpty()
        return stored.split('\n')
            .filter { it.isNotEmpty() }
            .asReversed()
            .joinToString("\n")
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_LINES).apply()
    }

    fun copyToClipboard(context: Context): Boolean {
        return try {
            val text = read(context)
            if (text.isBlank()) return false
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("SmsMonitor event log", text))
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun saveToCache(context: Context): File? {
        return try {
            val text = read(context)
            val file = File(context.cacheDir, CACHE_FILE)
            file.writeText(text)
            file
        } catch (_: Throwable) {
            null
        }
    }
}
