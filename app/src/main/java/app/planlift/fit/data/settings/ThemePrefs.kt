package app.planlift.fit.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 配色风格：同一颗种子色，生成不同「味道」的配色 */
enum class ColorStyle { TONAL, VIBRANT, NEUTRAL, EXPRESSIVE }

enum class FontStyle { SYSTEM, SERIF, MONOSPACE, CURSIVE }

enum class AppLanguage { SYSTEM, ZH, EN }

enum class NavBarStyle { FIXED, FLOATING }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val seed: Long = 0xFF38BDF8,
    val dynamic: Boolean = false,
    val pureBlack: Boolean = false,
    val colorStyle: ColorStyle = ColorStyle.TONAL,
    val font: FontStyle = FontStyle.SYSTEM,
    val textScale: Float = 1.0f,
    val ripple: Boolean = true,
    val navFloating: Boolean = false,
    val glass: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM
)

object ThemePrefs {
    private const val PREFS = "planlift_prefs"

    val state = MutableStateFlow(ThemeSettings())

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
        val defaults = ThemeSettings()
        state.value = ThemeSettings(
            mode = runCatching {
                ThemeMode.valueOf(p.getString("theme_mode", defaults.mode.name)!!)
            }.getOrDefault(defaults.mode),
            seed = p.getLong("theme_seed", defaults.seed),
            dynamic = p.getBoolean("theme_dynamic", defaults.dynamic),
            pureBlack = p.getBoolean("theme_pure_black", defaults.pureBlack),
            colorStyle = runCatching {
                ColorStyle.valueOf(p.getString("theme_style", defaults.colorStyle.name)!!)
            }.getOrDefault(defaults.colorStyle),
            font = runCatching {
                FontStyle.valueOf(p.getString("theme_font", defaults.font.name)!!)
            }.getOrDefault(defaults.font),
            textScale = p.getFloat("theme_text_scale", defaults.textScale),
            ripple = p.getBoolean("theme_ripple", defaults.ripple),
            navFloating = p.getBoolean("theme_nav_floating", defaults.navFloating),
            glass = p.getBoolean("theme_glass", defaults.glass),
            language = runCatching {
                AppLanguage.valueOf(p.getString("app_language", defaults.language.name)!!)
            }.getOrDefault(defaults.language)
        )
    }

    fun update(context: Context, settings: ThemeSettings) {
        state.value = settings
        context.getSharedPreferences(PREFS, 0).edit()
            .putString("theme_mode", settings.mode.name)
            .putLong("theme_seed", settings.seed)
            .putBoolean("theme_dynamic", settings.dynamic)
            .putBoolean("theme_pure_black", settings.pureBlack)
            .putString("theme_style", settings.colorStyle.name)
            .putString("theme_font", settings.font.name)
            .putFloat("theme_text_scale", settings.textScale)
            .putBoolean("theme_ripple", settings.ripple)
            .putBoolean("theme_nav_floating", settings.navFloating)
            .putBoolean("theme_glass", settings.glass)
            .putString("app_language", settings.language.name)
            .apply()
    }

    /** 给 Activity 用：返回语言代码，null 表示跟随系统 */
    fun languageTag(context: Context): String? = when (state.value.language) {
        AppLanguage.SYSTEM -> null
        AppLanguage.ZH -> "zh"
        AppLanguage.EN -> "en"
    }
}
