package app.planlift.fit.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    /** 主题色种子（ARGB） */
    val seed: Long = 0xFF38BDF8,
    /** Android 12+ 跟随壁纸取色 */
    val dynamic: Boolean = false,
    /** 深色模式下背景纯黑（OLED 省电） */
    val pureBlack: Boolean = false
)

object ThemePrefs {
    private const val PREFS = "planlift_prefs"
    private const val K_MODE = "theme_mode"
    private const val K_SEED = "theme_seed"
    private const val K_DYNAMIC = "theme_dynamic"
    private const val K_BLACK = "theme_pure_black"

    val state = MutableStateFlow(ThemeSettings())

    /** 预设主题色，与 FlClash 的主题色板同一思路：给一批耐看的种子色 */
    val PRESET_SEEDS = listOf(
        0xFF38BDF8, // 天蓝
        0xFF10B981, // 翡翠绿
        0xFF6366F1, // 靛蓝
        0xFFA855F7, // 紫
        0xFFF59E0B, // 琥珀
        0xFFEF4444, // 红
        0xFFEC4899, // 粉
        0xFF64748B  // 石墨灰
    )

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, 0)
        val mode = runCatching {
            ThemeMode.valueOf(p.getString(K_MODE, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        state.value = ThemeSettings(
            mode = mode,
            seed = p.getLong(K_SEED, 0xFF38BDF8),
            dynamic = p.getBoolean(K_DYNAMIC, false),
            pureBlack = p.getBoolean(K_BLACK, false)
        )
    }

    fun update(context: Context, settings: ThemeSettings) {
        state.value = settings
        context.getSharedPreferences(PREFS, 0).edit()
            .putString(K_MODE, settings.mode.name)
            .putLong(K_SEED, settings.seed)
            .putBoolean(K_DYNAMIC, settings.dynamic)
            .putBoolean(K_BLACK, settings.pureBlack)
            .apply()
    }
}
