@file:Suppress("SpellCheckingInspection", "UNCHECKED_CAST")

package dev.sunls24.sbv.util

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.sunls24.biliapi.http.util.generateBuvid
import dev.sunls24.sbv.SBVApp
import dev.sunls24.sbv.component.HomeTopNavItem
import dev.sunls24.sbv.component.PersonalTopNavItem
import dev.sunls24.sbv.component.controllers.DanmakuType
import dev.sunls24.sbv.entity.Audio
import dev.sunls24.sbv.entity.AuthData
import dev.sunls24.sbv.entity.PlaybackEndAction
import dev.sunls24.sbv.entity.PlaybackSpeed
import dev.sunls24.sbv.entity.Resolution
import dev.sunls24.sbv.entity.VideoCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Prefs {
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())
    private val flowMap = ConcurrentHashMap<Preferences.Key<*>, MutableStateFlow<Any?>>()
    private val initMutex = Mutex()
    private var initialized = false

    /**
     * 基本类型委托 (String, Int, Boolean, Float, Long)
     */
    private fun <T> pref(key: Preferences.Key<T>, default: T): PrefDelegate<T, T> {
        return PrefDelegate(key, default, flowMap)
    }

    /**
     * 对象映射委托 (Enum、Dp 等)
     * @param save 转换成基本类型存入 DataStore
     * @param restore 从 DataStore 的基本类型还原为对象
     */
    private fun <T, P> pref(
        key: Preferences.Key<P>,
        default: T,
        save: (T) -> P,
        restore: (P) -> T
    ): PrefDelegate<T, P> {
        return PrefDelegate(key, default, flowMap, save, restore)
    }

    // =========================================================================
    // 账号 & 认证
    // =========================================================================

    private val authDataPref = pref(
        PrefKeys.prefAuthDataKey,
        null as AuthData?,
        save = { it?.let { value -> Json.encodeToString(value) }.orEmpty() },
        restore = { value ->
            value.takeIf(String::isNotEmpty)?.let {
                runCatching { Json.decodeFromString<AuthData>(it) }.getOrNull()
            }
        }
    )
    val authData by authDataPref
    var buvid by pref(PrefKeys.prefBuvidKey, "")
    var buvid3 by pref(PrefKeys.prefBuvid3Key, "")
    var username by pref(PrefKeys.prefUsernameKey, "")
    var avatar by pref(PrefKeys.prefAvatarKey, "")
    var searchHistoryJson by pref(PrefKeys.prefSearchHistoryKey, "[]")

    // =========================================================================
    // 播放器 - 视频
    // =========================================================================

    var defaultQuality by pref(
        PrefKeys.prefDefaultQualityKey,
        Resolution.R1080P,
        save = { it.code },
        restore = { Resolution.fromCode(it) }
    )
    var defaultVideoCodec by pref(
        PrefKeys.prefDefaultVideoCodecKey,
        VideoCodec.AVC,
        save = { it.ordinal },
        restore = { VideoCodec.fromCode(it) }
    )
    var enableSoftwareVideoDecoder by pref(PrefKeys.prefEnableSoftwareVideoDecoder, false)
    var actionAfterPlay by pref(
        PrefKeys.prefActionAfterPlayKey,
        PlaybackEndAction.PlayNext,
        save = { it.code },
        restore = { PlaybackEndAction.fromCode(it) }
    )

    // =========================================================================
    // 播放器 - 音频
    // =========================================================================

    var defaultAudio by pref(
        PrefKeys.prefDefaultAudioKey,
        Audio.A192K,
        save = { it.code },
        restore = { Audio.fromCode(it) }
    )

    // =========================================================================
    // 播放器 - 弹幕
    // =========================================================================

    var defaultDanmakuTypes by pref(
        PrefKeys.prefDefaultDanmakuTypesKey,
        DanmakuType.entries.toList(),
        save = { list -> list.map { it.code }.joinToString(",") },
        restore = { str ->
            if (str.isEmpty()) emptyList()
            else str.split(",")
                .mapNotNull { code -> code.toIntOrNull()?.let(DanmakuType::fromCode) }
        }
    )
    var defaultDanmakuScale by pref(PrefKeys.prefDefaultDanmakuScaleKey, 1.6f)
    var defaultDanmakuOpacity by pref(PrefKeys.prefDefaultDanmakuOpacityKey, 0.7f)
    var defaultDanmakuSpeedFactor by pref(PrefKeys.prefDefaultDanmakuSpeedFactorKey, 1f)
    var defaultDanmakuArea by pref(PrefKeys.prefDefaultDanmakuAreaKey, 0.5f)

    // =========================================================================
    // 播放器 - 字幕
    // =========================================================================

    var defaultSubtitleFontSize by pref(
        PrefKeys.prefDefaultSubtitleFontSizeKey,
        24.sp,
        save = { it.value.roundToInt() },
        restore = { it.sp }
    )
    var defaultSubtitleBackgroundOpacity by pref(
        PrefKeys.prefDefaultSubtitleBackgroundOpacityKey,
        0.4f
    )
    var defaultSubtitleBottomPadding by pref(
        PrefKeys.prefDefaultSubtitleBottomPaddingKey,
        12.dp,
        save = { it.value.roundToInt() },
        restore = { it.dp }
    )

    // =========================================================================
    // 播放器 - 界面
    // =========================================================================

    var defaultPlaySpeed by pref(
        PrefKeys.prefDefaultPlaySpeedKey,
        PlaybackSpeed.x1,
        save = { it.code },
        restore = { PlaybackSpeed.fromCode(it) }
    )
    var showPersistentSeek by pref(PrefKeys.prefShowPersistentSeekKey, false)

    // =========================================================================
    // 应用界面
    // =========================================================================

    var firstHomeTopNavItem by pref(
        PrefKeys.prefFirstHomeTopNavItemKey,
        HomeTopNavItem.Dynamics,
        save = { it.code },
        restore = { HomeTopNavItem.fromCode(it) }
    )
    var firstPersonalTopNavItem by pref(
        PrefKeys.prefFirstPersonalTopNavItemKey,
        PersonalTopNavItem.ToView,
        save = { it.ordinal },
        restore = { PersonalTopNavItem.entries.getOrElse(it) { PersonalTopNavItem.ToView } }
    )
    var showHotword by pref(PrefKeys.prefShowHotwordKey, true)

    // =========================================================================
    // 隐私
    // =========================================================================

    var incognitoMode by pref(PrefKeys.prefIncognitoModeKey, false)

    // =========================================================================

    /**
     * [必须调用] 在 Application onCreate 中调用此方法。
     * 作用：读取 DataStore 到内存，再持续监听变化并同步内存缓存。
     */
    suspend fun init() = initMutex.withLock {
        if (initialized) return@withLock

        SBVApp.settingsDataStore.data.first().let { preferences ->
            updateMemoryCache(preferences)
            checkAndInitBuvid(preferences)
        }

        scope.launch { SBVApp.settingsDataStore.data.collect(::updateMemoryCache) }
        initialized = true
    }

    internal fun <T> persist(key: Preferences.Key<T>, value: T) {
        scope.launch {
            SBVApp.settingsDataStore.edit { preferences ->
                preferences[key] = value
            }
        }
    }

    suspend fun saveAuthData(value: AuthData?) {
        authDataPref.setAndPersist(value)
    }

    private fun updateMemoryCache(preferences: Preferences) {
        flowMap.forEach { (key, flow) ->
            if (preferences.contains(key)) {
                val newValue = preferences[key]
                flow.value = newValue
            }
        }
    }

    private fun checkAndInitBuvid(prefs: Preferences) {
        if (!prefs.contains(PrefKeys.prefBuvidKey) || prefs[PrefKeys.prefBuvidKey].isNullOrEmpty()) {
            val randomBuvid = generateBuvid()
            buvid = randomBuvid
        }
        if (!prefs.contains(PrefKeys.prefBuvid3Key) || prefs[PrefKeys.prefBuvid3Key].isNullOrEmpty()) {
            val randomBuvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
            buvid3 = randomBuvid3
        }
    }
}

/**
 * 核心委托类：
 * 1. 维护内存缓存 (via MutableStateFlow)
 * 2. Get: 直接读内存 (同步，无锁，极快)
 * 3. Set: 更新内存 + 异步写入 DataStore (不阻塞 UI)
 */
class PrefDelegate<T, P>(
    private val key: Preferences.Key<P>,
    private val defaultValue: T,
    map: ConcurrentHashMap<Preferences.Key<*>, MutableStateFlow<Any?>>,
    private val save: (T) -> P = { it as P },
    private val restore: (P) -> T = { it as T }
) : ReadWriteProperty<Any?, T> {

    private val _flow = MutableStateFlow<Any?>(save(defaultValue))

    init {
        map[key] = _flow
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val rawValue = _flow.value as? P
        return if (rawValue != null) restore(rawValue) else defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val persistValue = save(value)

        // 1. 立即更新内存，UI 瞬间响应
        _flow.value = persistValue

        // 2. 异步持久化
        Prefs.persist(key, persistValue)
    }

    suspend fun setAndPersist(value: T) {
        val persistValue = save(value)
        _flow.value = persistValue
        SBVApp.settingsDataStore.edit { preferences ->
            preferences[key] = persistValue
        }
    }
}

private object PrefKeys {
    // 账号 & 认证
    val prefAuthDataKey = stringPreferencesKey("auth_data")
    val prefBuvidKey = stringPreferencesKey("random_buvid")
    val prefBuvid3Key = stringPreferencesKey("random_buvid3")
    val prefUsernameKey = stringPreferencesKey("username")
    val prefAvatarKey = stringPreferencesKey("avatar")
    val prefSearchHistoryKey = stringPreferencesKey("search_history")

    // 播放器 - 视频
    val prefDefaultQualityKey = intPreferencesKey("dq")
    val prefDefaultVideoCodecKey = intPreferencesKey("dvc")
    val prefEnableSoftwareVideoDecoder = booleanPreferencesKey("enable_software_video_decoder")
    val prefActionAfterPlayKey = intPreferencesKey("action_after_play")

    // 播放器 - 音频
    val prefDefaultAudioKey = intPreferencesKey("da")

    // 播放器 - 弹幕
    val prefDefaultDanmakuTypesKey = stringPreferencesKey("ddts")
    val prefDefaultDanmakuScaleKey = floatPreferencesKey("dds2")
    val prefDefaultDanmakuOpacityKey = floatPreferencesKey("ddo")
    val prefDefaultDanmakuSpeedFactorKey = floatPreferencesKey("ddsf")
    val prefDefaultDanmakuAreaKey = floatPreferencesKey("dda")

    // 播放器 - 字幕
    val prefDefaultSubtitleFontSizeKey = intPreferencesKey("dsfs")
    val prefDefaultSubtitleBackgroundOpacityKey = floatPreferencesKey("dsbo")
    val prefDefaultSubtitleBottomPaddingKey = intPreferencesKey("dsbp")

    // 播放器 - 界面
    val prefDefaultPlaySpeedKey = intPreferencesKey("dps")
    val prefShowPersistentSeekKey = booleanPreferencesKey("show_persistent_seek")

    // 应用界面
    val prefFirstHomeTopNavItemKey = intPreferencesKey("first_home_top_nav")
    val prefFirstPersonalTopNavItemKey = intPreferencesKey("first_personal_top_nav")
    val prefShowHotwordKey = booleanPreferencesKey("shw")

    // 隐身模式
    val prefIncognitoModeKey = booleanPreferencesKey("im")
}
