package app.planlift.fit.data.sync

import app.planlift.fit.data.local.BodyMetricEntity
import app.planlift.fit.data.local.EnrollmentEntity
import app.planlift.fit.data.local.PersonalRecordEntity
import app.planlift.fit.data.local.SessionEntity
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.data.local.UserProfileEntity
import kotlinx.serialization.Serializable

/** 远端快照：全量、带 rev。个人数据量很小（<200 KB），全量最简单也最不容易错。 */
@Serializable
data class Snapshot(
    val schema: Int = 1,
    val rev: Long = 0,
    val updatedAt: Long = 0,
    val deviceId: String = "",
    val profiles: List<UserProfileEntity> = emptyList(),
    val enrollments: List<EnrollmentEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val setLogs: List<SetLogEntity> = emptyList(),
    val metrics: List<BodyMetricEntity> = emptyList(),
    val records: List<PersonalRecordEntity> = emptyList()
)

data class SyncResult(
    val ok: Boolean,
    val uploaded: Int = 0,
    val downloaded: Int = 0,
    val conflicts: Int = 0,
    val remoteRev: Long = 0,
    val message: String = ""
)
