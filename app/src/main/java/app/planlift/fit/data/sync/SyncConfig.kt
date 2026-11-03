package app.planlift.fit.data.sync

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class SyncConfig(
    val url: String = "",
    val user: String = "",
    val password: String = "",
    val dir: String = "PlanLift"
) {
    val configured: Boolean get() = url.isNotBlank() && user.isNotBlank() && password.isNotBlank()
}

/** 密码放 EncryptedSharedPreferences（Android Keystore 托管的主密钥）。 */
object SyncConfigStore {
    private const val FILE = "planlift_sync"
    private const val KEY_URL = "url"
    private const val KEY_USER = "user"
    private const val KEY_PASS = "pass"
    private const val KEY_DIR = "dir"
    private const val KEY_LAST = "last_sync"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun load(context: Context): SyncConfig {
        val p = prefs(context)
        return SyncConfig(
            url = p.getString(KEY_URL, "") ?: "",
            user = p.getString(KEY_USER, "") ?: "",
            password = p.getString(KEY_PASS, "") ?: "",
            dir = p.getString(KEY_DIR, "PlanLift") ?: "PlanLift"
        )
    }

    fun save(context: Context, config: SyncConfig) {
        prefs(context).edit()
            .putString(KEY_URL, config.url.trim())
            .putString(KEY_USER, config.user.trim())
            .putString(KEY_PASS, config.password)
            .putString(KEY_DIR, config.dir.ifBlank { "PlanLift" }.trim('/'))
            .apply()
    }

    fun lastSyncAt(context: Context): Long = prefs(context).getLong(KEY_LAST, 0L)

    fun markSynced(context: Context, time: Long) {
        prefs(context).edit().putLong(KEY_LAST, time).apply()
    }
}
