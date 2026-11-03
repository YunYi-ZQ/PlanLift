package app.planlift.fit

import android.app.Application
import app.planlift.fit.data.content.ContentRepository
import app.planlift.fit.data.local.PlanLiftDatabase
import app.planlift.fit.data.repo.WorkoutRepository
import app.planlift.fit.data.sync.SyncManager
import java.util.UUID

class PlanLiftApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    private val db = PlanLiftDatabase.get(app)
    val content = ContentRepository(app)
    val deviceId: String = DeviceId.get(app)
    val repo = WorkoutRepository(
        enrollmentDao = db.enrollmentDao(),
        sessionDao = db.sessionDao(),
        setLogDao = db.setLogDao(),
        metricDao = db.metricDao(),
        profileDao = db.profileDao(),
        content = content,
        deviceId = deviceId
    )
    val sync = SyncManager(app, repo)
}

/** 设备标识：用于同步冲突判定，卸载重装会变（可接受）。 */
object DeviceId {
    private const val PREFS = "planlift_prefs"
    private const val KEY = "device_id"

    fun get(app: Application): String {
        val prefs = app.getSharedPreferences(PREFS, 0)
        prefs.getString(KEY, null)?.let { return it }
        val id = UUID.randomUUID().toString().substring(0, 8)
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}
