package app.planlift.fit.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemePrefs {
    private const val PREFS = "planlift_prefs"
    private const val KEY = "theme_mode"

    val mode = MutableStateFlow(ThemeMode.SYSTEM)

    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS, 0).getString(KEY, null)
        mode.value = runCatching { ThemeMode.valueOf(saved ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun set(context: Context, value: ThemeMode) {
        mode.value = value
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, value.name).apply()
    }
}
