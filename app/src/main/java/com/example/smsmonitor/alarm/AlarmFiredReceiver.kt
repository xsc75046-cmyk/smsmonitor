package com.example.smsmonitor.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.smsmonitor.playback.AlarmPlaybackService

/**
 * Fired by AlarmManager.setAlarmClock() at the exact sub-minute moment
 * (e.g. 5 seconds after the user clicked the button). The OEM clock app's
 * minute-resolution alarm will follow shortly. This receiver just records
 * the event and shows a heads-up toast so the user knows the chain is alive.
 */
class AlarmFiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == CUSTOM_ACTION) {
            val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
            val playbackIntent = Intent(context, AlarmPlaybackService::class.java).apply {
                putExtra(AlarmPlaybackService.EXTRA_SOURCE, source)
                putExtra(
                    AlarmPlaybackService.EXTRA_VOLUME,
                    intent.getIntExtra(EXTRA_VOLUME, 10)
                )
                putExtra(
                    AlarmPlaybackService.EXTRA_DURATION,
                    intent.getIntExtra(EXTRA_DURATION, 30)
                )
                putExtra(
                    AlarmPlaybackService.EXTRA_VIBRATE,
                    intent.getBooleanExtra(EXTRA_VIBRATE, true)
                )
                putExtra(
                    AlarmPlaybackService.EXTRA_SENDER,
                    intent.getStringExtra(EXTRA_SENDER).orEmpty()
                )
                putExtra(
                    AlarmPlaybackService.EXTRA_BODY,
                    intent.getStringExtra(EXTRA_BODY).orEmpty()
                )
            }
            EventLog.append(context, "Custom alarm fired: starting AlarmPlaybackService")
            try {
                ContextCompat.startForegroundService(context, playbackIntent)
            } catch (t: Throwable) {
                EventLog.append(context, "Custom alarm service start FAILED: ${t.message}")
            }
            return
        }
        if (intent.action != ACTION) return
        val delay = intent.getIntExtra(EXTRA_DELAY_SECONDS, -1)
        EventLog.append(context, "SystemAlarmFired (scheduled +${delay}s)")
        // No audible playback here — the OEM clock app owns the alarm sound.
        Toast.makeText(context, "定时器到点 (+${delay}s), 系统闹钟即将响铃", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION = "com.example.smsmonitor.action.SYSTEM_ALARM_FIRED"
        const val CUSTOM_ACTION = "com.example.smsmonitor.action.CUSTOM_ALARM_FIRED"
        const val EXTRA_DELAY_SECONDS = "extra_delay_seconds"
        const val EXTRA_SOURCE = "extra_playback_source"
        const val EXTRA_VOLUME = "extra_playback_volume"
        const val EXTRA_DURATION = "extra_playback_duration"
        const val EXTRA_VIBRATE = "extra_playback_vibrate"
        const val EXTRA_SENDER = "extra_playback_sender"
        const val EXTRA_BODY = "extra_playback_body"
    }
}
