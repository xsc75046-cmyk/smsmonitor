package com.example.smsmonitor.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class RuleMatchMode { ANY, ALL }

enum class PlaybackSourceMode { LOCAL, ONLINE }

data class SmsRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val senders: String,
    val keywords: String,
    val mode: RuleMatchMode = RuleMatchMode.ANY,
    val enabled: Boolean = true
)

class ConfigRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    var monitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var senderFilter: String
        get() = prefs.getString(KEY_SENDER, DEFAULT_SENDER_FILTER).orEmpty()
        set(value) = prefs.edit().putString(KEY_SENDER, value).apply()

    var keywordFilter: String
        get() = prefs.getString(KEY_KEYWORD, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_KEYWORD, value).apply()

    var playbackSourceMode: PlaybackSourceMode
        get() {
            val stored = prefs.getString(KEY_PLAYBACK_SOURCE_MODE, null)
            if (!stored.isNullOrBlank()) {
                return runCatching { PlaybackSourceMode.valueOf(stored) }
                    .getOrDefault(PlaybackSourceMode.LOCAL)
            }
            val legacy = prefs.getString(KEY_PLAYBACK_SOURCE, null)
            return when {
                legacy?.startsWith("android.resource://") == true -> PlaybackSourceMode.LOCAL
                !legacy.isNullOrBlank() -> PlaybackSourceMode.ONLINE
                else -> PlaybackSourceMode.LOCAL
            }
        }
        set(value) = prefs.edit().putString(KEY_PLAYBACK_SOURCE_MODE, value.name).apply()

    var playbackSource: String
        get() {
            val legacy = prefs.getString(KEY_PLAYBACK_SOURCE, null)
            return when (playbackSourceMode) {
                PlaybackSourceMode.LOCAL -> prefs.getString(KEY_LOCAL_PLAYBACK_SOURCE, null)
                    ?: legacy?.takeIf { it.startsWith("android.resource://") }
                    ?: DEFAULT_LOCAL_PLAYBACK_SOURCE
                PlaybackSourceMode.ONLINE -> prefs.getString(KEY_ONLINE_PLAYBACK_SOURCE, null)
                    ?: legacy?.takeIf { it.isNotBlank() && !it.startsWith("android.resource://") }
                    ?: DEFAULT_ONLINE_PLAYBACK_SOURCE
            }
        }
        set(value) {
            val editor = prefs.edit().putString(KEY_PLAYBACK_SOURCE, value)
            if (playbackSourceMode == PlaybackSourceMode.LOCAL) {
                editor.putString(KEY_LOCAL_PLAYBACK_SOURCE, value)
            } else {
                editor.putString(KEY_ONLINE_PLAYBACK_SOURCE, value)
            }
            editor.apply()
        }

    var playbackVolume: Int
        get() {
            val stored = prefs.getInt(KEY_PLAYBACK_VOLUME, 10).coerceIn(0, 100)
            if (!prefs.getBoolean(KEY_PLAYBACK_VOLUME_MIGRATED, false)) {
                val migrated = if (!prefs.contains(KEY_PLAYBACK_VOLUME) || stored == 100) 10 else stored
                prefs.edit()
                    .putInt(KEY_PLAYBACK_VOLUME, migrated)
                    .putBoolean(KEY_PLAYBACK_VOLUME_MIGRATED, true)
                    .apply()
                return migrated
            }
            return stored
        }
        set(value) = prefs.edit().putInt(KEY_PLAYBACK_VOLUME, value.coerceIn(0, 100)).apply()

    var playbackDurationSeconds: Int
        get() = prefs.getInt(KEY_PLAYBACK_DURATION, 30).coerceIn(1, 3600)
        set(value) = prefs.edit().putInt(KEY_PLAYBACK_DURATION, value.coerceIn(1, 3600)).apply()

    var customPlaybackOnMatch: Boolean
        get() = prefs.getBoolean(KEY_CUSTOM_ON_MATCH, true)
        set(value) = prefs.edit().putBoolean(KEY_CUSTOM_ON_MATCH, value).apply()

    var playbackVibrate: Boolean
        get() = prefs.getBoolean(KEY_PLAYBACK_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_PLAYBACK_VIBRATE, value).apply()

    var activeWindowEnabled: Boolean
        get() = prefs.getBoolean(KEY_WINDOW_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_WINDOW_ENABLED, value).apply()

    var activeWindowStart: Int
        get() = prefs.getInt(KEY_WINDOW_START, 22 * 60)
        set(value) = prefs.edit().putInt(KEY_WINDOW_START, value.coerceIn(0, 1439)).apply()

    var activeWindowEnd: Int
        get() = prefs.getInt(KEY_WINDOW_END, 7 * 60)
        set(value) = prefs.edit().putInt(KEY_WINDOW_END, value.coerceIn(0, 1439)).apply()

    var rules: List<SmsRule>
        get() = readRules()
        set(value) = prefs.edit().putString(KEY_RULES, rulesToJson(value)).apply()

    var blacklistSenders: String
        get() = prefs.getString(KEY_BLACKLIST, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_BLACKLIST, value).apply()

    var ignoreCarrierVerification: Boolean
        get() = prefs.getBoolean(KEY_IGNORE_CARRIER, false)
        set(value) = prefs.edit().putBoolean(KEY_IGNORE_CARRIER, value).apply()

    /**
     * Check whether the received SMS matches the configured rules.
     * Phone numbers are normalized (strip +, spaces, dashes) before comparison
     * so users can paste "+86 138-0013-8000" while real SMS arrives as
     * "+8613800138000" and still matches.
     * Sender and keyword filters accept comma-separated multiple values;
     * any value matching wins (OR logic).
     */
    fun matches(sender: String, body: String): Boolean {
        if (!monitoringEnabled) return false
        if (!isWithinActiveWindow()) return false
        if (blacklistSenders.splitRules().any { sender.normalizePhone().contains(it.normalizePhone(), true) }) {
            return false
        }
        if (ignoreCarrierVerification && isCarrierVerification(sender, body)) return false
        return rules.filter { it.enabled }.any { rule ->
            val senderValues = rule.senders.splitRules()
            val keywordValues = rule.keywords.splitRules()
            if (senderValues.isEmpty() && keywordValues.isEmpty()) return@any false

            val normalizedSender = sender.normalizePhone()
            val senderMatched = senderValues.any { needle ->
                normalizedSender.contains(needle.normalizePhone(), ignoreCase = true)
            }
            val keywordMatched = keywordValues.any { needle ->
                body.contains(needle, ignoreCase = true)
            }
            when (rule.mode) {
                RuleMatchMode.ANY -> senderMatched || keywordMatched
                RuleMatchMode.ALL ->
                    (senderValues.isEmpty() || senderMatched) &&
                        (keywordValues.isEmpty() || keywordMatched)
            }
        }
    }

    private fun isCarrierVerification(sender: String, body: String): Boolean {
        val normalizedSender = sender.filter { it.isDigit() }
        if (normalizedSender !in CARRIER_SENDERS) return false
        return body.contains("验证码", ignoreCase = true) ||
            body.contains("校验码", ignoreCase = true)
    }

    fun isWithinActiveWindow(nowMinutes: Int = currentMinutes()): Boolean {
        if (!activeWindowEnabled) return true
        val start = activeWindowStart
        val end = activeWindowEnd
        return if (start == end) true
        else if (start < end) nowMinutes in start until end
        else nowMinutes >= start || nowMinutes < end
    }

    private fun readRules(): List<SmsRule> {
        val raw = prefs.getString(KEY_RULES, null)
        if (raw.isNullOrBlank()) {
            val legacySender = senderFilter
            val legacyKeyword = keywordFilter
            return if (legacySender.isNotBlank() || legacyKeyword.isNotBlank()) {
                listOf(SmsRule(
                    id = "legacy-default",
                    name = "默认规则",
                    senders = legacySender,
                    keywords = legacyKeyword
                ))
            } else emptyList()
        }
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    add(SmsRule(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = item.optString("name", "未命名规则"),
                        senders = item.optString("senders"),
                        keywords = item.optString("keywords"),
                        mode = runCatching { RuleMatchMode.valueOf(item.optString("mode", "ANY")) }
                            .getOrDefault(RuleMatchMode.ANY),
                        enabled = item.optBoolean("enabled", true)
                    ))
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun rulesToJson(value: List<SmsRule>): String = JSONArray().apply {
        value.forEach { rule ->
            put(JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("senders", rule.senders)
                put("keywords", rule.keywords)
                put("mode", rule.mode.name)
                put("enabled", rule.enabled)
            })
        }
    }.toString()

    private fun currentMinutes(): Int {
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(java.util.Calendar.MINUTE)
    }

    private fun String.splitRules(): List<String> =
        split(',', '，', '\n', '\t')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun String.normalizePhone(): String =
        replace("+","").replace(" ", "").replace("-", "").replace("(", "").replace(")", "")

    companion object {
        private const val PREFS_NAME = "sms_monitor_prefs"
        private const val KEY_ENABLED = "monitoring_enabled"
        private const val KEY_SENDER = "sender_filter"
        private const val KEY_KEYWORD = "keyword_filter"
        private const val KEY_PLAYBACK_SOURCE = "playback_source"
        private const val KEY_PLAYBACK_SOURCE_MODE = "playback_source_mode"
        private const val KEY_LOCAL_PLAYBACK_SOURCE = "playback_local_source"
        private const val KEY_ONLINE_PLAYBACK_SOURCE = "playback_online_source"
        private const val KEY_PLAYBACK_VOLUME = "playback_volume"
        private const val KEY_PLAYBACK_VOLUME_MIGRATED = "playback_volume_default_migrated"
        private const val KEY_PLAYBACK_DURATION = "playback_duration"
        private const val KEY_CUSTOM_ON_MATCH = "custom_playback_on_match"
        private const val KEY_PLAYBACK_VIBRATE = "playback_vibrate"
        private const val KEY_RULES = "sms_rules"
        private const val KEY_WINDOW_ENABLED = "active_window_enabled"
        private const val KEY_WINDOW_START = "active_window_start"
        private const val KEY_WINDOW_END = "active_window_end"
        private const val KEY_BLACKLIST = "blacklist_senders"
        private const val KEY_IGNORE_CARRIER = "ignore_carrier_verification"
        const val DEFAULT_SENDER_FILTER = "121234101"
        const val DEFAULT_LOCAL_PLAYBACK_SOURCE = "android.resource://com.example.smsmonitor/raw/xiaoxin_tixing"
        const val DEFAULT_ONLINE_PLAYBACK_SOURCE = "http://music.163.com/song/media/outer/url?id=447925558.mp3"
        private val CARRIER_SENDERS = setOf(
            "10086", "10010", "10000", "10001", "10011", "10085", "1008611"
        )
    }
}
