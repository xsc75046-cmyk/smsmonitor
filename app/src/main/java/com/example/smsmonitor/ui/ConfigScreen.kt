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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var showSystemDetails by remember { mutableStateOf(false) }
    val tabs = listOf("概览", "匹配规则", "提醒设置", "运行日志")
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
        val duration = playbackDuration.toIntOrNull()?.coerceIn(1, 3600)
        if (playbackSource.isBlank() || volume == null || duration == null) {
            Toast.makeText(context, "请填写有效的音乐地址、音量(0-100)和时长(1-3600秒)", Toast.LENGTH_LONG).show()
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
        topBar = {
            Column {
                TopAppBar(title = { Text("短信提醒助手") })
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) }
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
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    val coreReadyCount = listOf(
                        notificationPermissionGranted,
                        fullScreenIntentGranted,
                        exactAlarmGranted,
                        vibrationGranted
                    ).count { it }
                    OverviewGrid(
                        monitoringEnabled = monitoringEnabled,
                        enabledRuleCount = rules.count { it.enabled },
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
                        coreReadyCount = coreReadyCount,
                        corePermissionsReady = coreReadyCount == 4,
                        onOpenSystemPermissions = { showSystemDetails = true }
                    )
                    if (showSystemDetails) {
                        SystemAccessCard(
                            notificationGranted = notificationPermissionGranted,
                            fullScreenIntentGranted = fullScreenIntentGranted,
                            exactAlarmGranted = exactAlarmGranted,
                            batteryOptimizationIgnored = batteryOptimizationIgnored,
                            vibrationGranted = vibrationGranted,
                            onNotificationPermission = onRequestNotificationPermission,
                            onFullScreenIntent = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    openSettings(context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                        Uri.parse("package:${context.packageName}"))
                                }
                            },
                            onExactAlarm = {
                                openSettings(context, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"))
                            },
                            onBatteryOptimization = {
                                openSettings(context, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}"))
                            },
                            onBackgroundSettings = {
                                openSettings(context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}"))
                            }
                        )
                        OutlinedButton(
                            onClick = { showSystemDetails = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("收起系统权限详情")
                        }
                    }
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
                        customOnMatch = customPlaybackOnMatch,
                        onCustomOnMatchChange = {
                            customPlaybackOnMatch = it
                            if (loaded) repo.customPlaybackOnMatch = it
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
                    InfoCard()
                }
            }
        }
    }
}

@Composable
private fun OverviewGrid(
    monitoringEnabled: Boolean,
    enabledRuleCount: Int,
    onMonitoringChange: (Boolean) -> Unit,
    smsPermissionGranted: Boolean,
    onRequestSmsPermission: () -> Unit,
    notificationAccessGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    coreReadyCount: Int,
    corePermissionsReady: Boolean,
    onOpenSystemPermissions: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewTile(
                modifier = Modifier.weight(1f),
                title = "监听状态",
                summary = if (monitoringEnabled) "已启用 $enabledRuleCount 条规则" else "已暂停自动提醒",
                icon = if (monitoringEnabled) Icons.Filled.CheckCircle else Icons.Filled.HighlightOff,
                active = monitoringEnabled
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("自动监听", style = MaterialTheme.typography.labelLarge)
                    Switch(checked = monitoringEnabled, onCheckedChange = onMonitoringChange)
                }
            }
            OverviewTile(
                modifier = Modifier.weight(1f),
                title = "短信权限",
                summary = if (smsPermissionGranted) "可以接收新短信" else "需要授权后才能监听",
                icon = if (smsPermissionGranted) Icons.Filled.CheckCircle else Icons.Filled.HighlightOff,
                active = smsPermissionGranted
            ) {
                if (!smsPermissionGranted) {
                    OutlinedButton(
                        onClick = onRequestSmsPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("去授权") }
                } else {
                    Text("已准备就绪", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewTile(
                modifier = Modifier.weight(1f),
                title = "备用通道",
                summary = if (notificationAccessGranted) "短信通知监听已开启" else "建议开启，提升到达率",
                icon = Icons.Filled.Schedule,
                active = notificationAccessGranted
            ) {
                OutlinedButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (notificationAccessGranted) "管理设置" else "去开启") }
            }
            OverviewTile(
                modifier = Modifier.weight(1f),
                title = "系统权限",
                summary = "$coreReadyCount / 4 项核心权限已就绪",
                icon = Icons.Filled.Alarm,
                active = corePermissionsReady,
                warning = !corePermissionsReady
            ) {
                OutlinedButton(
                    onClick = onOpenSystemPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("查看详情") }
            }
        }
    }
}

@Composable
private fun OverviewTile(
    modifier: Modifier,
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    warning: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.height(178.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (warning) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "有权限未开启",
                        tint = androidx.compose.ui.graphics.Color(0xFFD32F2F)
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.weight(1f))
            content()
        }
    }
}

@Composable
private fun ListenerSwitchCard(
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("短信自动提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (enabled) "正在监测新短信并按规则提醒" else "已暂停监测，历史短信不会被扫描",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SystemAccessCard(
    notificationGranted: Boolean,
    fullScreenIntentGranted: Boolean,
    exactAlarmGranted: Boolean,
    batteryOptimizationIgnored: Boolean,
    vibrationGranted: Boolean,
    onNotificationPermission: () -> Unit,
    onFullScreenIntent: () -> Unit,
    onExactAlarm: () -> Unit,
    onBatteryOptimization: () -> Unit,
    onBackgroundSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("核心提醒权限与后台运行", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            PermissionRow("通知权限", notificationGranted, onNotificationPermission)
            PermissionRow("全屏提醒", fullScreenIntentGranted, onFullScreenIntent)
            PermissionRow("闹钟提醒权限", exactAlarmGranted, onExactAlarm)
            PermissionRow("电池优化豁免", batteryOptimizationIgnored, onBatteryOptimization)
            PermissionRow("振动权限", vibrationGranted, null)
            OutlinedButton(onClick = onBackgroundSettings, modifier = Modifier.fillMaxWidth()) {
                Text("打开应用后台设置")
            }
            Text(
                "建议开启自启动、允许后台运行，并将电池策略设为不限制，否则系统清理后台后可能无法提醒。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(if (granted) "✓ $label" else "✗ $label", style = MaterialTheme.typography.bodyMedium)
        if (!granted && onRequest != null) {
            OutlinedButton(onClick = onRequest) { Text("去开启") }
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("匹配规则", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("每条规则独立启用，可按任一条件或全部条件匹配", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = ::resetEditor) { Text("新增规则") }
            }

            rules.forEach { rule ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "号码: ${rule.senders.ifBlank { "不限" }} · 关键词: ${rule.keywords.ifBlank { "不限" }} · " +
                                    if (rule.mode == RuleMatchMode.ANY) "任一命中" else "全部命中",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { value ->
                                onRulesChanged(rules.map { if (it.id == rule.id) it.copy(enabled = value) else it })
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { edit(rule) },
                            modifier = Modifier.weight(1f)
                        ) { Text("编辑") }
                        OutlinedButton(
                            onClick = {
                                onRulesChanged(rules.filterNot { it.id == rule.id })
                                if (editingId == rule.id) editingId = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("删除") }
                    }
                }
            }

            if (editingId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (editingId == "new") "新建规则" else "编辑规则",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
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
                    Text("启用此规则")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = ::save) { Text("保存规则") }
                    OutlinedButton(onClick = { editingId = null }) { Text("取消") }
                }
            }
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("提醒时间段", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("过滤选项", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
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
    seconds >= 60 -> "${seconds / 60}分${seconds % 60}秒"
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
    customOnMatch: Boolean,
    onCustomOnMatchChange: (Boolean) -> Unit,
    onSchedule: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "自定义闹钟播放",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "使用系统精确定时，到点由本应用播放音乐，不写入系统闹钟 App。",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("音频来源", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (sourceMode == PlaybackSourceMode.LOCAL) "本地音频文件" else "线上音频地址",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sourceMode == PlaybackSourceMode.ONLINE,
                    onCheckedChange = {
                        onSourceModeChange(if (it) PlaybackSourceMode.ONLINE else PlaybackSourceMode.LOCAL)
                    }
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
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onPickLocalSource,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("选择本地音乐文件")
                }
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
                valueLabel = formatDurationLabel(duration.toIntOrNull()?.coerceIn(1, 3600) ?: 30),
                value = duration.toFloatOrNull()?.coerceIn(1f, 3600f) ?: 30f,
                valueRange = 1f..3600f,
                steps = 359,
                onValueChange = {
                    val seconds = (it.roundToInt() / 10 * 10).coerceIn(1, 3600)
                    onDurationChange(seconds.toString())
                },
                minLabel = "1 秒",
                maxLabel = "60 分钟"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("短信匹配后使用自定义播放", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (customOnMatch) "匹配成功后走应用播放服务" else "匹配成功后继续写入系统闹钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = customOnMatch, onCheckedChange = onCustomOnMatchChange)
            }
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

@Composable
private fun EventLogCard(
    text: String,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "运行日志（最近 50 条，最新在上）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.width(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新")
                }
                OutlinedButton(
                    onClick = onCopy,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.width(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }
                OutlinedButton(
                    onClick = onSave,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.width(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onClear,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.width(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("清空日志")
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (text.isBlank()) {
                Text(
                    text = "(暂无日志,点上面按钮试试)",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "使用说明与限制",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = { showFeatureGuide = true }) {
                    Text("功能介绍")
                }
            }
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
