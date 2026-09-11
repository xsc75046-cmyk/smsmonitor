package com.example.smsmonitor.playback

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsmonitor.ui.theme.SmsMonitorTheme

class AlarmAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        val sender = intent.getStringExtra(AlarmPlaybackService.EXTRA_SENDER).orEmpty()
        val body = intent.getStringExtra(AlarmPlaybackService.EXTRA_BODY).orEmpty()
        val source = intent.getStringExtra(AlarmPlaybackService.EXTRA_SOURCE).orEmpty()
        val volume = intent.getIntExtra(AlarmPlaybackService.EXTRA_VOLUME, 10)
        val duration = intent.getIntExtra(AlarmPlaybackService.EXTRA_DURATION, 30)
        val vibrate = intent.getBooleanExtra(AlarmPlaybackService.EXTRA_VIBRATE, true)
        setContent {
            SmsMonitorTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AlarmAlertContent(
                        sender = sender,
                        body = body,
                        onStop = {
                            sendServiceAction(AlarmPlaybackService.ACTION_STOP, source, volume, duration, vibrate, sender, body)
                            finish()
                        },
                        onSnooze = {
                            sendServiceAction(AlarmPlaybackService.ACTION_SNOOZE, source, volume, duration, vibrate, sender, body)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun sendServiceAction(
        action: String,
        source: String,
        volume: Int,
        duration: Int,
        vibrate: Boolean,
        sender: String,
        body: String
    ) {
        startService(Intent(this, AlarmPlaybackService::class.java).apply {
            this.action = action
            putExtra(AlarmPlaybackService.EXTRA_SOURCE, source)
            putExtra(AlarmPlaybackService.EXTRA_VOLUME, volume)
            putExtra(AlarmPlaybackService.EXTRA_DURATION, duration)
            putExtra(AlarmPlaybackService.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmPlaybackService.EXTRA_SENDER, sender)
            putExtra(AlarmPlaybackService.EXTRA_BODY, body)
        })
    }
}

@Composable
private fun AlarmAlertContent(
    sender: String,
    body: String,
    onStop: () -> Unit,
    onSnooze: () -> Unit
) {
    var showFullBody by remember { mutableStateOf(false) }
    val summary = body.replace("\r", " ").replace("\n", " ").let {
        if (it.length > 80) it.take(80) + "…" else it
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("短信提醒", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (sender.isNotBlank()) {
            Text("发送方：$sender", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            if (body.isBlank()) "自定义提醒正在播放" else if (showFullBody) body else summary,
            style = MaterialTheme.typography.bodyLarge
        )
        if (body.length > 80) {
            OutlinedButton(onClick = { showFullBody = !showFullBody }) {
                Text(if (showFullBody) "收起短信" else "查看完整短信")
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onStop, modifier = Modifier.weight(1f)) {
                Text("停止提醒")
            }
            OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                Text("稍后 5 分钟")
            }
        }
    }
}
