package com.example.smsmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.smsmonitor.alarm.EventLog
import com.example.smsmonitor.alarm.AlarmTriggerGate
import com.example.smsmonitor.alarm.Scheduler
import com.example.smsmonitor.data.ConfigRepository

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        EventLog.append(context, "[1/7] onReceive enter, action=${intent.action}")
        EventLog.append(
            context,
            "[1/7] extras=${intent.extras?.keySet()?.joinToString(",").orEmpty()}"
        )

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            EventLog.append(context, "[1/7] ignore non-SMS intent")
            return
        }
        EventLog.append(context, "[2/7] SMS_RECEIVED confirmed, goAsync")

        val pending = goAsync()
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                EventLog.append(context, "[3/7] no messages in pdus, abort")
                pending.finish()
                return
            }
            EventLog.append(context, "[3/7] parsed ${messages.size} pdu segment(s)")

            val sender = messages.firstOrNull()?.displayOriginatingAddress.orEmpty()
            val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
            val printableBody = body.replace("\r", "\\r").replace("\n", "\\n")
            EventLog.append(context, "[4/7] SMS RECEIVED sender='$sender' body='$printableBody'")

            val repo = ConfigRepository(context)
            val monitoring = repo.monitoringEnabled
            val matched = repo.matches(sender, body)
            EventLog.append(
                context,
                    "[5/7] monitoring=$monitoring  matched=$matched  " +
                    "mode=${if (repo.customPlaybackOnMatch) "custom" else "system"} " +
                    "enabledRules=${repo.rules.count { it.enabled }}"
            )

            if (matched) {
                if (AlarmTriggerGate.shouldSchedule(context, "sms", sender, body)) {
                    if (repo.customPlaybackOnMatch) {
                        EventLog.append(context, "[6/7] match → custom playback alarm")
                        Scheduler.scheduleCustomPlayback(
                            context,
                            0,
                            repo.playbackSource,
                            repo.playbackVolume,
                            repo.playbackDurationSeconds,
                            repo.playbackVibrate,
                            sender,
                            body
                        )
                    } else {
                        EventLog.append(context, "[6/7] match → calling Scheduler.scheduleAlarm(0)")
                        Scheduler.scheduleAlarm(context, 0)
                    }
                    EventLog.append(context, "[7/7] match alarm scheduling returned")
                } else {
                    EventLog.append(context, "[6/7] duplicate message, alarm NOT triggered")
                }
            } else {
                EventLog.append(context, "[6/7] no match, alarm NOT triggered")
            }
        } catch (t: Throwable) {
            EventLog.append(context, "SmsReceiver EXCEPTION: ${t.message}")
        } finally {
            pending.finish()
        }
    }
}
