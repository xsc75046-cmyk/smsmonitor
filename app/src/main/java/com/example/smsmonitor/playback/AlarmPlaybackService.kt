package com.example.smsmonitor.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.smsmonitor.MainActivity
import com.example.smsmonitor.R
import com.example.smsmonitor.alarm.EventLog
import com.example.smsmonitor.alarm.Scheduler
import kotlin.math.roundToInt

class AlarmPlaybackService : Service() {

    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var stopRunnable: Runnable? = null
    private var vibrator: Vibrator? = null
    private var source: String = ""
    private var volumePercent: Int = 10
    private var durationSeconds: Int = 30
    private var shouldVibrate: Boolean = true
    private var sender: String = ""
    private var body: String = ""
    private var previousAlarmVolume: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        applyPlaybackExtras(intent)
        if (intent?.action == ACTION_STOP) {
            EventLog.append(this, "AlarmPlaybackService stop requested")
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SNOOZE) {
            EventLog.append(this, "AlarmPlaybackService snooze requested")
            Scheduler.scheduleCustomPlayback(
                this,
                5 * 60,
                source,
                volumePercent,
                durationSeconds,
                shouldVibrate,
                sender,
                body
            )
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        EventLog.append(this, "AlarmPlaybackService started source='$source' volume=$volumePercent% duration=${durationSeconds}s vibrate=$shouldVibrate")
        releasePlayer()
        if (source.isBlank()) {
            EventLog.append(this, "AlarmPlaybackService: source is blank, playback skipped")
            stopSelf()
            return START_NOT_STICKY
        }
        // Set the system alarm stream before preparing media so the configured
        // volume applies to both local and remote sources. It is restored in
        // abandonAudioFocus() when the reminder ends.
        setTemporaryAlarmVolume()
        if (shouldVibrate) startVibration()
        scheduleDurationStop()
        play(source)
        return START_NOT_STICKY
    }

    private fun applyPlaybackExtras(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra(EXTRA_SOURCE)) source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
        if (intent.hasExtra(EXTRA_VOLUME)) {
            volumePercent = intent.getIntExtra(EXTRA_VOLUME, 10).coerceIn(0, 100)
        }
        if (intent.hasExtra(EXTRA_DURATION)) {
            durationSeconds = intent.getIntExtra(EXTRA_DURATION, 30).coerceIn(1, 3600)
        }
        if (intent.hasExtra(EXTRA_VIBRATE)) shouldVibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        if (intent.hasExtra(EXTRA_SENDER)) sender = intent.getStringExtra(EXTRA_SENDER).orEmpty()
        if (intent.hasExtra(EXTRA_BODY)) body = intent.getStringExtra(EXTRA_BODY).orEmpty()
    }

    private fun play(source: String) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        try {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            if (source.startsWith("http://") || source.startsWith("https://")) {
                mediaPlayer.setDataSource(source)
            } else {
                mediaPlayer.setDataSource(this, Uri.parse(source))
            }
            mediaPlayer.setOnPreparedListener {
                requestAudioFocus()
                it.start()
                EventLog.append(this, "AlarmPlaybackService playback started")
            }
            mediaPlayer.setOnCompletionListener {
                EventLog.append(this, "AlarmPlaybackService playback completed")
                stopSelf()
            }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                EventLog.append(this, "AlarmPlaybackService playback error what=$what extra=$extra")
                stopSelf()
                true
            }
            mediaPlayer.prepareAsync()
        } catch (t: Throwable) {
            EventLog.append(this, "AlarmPlaybackService prepare FAILED: ${t.message}")
            releasePlayer()
            stopSelf()
        }
    }

    private fun scheduleDurationStop() {
        stopRunnable?.let { android.os.Handler(mainLooper).removeCallbacks(it) }
        val stop = Runnable {
            EventLog.append(this, "AlarmPlaybackService duration reached, stopping")
            stopSelf()
        }
        stopRunnable = stop
        android.os.Handler(mainLooper).postDelayed(stop, durationSeconds * 1000L)
    }

    private fun setTemporaryAlarmVolume() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val manager = audioManager ?: run {
            EventLog.append(this, "AlarmPlaybackService alarm volume unavailable")
            return
        }
        if (previousAlarmVolume == null) {
            previousAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val targetVolume = (maxVolume * volumePercent / 100f).roundToInt().coerceIn(0, maxVolume)
        runCatching {
            manager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
            val actualVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
            EventLog.append(this, "AlarmPlaybackService temporary alarm volume set $actualVolume/$maxVolume (target=$targetVolume, restore=${previousAlarmVolume})")
        }.onFailure {
            EventLog.append(this, "AlarmPlaybackService alarm volume set failed: ${it.message}")
        }
    }

    private fun requestAudioFocus() {
        val manager = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.also {
            audioManager = it
        } ?: return
        val listener = AudioManager.OnAudioFocusChangeListener { }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(listener)
                .build()
            manager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(listener, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        previousAlarmVolume?.let { previous ->
            runCatching {
                manager.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
                val restoredVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
                EventLog.append(this, "AlarmPlaybackService alarm volume restored $restoredVolume")
            }.onFailure {
                EventLog.append(this, "AlarmPlaybackService alarm volume restore failed: ${it.message}")
            }
        }
        previousAlarmVolume = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
        }
        audioManager = null
        focusRequest = null
    }

    private fun releasePlayer() {
        stopRunnable?.let { android.os.Handler(mainLooper).removeCallbacks(it) }
        stopRunnable = null
        player?.runCatching { stop() }
        player?.release()
        player = null
        abandonAudioFocus()
        vibrator?.cancel()
        vibrator = null
    }

    private fun startVibration() {
        val v = getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) {
            EventLog.append(this, "AlarmPlaybackService vibration unavailable")
            return
        }
        vibrator = v
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0)
        val vibrationResult = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                v.vibrate(effect, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build())
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effect)
            }
        }
        if (vibrationResult.isFailure) {
            EventLog.append(this, "AlarmPlaybackService vibration FAILED: ${vibrationResult.exceptionOrNull()?.message}")
            vibrator = null
            return
        }
        EventLog.append(this, "AlarmPlaybackService vibration started")
    }

    private fun buildNotification(): Notification {
        val channelManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "自定义闹钟",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { setSound(null, null) }
            )
        }
        val openIntent = PendingIntent.getActivity(
            this,
            9001,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            9002,
            Intent(this, AlarmAlertActivity::class.java).apply {
                putExtra(EXTRA_SOURCE, source)
                putExtra(EXTRA_VOLUME, volumePercent)
                putExtra(EXTRA_DURATION, durationSeconds)
                putExtra(EXTRA_VIBRATE, shouldVibrate)
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            9003,
            actionIntent(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = PendingIntent.getService(
            this,
            9004,
            actionIntent(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("自定义闹钟播放中")
            .setContentText(if (sender.isBlank()) "短信提醒助手播放中" else "来自 $sender 的短信提醒")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(0, "停止", stopIntent)
            .addAction(0, "稍后 5 分钟", snoozeIntent)
            .build()
    }

    private fun actionIntent(action: String): Intent =
        Intent(this, AlarmPlaybackService::class.java).apply {
            this.action = action
            putExtra(EXTRA_SOURCE, source)
            putExtra(EXTRA_VOLUME, volumePercent)
            putExtra(EXTRA_DURATION, durationSeconds)
            putExtra(EXTRA_VIBRATE, shouldVibrate)
            putExtra(EXTRA_SENDER, sender)
            putExtra(EXTRA_BODY, body)
        }

    override fun onDestroy() {
        releasePlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        EventLog.append(this, "AlarmPlaybackService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_SOURCE = "extra_playback_source"
        const val EXTRA_VOLUME = "extra_playback_volume"
        const val EXTRA_DURATION = "extra_playback_duration"
        const val EXTRA_VIBRATE = "extra_playback_vibrate"
        const val EXTRA_SENDER = "extra_playback_sender"
        const val EXTRA_BODY = "extra_playback_body"
        const val ACTION_STOP = "com.example.smsmonitor.action.STOP_PLAYBACK"
        const val ACTION_SNOOZE = "com.example.smsmonitor.action.SNOOZE_PLAYBACK"
        private const val CHANNEL_ID = "custom_alarm_playback"
        private const val NOTIFICATION_ID = 8101
    }
}
