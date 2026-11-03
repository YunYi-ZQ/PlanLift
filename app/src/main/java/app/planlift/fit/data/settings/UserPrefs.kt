package app.planlift.fit.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** 身高这类「一次填好」的资料 */
object UserPrefs {
    private const val PREFS = "planlift_prefs"
    private const val KEY_HEIGHT = "user_height_cm"

    val heightCm = MutableStateFlow(0)

    fun init(context: Context) {
        heightCm.value = context.getSharedPreferences(PREFS, 0).getInt(KEY_HEIGHT, 0)
    }

    fun setHeight(context: Context, cm: Int) {
        heightCm.value = cm
        context.getSharedPreferences(PREFS, 0).edit().putInt(KEY_HEIGHT, cm).apply()
    }

    /** BMI = kg / m²，保留一位小数 */
    fun bmi(weightKg: Double, heightCm: Int): Double? {
        if (weightKg <= 0.0 || heightCm < 100) return null
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }
}
