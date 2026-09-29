package com.example.smsmonitor.ui

import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.smsmonitor.alarm.EventLog
import com.example.smsmonitor.alarm.Scheduler
import com.example.smsmonitor.data.ConfigRepository
import com.example.smsmonitor.data.PlaybackSourceMode
import com.example.smsmonitor.data.RuleMatchMode
import com.example.smsmonitor.data.SmsRule
import com.example.smsmonitor.ui.theme.AppSuccess
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { ConfigRepository(context) }

    var notificationAccessGranted by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        )
    }
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var fullScreenIntentGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                (context.getSystemService(android.app.NotificationManager::class.java)
                    ?.canUseFullScreenIntent() == true)
        )
    }
    var exactAlarmGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true)
        )
    }
    var batteryOptimizationIgnored by remember {
        mutableStateOf(
            (context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName) == true)
        )
    }
    var vibrationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var smsPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
                notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                fullScreenIntentGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                    (context.getSystemService(android.app.NotificationManager::class.java)
                        ?.canUseFullScreenIntent() == true)
                exactAlarmGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    (context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true)
                batteryOptimizationIgnored = context.getSystemService(PowerManager::class.java)
                    ?.isIgnoringBatteryOptimizations(context.packageName) == true
                vibrationGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.VIBRATE
                ) == PackageManager.PERMISSION_GRANTED
                smsPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                    PackageManager.PERMISSION_GRANTED
                EventLog.append(
                    context,
                    "Core permission check (resume): notification=$notificationPermissionGranted " +
                        "fullScreen=$fullScreenIntentGranted vibration=$vibrationGranted " +
                        "alarm=$exactAlarmGranted"
                )
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsPermissionGranted = granted
        EventLog.append(context, "RECEIVE_SMS permission result=$granted")
        if (!granted) {
            Toast.makeText(context, "未授予短信读取权限, 无法监听新短信", Toast.LENGTH_LONG).show()
        }
        onRequestNotificationPermission()
    }

    var monitoringEnabled by remember { mutableStateOf(repo.monitoringEnabled) }
    var rules by remember { mutableStateOf(repo.rules) }
    var activeWindowEnabled by remember { mutableStateOf(repo.activeWindowEnabled) }
    var activeWindowStart by remember { mutableStateOf(formatMinutes(repo.activeWindowStart)) }
    var activeWindowEnd by remember { mutableStateOf(formatMinutes(repo.activeWindowEnd)) }
    var blacklistSenders by remember { mutableStateOf(repo.blacklistSenders) }
    var ignoreCarrierVerification by remember { mutableStateOf(repo.ignoreCarrierVerification) }
    var playbackSource by remember { mutableStateOf(repo.playbackSource) }
    var playbackSourceMode by remember { mutableStateOf(repo.playbackSourceMode) }
    var playbackVolume by remember { mutableStateOf(repo.playbackVolume.toString()) }
    var playbackDuration by remember { mutableStateOf(repo.playbackDurationSeconds.toString()) }
    var playbackVibrate by remember { mutableStateOf(repo.playbackVibrate) }
    var customPlaybackOnMatch by remember { mutableStateOf(repo.customPlaybackOnMatch) }
    var loaded by remember { mutableStateOf(false) }
    val localAudioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        playbackSourceMode = PlaybackSourceMode.LOCAL
        repo.playbackSourceMode = PlaybackSourceMode.LOCAL
        playbackSource = uri.toString()
        if (loaded) repo.playbackSource = uri.toString()
        EventLog.append(context, "Local playback source selected: $uri")
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("设置", "匹配规则", "提醒设置", "运行日志")
    val tabIcons = listOf(
        Icons.Filled.CheckCircle,
        Icons.Filled.Alarm,
        Icons.Filled.Timer,
        Icons.Filled.Refresh
    )
    var refreshTick by remember { mutableIntStateOf(0) }
    val eventLogText = remember(refreshTick) { EventLog.read(context) }
    LaunchedEffect(Unit) {
        monitoringEnabled = repo.monitoringEnabled
        rules = repo.rules
        activeWindowEnabled = repo.activeWindowEnabled
        activeWindowStart = formatMinutes(repo.activeWindowStart)
        activeWindowEnd = formatMinutes(repo.activeWindowEnd)
        blacklistSenders = repo.blacklistSenders
        ignoreCarrierVerification = repo.ignoreCarrierVerification
        playbackSource = repo.playbackSource
        playbackSourceMode = repo.playbackSourceMode
        playbackVolume = repo.playbackVolume.toString()
        playbackDuration = repo.playbackDurationSeconds.toString()
        playbackVibrate = repo.playbackVibrate
        customPlaybackOnMatch = repo.customPlaybackOnMatch
        loaded = true
        EventLog.append(
            context,
            "Core permission check: notification=$notificationPermissionGranted " +
                "fullScreen=$fullScreenIntentGranted vibration=$vibrationGranted " +
                "alarm=$exactAlarmGranted"
        )
        EventLog.append(
            context,
            "Listener init: RECEIVE_SMS granted=$smsPermissionGranted, monitoring=${repo.monitoringEnabled}, " +
                "rules=${repo.rules.size}, windowEnabled=${repo.activeWindowEnabled}"
        )
        EventLog.append(context, "Notification listener init: accessGranted=$notificationAccessGranted")
        if (!smsPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            EventLog.append(context, "Listener init: requesting RECEIVE_SMS permission")
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        } else {
            onRequestNotificationPermission()
        }
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) refreshTick++
    }

    fun scheduleNow() {
        EventLog.append(context, "UI clicked 立即响铃")
        Scheduler.scheduleAlarm(context, 0)
        refreshTick++
    }

    fun schedule5s() {
        EventLog.append(context, "UI clicked 5秒后响铃")
        Scheduler.scheduleAlarm(context, 5)
        refreshTick++
    }

    fun scheduleCustomPlayback() {
        val volume = playbackVolume.toIntOrNull()?.coerceIn(0, 100)
        val duration = playbackDuration.toIntOrNull()?.coerceIn(1, 60)
        if (playbackSource.isBlank() || volume == null || duration == null) {
            Toast.makeText(context, "请填写有效的音乐地址、音量(0-100)和时长(1-60秒)", Toast.LENGTH_LONG).show()
            return
        }
        repo.playbackSource = playbackSource.trim()
        repo.playbackVolume = volume
        repo.playbackDurationSeconds = duration
        EventLog.append(context, "UI clicked 自定义闹钟播放")
        Scheduler.scheduleCustomPlayback(
            context,
            5,
            playbackSource.trim(),
            volume,
            duration,
            playbackVibrate
        )
        refreshTick++
    }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(31.dp),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    SettingsOverview(
                        monitoringEnabled = monitoringEnabled,
                        enabledRuleCount = rules.count { it.enabled },
                        activeWindowEnabled = activeWindowEnabled,
                        activeWindowStart = activeWindowStart,
                        activeWindowEnd = activeWindowEnd,
                        onOpenRules = { selectedTab = 1 },
                        onOpenReminderSettings = { selectedTab = 2 },
                        onMonitoringChange = { value ->
                            if (value && !smsPermissionGranted) {
                                Toast.makeText(context, "请先授予短信监听权限", Toast.LENGTH_SHORT).show()
                            } else {
                                monitoringEnabled = value
                                if (loaded) repo.monitoringEnabled = value
                                EventLog.append(context, "Listener switch changed: monitoring=$value")
                            }
                        },
                        smsPermissionGranted = smsPermissionGranted,
                        onRequestSmsPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            }
                        },
                        notificationAccessGranted = notificationAccessGranted,
                        onOpenNotificationSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                EventLog.append(context, "Notification listener settings opened")
                            } catch (t: Throwable) {
                                EventLog.append(context, "Notification listener settings failed: ${t.message}")
                                Toast.makeText(context, "无法打开通知访问设置", Toast.LENGTH_LONG).show()
                            }
                        },
                        notificationPermissionGranted = notificationPermissionGranted,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        fullScreenIntentGranted = fullScreenIntentGranted,
                        onFullScreenIntent = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                openSettings(context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse("package:${context.packageName}"))
                            }
                        },
                        exactAlarmGranted = exactAlarmGranted,
                        onExactAlarm = {
                            openSettings(context, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"))
                        },
                        batteryOptimizationIgnored = batteryOptimizationIgnored,
                        onBatteryOptimization = {
                            openSettings(context, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"))
                        },
                        vibrationGranted = vibrationGranted,
                        onBackgroundSettings = {
                            openSettings(context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"))
                        }
                    )
                }
                1 -> {
                    RuleManagementCard(
                        rules = rules,
                        onRulesChanged = {
                            rules = it
                            repo.rules = it
                            refreshTick++
                        }
                    )
                    FilterOptionsCard(
                        blacklist = blacklistSenders,
                        onBlacklistChange = {
                            blacklistSenders = it
                            repo.blacklistSenders = it
                        },
                        ignoreCarrierVerification = ignoreCarrierVerification,
                        onIgnoreCarrierChange = {
                            ignoreCarrierVerification = it
                            repo.ignoreCarrierVerification = it
                        }
                    )
                }
                2 -> {
                    CustomPlaybackSwitchRow(
                        enabled = customPlaybackOnMatch,
                        sourceMode = playbackSourceMode,
                        duration = playbackDuration,
                        onEnabledChange = {
                            customPlaybackOnMatch = it
                            if (loaded) repo.customPlaybackOnMatch = it
                        }
                    )
                    TimeWindowCard(
                        enabled = activeWindowEnabled,
                        start = activeWindowStart,
                        end = activeWindowEnd,
                        onEnabledChange = {
                            activeWindowEnabled = it
                            repo.activeWindowEnabled = it
                        },
                        onStartChange = { value ->
                            activeWindowStart = value
                            parseMinutes(value)?.let { repo.activeWindowStart = it }
                        },
                        onEndChange = { value ->
                            activeWindowEnd = value
                            parseMinutes(value)?.let { repo.activeWindowEnd = it }
                        }
                    )
                    CustomPlaybackCard(
                        source = playbackSource,
                        sourceMode = playbackSourceMode,
                        onPickLocalSource = {
                            localAudioPicker.launch(arrayOf("audio/*"))
                        },
                        onSourceModeChange = { mode ->
                            playbackSourceMode = mode
                            repo.playbackSourceMode = mode
                            playbackSource = repo.playbackSource
                        },
                        onSourceChange = {
                            playbackSource = it
                            if (loaded) repo.playbackSource = it
                        },
                        volume = playbackVolume,
                        onVolumeChange = {
                            playbackVolume = it.filter(Char::isDigit).take(3)
                            playbackVolume.toIntOrNull()?.let { value -> if (loaded) repo.playbackVolume = value }
                        },
                        duration = playbackDuration,
                        onDurationChange = {
                            playbackDuration = it.filter(Char::isDigit).take(4)
                            playbackDuration.toIntOrNull()?.let { value -> if (loaded) repo.playbackDurationSeconds = value }
                        },
                        vibrate = playbackVibrate,
                        onVibrateChange = {
                            playbackVibrate = it
                            if (loaded) repo.playbackVibrate = it
                        },
                        onSchedule = ::scheduleCustomPlayback
                    )
                    Button(onClick = ::scheduleNow, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Alarm, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("立即测试系统闹钟")
                    }
                }
                else -> {
                    InfoCard()
                    EventLogCard(
                        text = eventLogText,
                        onRefresh = { refreshTick++ },
                        onClear = {
                            EventLog.clear(context)
                            refreshTick++
                        },
                        onCopy = {
                            val ok = EventLog.copyToClipboard(context)
                            Toast.makeText(context, if (ok) "已复制到剪贴板" else "日志为空", Toast.LENGTH_SHORT).show()
                        },
                        onSave = {
                            val file = EventLog.saveToCache(context)
                            Toast.makeText(context, if (file != null) "日志已保存到 ${file.absolutePath}" else "保存失败", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsOverview(
    monitoringEnabled: Boolean,
    enabledRuleCount: Int,
    activeWindowEnabled: Boolean,
    activeWindowStart: String,
    activeWindowEnd: String,
    onOpenRules: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onMonitoringChange: (Boolean) -> Unit,
    smsPermissionGranted: Boolean,
    onRequestSmsPermission: () -> Unit,
    notificationAccessGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    fullScreenIntentGranted: Boolean,
    onFullScreenIntent: () -> Unit,
    exactAlarmGranted: Boolean,
    onExactAlarm: () -> Unit,
    batteryOptimizationIgnored: Boolean,
    onBatteryOptimization: () -> Unit,
    vibrationGranted: Boolean,
    onBackgroundSettings: () -> Unit
) {
    val pendingCount = listOf(
        !smsPermissionGranted,
        !notificationAccessGranted,
        !notificationPermissionGranted,
        !fullScreenIntentGranted,
        !exactAlarmGranted,
        !batteryOptimizationIgnored,
        !vibrationGranted
    ).count { it }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "短信提醒总开关",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (monitoringEnabled) {
                            "关闭后不再推送任何提醒，规则仍然保留"
                        } else {
                            "当前已关闭提醒，开启后按规则监听新短信"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = monitoringEnabled, onCheckedChange = onMonitoringChange)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                OverviewSettingRow(
                    icon = Icons.Filled.CheckCircle,
                    label = "启用规则",
                    value = "$enabledRuleCount 条规则",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenRules
                )
                OverviewSettingRow(
                    icon = Icons.Filled.Schedule,
                    label = "免打扰时段",
                    value = if (activeWindowEnabled) "$activeWindowStart - $activeWindowEnd" else "未启用",
                    tint = if (activeWindowEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onOpenReminderSettings
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "通知状态诊断",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (pendingCount == 0) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            if (pendingCount == 0) "状态正常" else "$pendingCount 项待处理",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (pendingCount == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                OverviewDiagnosticRow("短信权限", smsPermissionGranted, "去授权", onRequestSmsPermission)
                OverviewDiagnosticRow("通知访问", notificationAccessGranted, "去设置", onOpenNotificationSettings)
                OverviewDiagnosticRow("通知权限", notificationPermissionGranted, "去设置", onRequestNotificationPermission)
                OverviewDiagnosticRow("全屏提醒", fullScreenIntentGranted, "去设置", onFullScreenIntent)
                OverviewDiagnosticRow("精确闹钟", exactAlarmGranted, "去设置", onExactAlarm)
                OverviewDiagnosticRow("电池优化豁免", batteryOptimizationIgnored, "去设置", onBatteryOptimization)
                OverviewDiagnosticRow("振动权限", vibrationGranted, "去设置", onBackgroundSettings)
                if (pendingCount > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
                    ) {
                        Text(
                            "后台运行受限时，提醒可能延迟 1-2 分钟送达。",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = onClick?.let { callback -> Modifier.clickable(onClick = callback) } ?: Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
private fun OverviewDiagnosticRow(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "•",
            color = if (granted) AppSuccess else MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        if (granted) {
            Text("正常", style = MaterialTheme.typography.labelMedium, color = AppSuccess)
        } else {
            TextButton(
                onClick = onAction,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun SettingsStatusCard(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    ready: Boolean,
    warningText: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (ready) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (ready) "已开启" else "需处理",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (ready) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            if (!ready) {
                Text(
                    warningText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            content?.invoke()
            if (!ready && actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun RuleManagementCard(
    rules: List<SmsRule>,
    onRulesChanged: (List<SmsRule>) -> Unit
) {
    val context = LocalContext.current
    var editingId by remember { mutableStateOf<String?>(null) }
    var rulesExpanded by remember { mutableStateOf(true) }
    val rulesRotation by animateFloatAsState(
        targetValue = if (rulesExpanded) 180f else 0f,
        label = "rulesExpand"
    )
    var name by remember { mutableStateOf("") }
    var senders by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(RuleMatchMode.ANY) }
    var enabled by remember { mutableStateOf(true) }

    fun edit(rule: SmsRule) {
        editingId = rule.id
        name = rule.name
        senders = rule.senders
        keywords = rule.keywords
        mode = rule.mode
        enabled = rule.enabled
    }

    fun resetEditor() {
        editingId = "new"
        name = ""
        senders = ""
        keywords = ""
        mode = RuleMatchMode.ANY
        enabled = true
    }

    fun save() {
        if (senders.isBlank() && keywords.isBlank()) {
            Toast.makeText(context, "至少填写一个号码或关键词", Toast.LENGTH_SHORT).show()
            return
        }
        val rule = SmsRule(
            id = if (editingId == "new" || editingId == null) java.util.UUID.randomUUID().toString() else editingId!!,
            name = name.trim().ifBlank { "未命名规则" },
            senders = senders.trim(),
            keywords = keywords.trim(),
            mode = mode,
            enabled = enabled
        )
        val updated = if (editingId == "new" || editingId == null) rules + rule
        else rules.map { if (it.id == rule.id) rule else it }
        onRulesChanged(updated)
        editingId = null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("匹配规则", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "每条规则独立启用，可按任一条件或全部条件匹配",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = ::resetEditor) { Text("新增规则") }
                IconButton(onClick = { rulesExpanded = !rulesExpanded }) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = if (rulesExpanded) "收起规则列表" else "展开规则列表",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rulesRotation)
                    )
                }
            }

            if (rulesExpanded) {
                if (rules.isEmpty()) {
                    Text(
                        "暂无规则，点击右上角新增规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    rules.forEach { rule ->
                        RuleListItem(
                            rule = rule,
                            onToggle = { value ->
                                onRulesChanged(rules.map { if (it.id == rule.id) it.copy(enabled = value) else it })
                            },
                            onEdit = { edit(rule) },
                            onDelete = {
                                onRulesChanged(rules.filterNot { it.id == rule.id })
                                if (editingId == rule.id) editingId = null
                            }
                        )
                    }
                }
            }
        }
    }

    if (editingId != null) {
        AlertDialog(
            onDismissRequest = { editingId = null },
            title = { Text(if (editingId == "new") "新增匹配规则" else "编辑匹配规则") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("规则名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = senders,
                        onValueChange = { senders = it },
                        label = { Text("目标号码") },
                        placeholder = { Text("多个号码用逗号分隔") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text("关键词") },
                        placeholder = { Text("多个关键词用逗号分隔") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { mode = RuleMatchMode.ANY },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (mode == RuleMatchMode.ANY) "✓ 任一命中" else "任一命中")
                        }
                        OutlinedButton(
                            onClick = { mode = RuleMatchMode.ALL },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (mode == RuleMatchMode.ALL) "✓ 全部命中" else "全部命中")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("启用此规则", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            },
            confirmButton = { Button(onClick = ::save) { Text("保存") } },
            dismissButton = { TextButton(onClick = { editingId = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun RuleListItem(
    rule: SmsRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val revealDistance = with(density) { 92.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.82f))
                .padding(end = 14.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDelete) {
                Text("删除", color = androidx.compose.ui.graphics.Color.White)
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-revealDistance, 0f)
                        },
                        onDragEnd = {
                            offsetX = if (offsetX < -revealDistance * 0.45f) -revealDistance else 0f
                        }
                    )
                },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "号码: ${rule.senders.ifBlank { "不限" }} · 关键词: ${rule.keywords.ifBlank { "不限" }} · " +
                            if (rule.mode == RuleMatchMode.ANY) "任一命中" else "全部命中",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onEdit,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("编辑", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
        }
    }
}

@Composable
private fun CustomPlaybackSwitchRow(
    enabled: Boolean,
    sourceMode: PlaybackSourceMode,
    duration: String,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("短信匹配后使用自定义播放", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (enabled) {
                        "已启用 · ${if (sourceMode == PlaybackSourceMode.LOCAL) "本地音频" else "线上音频"} · " +
                            formatDurationLabel(duration.toIntOrNull()?.coerceIn(1, 60) ?: 35)
                    } else {
                        "未启用 · 匹配成功后继续写入系统闹钟"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) AppSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun TimeWindowCard(
    enabled: Boolean,
    start: String,
    end: String,
    onEnabledChange: (Boolean) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("提醒时间段", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("仅在指定时间段内匹配并提醒，支持跨午夜时段", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
            if (enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = onStartChange,
                        label = { Text("开始 HH:mm") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = onEndChange,
                        label = { Text("结束 HH:mm") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterOptionsCard(
    blacklist: String,
    onBlacklistChange: (String) -> Unit,
    ignoreCarrierVerification: Boolean,
    onIgnoreCarrierChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("过滤选项", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = blacklist,
                onValueChange = onBlacklistChange,
                label = { Text("黑名单号码") },
                placeholder = { Text("多个号码用逗号分隔，命中后不提醒") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("忽略运营商验证码", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "仅过滤 10086、10010、10000 等运营商号码的验证码短信",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = ignoreCarrierVerification, onCheckedChange = onIgnoreCarrierChange)
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String =
    "%02d:%02d".format(minutes / 60, minutes % 60)

private fun parseMinutes(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun openSettings(context: android.content.Context, action: String, data: Uri? = null) {
    try {
        context.startActivity(Intent(action).apply {
            data?.let { this.data = it }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (t: Throwable) {
        Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show()
        EventLog.append(context, "Open settings failed action=$action error=${t.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSettingSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    minLabel: String,
    maxLabel: String
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        activeTickColor = MaterialTheme.colorScheme.primary,
        inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                valueLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = sliderColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(horizontal = 2.dp),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = sliderColors,
                    thumbSize = DpSize(18.dp, 18.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = sliderColors,
                    modifier = Modifier.height(4.dp)
                )
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(minLabel, style = MaterialTheme.typography.labelSmall)
            Text(maxLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatDurationLabel(seconds: Int): String = when {
    seconds > 60 -> "${seconds / 60}分${seconds % 60}秒"
    else -> "$seconds 秒"
}

@Composable
private fun CustomPlaybackCard(
    source: String,
    sourceMode: PlaybackSourceMode,
    onPickLocalSource: () -> Unit,
    onSourceModeChange: (PlaybackSourceMode) -> Unit,
    onSourceChange: (String) -> Unit,
    volume: String,
    onVolumeChange: (String) -> Unit,
    duration: String,
    onDurationChange: (String) -> Unit,
    vibrate: Boolean,
    onVibrateChange: (Boolean) -> Unit,
    onSchedule: () -> Unit
) {
    var showCustomConfig by remember { mutableStateOf(false) }
    val expandRotation by animateFloatAsState(
        targetValue = if (showCustomConfig) 180f else 0f,
        label = "customPlaybackExpand"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomConfig = !showCustomConfig },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自定义播放配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "使用系统精确定时，到点由本应用播放音乐",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (showCustomConfig) "收起自定义播放配置" else "展开自定义播放配置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(expandRotation)
                )
            }
            if (showCustomConfig) {
                TabRow(
                    selectedTabIndex = if (sourceMode == PlaybackSourceMode.LOCAL) 0 else 1,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = sourceMode == PlaybackSourceMode.LOCAL,
                        onClick = { onSourceModeChange(PlaybackSourceMode.LOCAL) },
                        text = { Text("本地音频", style = MaterialTheme.typography.labelLarge) }
                    )
                    Tab(
                        selected = sourceMode == PlaybackSourceMode.ONLINE,
                        onClick = { onSourceModeChange(PlaybackSourceMode.ONLINE) },
                        text = { Text("线上音频", style = MaterialTheme.typography.labelLarge) }
                    )
                }
                if (sourceMode == PlaybackSourceMode.ONLINE) {
                    OutlinedTextField(
                        value = source,
                        onValueChange = onSourceChange,
                        label = { Text("线上音频地址") },
                        placeholder = { Text("http(s):// 音频地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = source,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("本地音频路径") },
                        supportingText = { Text("默认使用内置提醒音，也可以选择手机中的音乐文件") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = onPickLocalSource) {
                                Icon(
                                    imageVector = Icons.Filled.FolderOpen,
                                    contentDescription = "选择本地音频",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
                PlaybackSettingSlider(
                    icon = Icons.Filled.VolumeUp,
                    title = "系统闹钟音量",
                    valueLabel = "${volume.toIntOrNull()?.coerceIn(0, 100) ?: 10}%",
                    value = volume.toFloatOrNull()?.coerceIn(0f, 100f) ?: 10f,
                    valueRange = 0f..100f,
                    steps = 99,
                    onValueChange = { onVolumeChange(it.roundToInt().toString()) },
                    minLabel = "静音",
                    maxLabel = "最大"
                )
                PlaybackSettingSlider(
                    icon = Icons.Filled.Timer,
                    title = "播放时长",
                    valueLabel = formatDurationLabel(duration.toIntOrNull()?.coerceIn(1, 60) ?: 35),
                    value = duration.toFloatOrNull()?.coerceIn(1f, 60f) ?: 35f,
                    valueRange = 1f..60f,
                    steps = 58,
                    onValueChange = {
                        val seconds = it.roundToInt().coerceIn(1, 60)
                        onDurationChange(seconds.toString())
                    },
                    minLabel = "1 秒",
                    maxLabel = "60 秒"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("播放时振动", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = vibrate, onCheckedChange = onVibrateChange)
                }
                Button(
                    onClick = onSchedule,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("5 秒后自定义闹钟播放")
                }
            }
        }
    }
}

@Composable
private fun EventLogCard(
    text: String,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    val expandRotation by animateFloatAsState(
        targetValue = if (detailsExpanded) 180f else 0f,
        label = "eventLogExpand"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "运行日志",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { detailsExpanded = !detailsExpanded }) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (detailsExpanded) "收起日志详情" else "展开日志详情",
                        modifier = Modifier.rotate(expandRotation)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新日志")
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "复制日志")
                }
                IconButton(onClick = onSave) {
                    Icon(Icons.Filled.Save, contentDescription = "保存日志")
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Delete, contentDescription = "清空日志")
                }
            }
            if (detailsExpanded) {
                Text(
                    text = text.ifBlank { "暂无日志" },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = if (text.isBlank()) "暂无日志" else "最近 50 条，最新记录在上 · 点击箭头查看详情",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusCard(enabled: Boolean, rules: List<SmsRule>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (enabled) "监听已开启" else "监听已关闭",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (rules.isEmpty()) {
                Text("未配置匹配规则", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = "已启用 ${rules.count { it.enabled }} 条规则，支持号码/关键词和任一/全部匹配",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    var showFeatureGuide by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    val expandRotation by animateFloatAsState(
        targetValue = if (detailsExpanded) 180f else 0f,
        label = "infoExpand"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "使用说明与限制",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { detailsExpanded = !detailsExpanded }
                )
                TextButton(onClick = { showFeatureGuide = true }) {
                    Text("功能介绍")
                }
                IconButton(onClick = { detailsExpanded = !detailsExpanded }) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = if (detailsExpanded) "收起使用说明" else "展开使用说明",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(expandRotation)
                    )
                }
            }
            if (detailsExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                BulletLine("规则支持多个号码和关键词,每条规则可选择任一或全部命中")
                BulletLine("号码自动去除 +86 / 空格 / - 等符号后再匹配")
                BulletLine("启用时间段后,仅在指定时段内进行匹配提醒")
                BulletLine("短信广播和短信通知是两条独立监听通道")
                BulletLine("自定义播放按设置临时调整系统闹钟音量,提醒结束后恢复原音量")
                BulletLine("锁屏/全屏提醒依赖系统通知、全屏提醒和电池策略权限")
                BulletLine("本应用仅处理当前手机收到的新短信,不会扫描历史短信")
                BulletLine("短信内容和手机号仅保存在本机,不会上传服务器")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "vivo 后台白名单 (收不到短信时检查)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                BulletLine("i 管家 → 应用管理 → 短信提醒助手 → 自启动 → 开")
                BulletLine("i 管家 → 应用管理 → 短信提醒助手 → 后台高耗电 → 允许")
                BulletLine("设置 → 应用 → 短信提醒助手 → 通知 → 允许通知")
                BulletLine("设置 → 电池 → 后台耗电管理 → 短信提醒助手 → 无限制")
            }
        }
    }
    if (showFeatureGuide) {
        AlertDialog(
            onDismissRequest = { showFeatureGuide = false },
            title = { Text("短信提醒助手功能介绍") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GuideSection(
                        title = "一、首次配置",
                        body = "打开概览页，依次授予短信监听、通知、全屏提醒、振动和闹钟提醒权限。部分手机还需要开启自启动和电池后台运行。"
                    )
                    GuideSection(
                        title = "二、添加匹配规则",
                        body = "进入匹配规则，点击“新增规则”，填写发件人号码或短信关键词。每条规则可以选择“任一命中”或“全部命中”，保存后开启规则开关。"
                    )
                    GuideSection(
                        title = "三、设置提醒方式",
                        body = "进入提醒设置，选择本地音频或线上地址，调整系统闹钟音量、播放时长和振动。自定义闹钟播放开启后，匹配短信会直接使用应用播放服务提醒。"
                    )
                    GuideSection(
                        title = "四、查看运行日志",
                        body = "进入运行日志，点击“刷新”查看最新链路；可复制日志用于排查短信广播、通知监听、规则匹配和闹钟播放问题，也可以保存日志文件。"
                    )
                    GuideSection(
                        title = "五、重要说明",
                        body = "应用只处理本机新收到的短信，不读取其他设备短信。权限被关闭、后台进程被清理或系统限制后台运行时，可能无法及时监听和提醒。"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFeatureGuide = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SmsPermissionCard(granted: Boolean, onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "短信监听权限",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (granted)
                    "✓ 已允许监听新短信 — 无需设为默认短信 App"
                else
                    "✗ 未允许监听短信 — 请授权 RECEIVE_SMS, 应用才能监听系统收到的新短信",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!granted) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("授予短信读取权限")
                }
            }
        }
    }
}

@Composable
private fun NotificationAccessCard(
    granted: Boolean,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "通知栏监听（备用通道）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (granted)
                    "✓ 已允许读取通知栏 — 可从短信通知中提取内容"
                else
                    "✗ 未允许读取通知栏 — 短信广播不可用时，无法使用备用监听通道",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (granted) "打开设置" else "允许通知访问")
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("刷新状态")
                }
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
