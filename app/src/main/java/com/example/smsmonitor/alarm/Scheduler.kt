package com.example.smsmonitor.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import android.widget.Toast
import java.util.Calendar

object Scheduler {

    private const val TAG = "Scheduler"

    /**
     * Schedule a one-shot alarm through the system clock app via
     * [AlarmClock.ACTION_SET_ALARM]. The OEM clock app owns the audible
     * alarm (ring + vibrate + full-screen) — this app does not play any
     * sound itself.
     *
     * Note: ACTION_SET_ALARM has only minute resolution, so "delaySeconds=5"
     * rounds to the next minute mark. For sub-minute precision we also
     * arm an AlarmManager.setAlarmClock() backup that fires our receiver
     * at exactly the requested moment; the user gets a heads-up
     * notification when the sub-minute timer fires, in case the OEM clock
     * app silently dropped the request.
     */
    fun scheduleAlarm(context: Context, delaySeconds: Int) {
        val now = System.currentTimeMillis()
        val triggerAt = now + delaySeconds * 1000L

        // Sub-minute exact timer via AlarmManager.setAlarmClock (system framework).
        armExactTimer(context, triggerAt, delaySeconds)

        // OEM clock-app database entry via ACTION_SET_ALARM (minute resolution).
        val ok = armViaClockApp(context, triggerAt)
        if (!ok) {
            // Clock app missing or refused; the exact-timer broadcast will be
            // the only user-visible signal (we surface it as a heads-up toast).
            EventLog.append(context, "Scheduler: clock-app intent did not start; relying on exact timer")
        }
    }

    fun scheduleCustomPlayback(
        context: Context,
        delaySeconds: Int,
        source: String,
        volumePercent: Int,
        durationSeconds: Int,
        vibrate: Boolean,
        sender: String = "",
        body: String = ""
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            EventLog.append(context, "Custom playback: exact alarm permission unavailable")
            Toast.makeText(context, "未允许闹钟提醒权限，请在系统设置中开启", Toast.LENGTH_LONG).show()
            return
        }
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        val intent = Intent(context, AlarmFiredReceiver::class.java).apply {
            action = AlarmFiredReceiver.CUSTOM_ACTION
            putExtra(AlarmFiredReceiver.EXTRA_SOURCE, source)
            putExtra(AlarmFiredReceiver.EXTRA_VOLUME, volumePercent.coerceIn(0, 100))
            putExtra(AlarmFiredReceiver.EXTRA_DURATION, durationSeconds.coerceIn(1, 60))
            putExtra(AlarmFiredReceiver.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmFiredReceiver.EXTRA_SENDER, sender)
            putExtra(AlarmFiredReceiver.EXTRA_BODY, body)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CUSTOM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, null), pi)
            EventLog.append(
                context,
                "Custom playback: setAlarmClock +${delaySeconds}s armed " +
                    "volume=${volumePercent.coerceIn(0, 100)}% duration=${durationSeconds.coerceIn(1, 60)}s vibrate=$vibrate"
            )
            Toast.makeText(context, "自定义闹钟已设置, ${delaySeconds} 秒后播放", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            EventLog.append(context, "Custom playback setAlarmClock FAILED: ${t.message}")
            Log.e(TAG, "custom playback alarm failed", t)
        }
    }

    private fun armExactTimer(context: Context, triggerAt: Long, delaySeconds: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return

        val fireIntent = Intent(context, AlarmFiredReceiver::class.java).apply {
            action = AlarmFiredReceiver.ACTION
            putExtra(AlarmFiredReceiver.EXTRA_DELAY_SECONDS, delaySeconds)
        }
        val firePi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_FIRE,
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            val info = AlarmManager.AlarmClockInfo(triggerAt, null)
            am.setAlarmClock(info, firePi)
            EventLog.append(context, "Scheduler: setAlarmClock +${delaySeconds}s armed")
        } catch (t: Throwable) {
            Log.w(TAG, "setAlarmClock failed", t)
        }
    }

    private fun armViaClockApp(context: Context, triggerAt: Long): Boolean {
        // Vivo (and most OEM clock apps) reject or silently postpone alarm
        // entries whose time is in the past — they end up listed as "tomorrow
        // HH:MM" instead of ringing immediately. We always offset by +1 minute
        // to guarantee the entry is in the future, then round up to the next
        // whole minute so the user sees a clean clock time.
        val target = Calendar.getInstance().apply {
            timeInMillis = triggerAt
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If rounding pushed us to a minute equal to or before the original
        // requested second-precision trigger (within the same minute), bump
        // another minute so we are always strictly in the future.
        if (target.timeInMillis <= triggerAt) {
            target.add(Calendar.MINUTE, 1)
        }
        val hour = target.get(Calendar.HOUR_OF_DAY)
        val minute = target.get(Calendar.MINUTE)

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true) // 是否跳过闹钟设置界面
            putExtra(AlarmClock.EXTRA_VIBRATE, true) // 是否开启振动
            putExtra(AlarmClock.EXTRA_MESSAGE, "短信提醒助手")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            EventLog.append(
                context,
                "Scheduler OK: ACTION_SET_ALARM ${"%02d:%02d".format(hour, minute)} (skip_ui=true)"
            )
            Toast.makeText(
                context,
                "已写入系统闹钟 ${"%02d:%02d".format(hour, minute)}",
                Toast.LENGTH_SHORT
            ).show()
            true
        } catch (t: Throwable) {
            EventLog.append(context, "Scheduler startActivity FAILED: ${t.message}")
            Log.e(TAG, "startActivity failed", t)
            Toast.makeText(context, "启动闹钟App失败: ${t.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    private const val REQUEST_CODE_FIRE = 3001
    private const val REQUEST_CODE_CUSTOM = 3002
}
