package app.planlift.fit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 所有可变记录都带同步字段：updatedAt / rev / deviceId / deleted。
 * WebDAV 合并依赖这四个字段，W4 不需要改表。
 */

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val sex: String,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val level: String,
    val daysAvailable: Int,
    val equipment: String,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "enrollment", indices = [Index("planId")])
data class EnrollmentEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val startDate: Long,
    val currentWeek: Int = 1,
    val weightOffsetKg: Double = 0.0,
    val status: String = "active",
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "training_session", indices = [Index("enrollmentId"), Index("date")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val enrollmentId: String,
    val planDayIndex: Int,
    val dayNameZh: String,
    val dayNameEn: String,
    val date: Long,
    val durationMin: Int,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val note: String = "",
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "set_log", indices = [Index("sessionId")])
data class SetLogEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseZh: String,
    val exerciseEn: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val done: Boolean,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "body_metric", indices = [Index("date")])
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val weightKg: Double?,
    val bodyFat: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)

@Entity(tableName = "personal_record", indices = [Index(value = ["exerciseId"], unique = true)])
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val e1rm: Double,
    val weightKg: Double,
    val reps: Int,
    val date: Long,
    val updatedAt: Long,
    val rev: Long,
    val deviceId: String,
    val deleted: Boolean = false
)
