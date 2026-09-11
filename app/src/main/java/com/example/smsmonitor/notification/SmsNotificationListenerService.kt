package com.example.smsmonitor.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.smsmonitor.alarm.AlarmTriggerGate
import com.example.smsmonitor.alarm.EventLog
import com.example.smsmonitor.alarm.Scheduler
import com.example.smsmonitor.data.ConfigRepository

/** Backup channel for devices that do not deliver SMS_RECEIVED to third-party
 * apps. It reads notification text only after the user grants notification
 * access in system settings. */
class SmsNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        EventLog.append(this, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        EventLog.append(this, "Notification listener disconnected")
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (!isSmsNotification(sbn)) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(separator = " ")
            ?: extras.getCharSequenceArrayList(Notification.EXTRA_TEXT_LINES)
                ?.joinToString(separator = " ")
                .orEmpty()
        val ticker = sbn.notification.tickerText?.toString().orEmpty()
        val body = listOf(text, bigText, lines, ticker)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(separator = " | ")
        val printableBody = body.replace("\r", "\\r").replace("\n", "\\n")

        EventLog.append(
            this,
            "Notification posted package='${sbn.packageName}' id=${sbn.id} " +
                "title='$title' body='$printableBody'"
        )

        val repo = ConfigRepository(this)
        val matched = repo.matches(title, body)
        EventLog.append(
            this,
            "Notification match: monitoring=${repo.monitoringEnabled} matched=$matched " +
                "mode=${if (repo.customPlaybackOnMatch) "custom" else "system"} " +
                "enabledRules=${repo.rules.count { it.enabled }}"
        )

        if (matched && AlarmTriggerGate.shouldSchedule(this, "notification", title, body)) {
            if (repo.customPlaybackOnMatch) {
                EventLog.append(this, "Notification match -> custom playback alarm")
                Scheduler.scheduleCustomPlayback(
                    this,
                    0,
                    repo.playbackSource,
                    repo.playbackVolume,
                    repo.playbackDurationSeconds,
                    repo.playbackVibrate,
                    title,
                    body
                )
            } else {
                EventLog.append(this, "Notification match -> calling Scheduler.scheduleAlarm(0)")
                Scheduler.scheduleAlarm(this, 0)
            }
            EventLog.append(this, "Notification match alarm scheduling returned")
        }
    }

    private fun isSmsNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName.lowercase()
        if (SMS_PACKAGES.any { packageName == it || packageName.startsWith("$it.") }) return true
        return sbn.notification.category == android.app.Notification.CATEGORY_MESSAGE &&
            (packageName.contains("mms") || packageName.contains("sms"))
    }

    companion object {
        private val SMS_PACKAGES = setOf(
            "com.android.mms.service",
            "com.android.mms",
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.vivo.messaging",
            "com.vivo.message",
            "com.vivo.sms"
        )
    }
}
